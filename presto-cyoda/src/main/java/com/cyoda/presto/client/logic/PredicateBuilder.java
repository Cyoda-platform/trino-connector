/*
 * Copyright (C) 2022 Cyoda Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.cyoda.presto.client.logic;

import com.cyoda.presto.client.types.SupportedDataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.common.predicate.DiscreteValues;
import com.facebook.presto.common.predicate.Domain;
import com.facebook.presto.common.predicate.EquatableValueSet;
import com.facebook.presto.common.predicate.Marker;
import com.facebook.presto.common.predicate.Range;
import com.facebook.presto.common.predicate.Ranges;
import com.facebook.presto.common.predicate.SortedRangeSet;
import com.facebook.presto.common.predicate.TupleDomain;
import com.facebook.presto.common.predicate.ValueSet;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.logic.LeafPredicateNode.leaf;
import static com.cyoda.presto.client.logic.Predicate.*;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Collections.nCopies;

/**
 * Adopted from Presto's PreparedStatementBuilder#toPredicate
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
public class PredicateBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(PredicateBuilder.class);

    private PredicateBuilder() {
    }

    private static String toBindPredicate(String columnName, String operator) {
        return format("%s %s ?", columnName, operator);
    }

    private static String debug(
            TupleDomain.ColumnDomain<ColumnHandle> columnDomain) {
        Domain domain = columnDomain.getDomain();
        CyodaColumnHandle columnHandle = (CyodaColumnHandle) columnDomain.getColumn();

        String columnName = columnHandle.getColumnName();
        if (domain.getValues().isAll()) {
            return domain.isNullAllowed() ? "TRUE" : columnName + " IS NOT NULL";
        }
        if (domain.getValues().isNone()) {
            return domain.isNullAllowed() ? columnName + " IS NULL" : "FALSE";
        }

        return domain.getValues().getValuesProcessor().transform(
                ranges -> {
                    // Add disjuncts for ranges
                    List<String> disjuncts = new ArrayList<>();
                    List<Object> singleValues = new ArrayList<>();

                    // Add disjuncts for ranges
                    for (Range range : ranges.getOrderedRanges()) {
                        checkState(!range.isAll()); // Already checked
                        if (range.isSingleValue()) {
                            singleValues.add(range.getSingleValue());
                        } else {
                            List<String> rangeConjuncts = new ArrayList<>();
                            if (!range.isLowUnbounded()) {
                                rangeConjuncts.add(toBindPredicate(columnName, range.isLowInclusive() ? ">=" : ">"));
                            }
                            if (!range.isHighUnbounded()) {
                                rangeConjuncts.add(toBindPredicate(columnName, range.isHighInclusive() ? "<=" : "<"));
                            }
                            // If rangeConjuncts is null, then the range was ALL, which should already have been checked for
                            checkState(!rangeConjuncts.isEmpty());
                            disjuncts.add("(" + Joiner.on(" AND ").join(rangeConjuncts) + ")");
                        }
                    }

                    // Add back all of the possible single values either as an equality or an IN predicate
                    if (singleValues.size() == 1) {
                        disjuncts.add(toBindPredicate(columnName, "="));
                    } else if (singleValues.size() > 1) {
                        disjuncts.add(columnName + " IN (" + Joiner.on(",").join(nCopies(singleValues.size(), "?")) + ")");
                    }

                    // Add nullability disjuncts
                    checkState(!disjuncts.isEmpty());
                    if (domain.isNullAllowed()) {
                        disjuncts.add(columnName + " IS NULL");
                    }

                    return "(" + Joiner.on(" OR ").join(disjuncts) + ")";
                },

                discreteValues -> {
                    String values = Joiner.on(",").join(nCopies(discreteValues.getValues().size(), "?"));
                    String predicate = columnName + (discreteValues.isWhiteList() ? "" : " NOT") + " IN (" + values + ")";
                    if (domain.isNullAllowed()) {
                        predicate = "(" + predicate + " OR " + columnName + " IS NULL)";
                    }
                    return predicate;
                },

                allOrNone -> {
                    throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "Case should not be reachable");
                });
    }

    public static PredicateNode<Any> setupConstraintPredicates(TupleDomain<ColumnHandle> constraintSummary) {
        if (LOG.isDebugEnabled()) {
            List<TupleDomain.ColumnDomain<ColumnHandle>> columnDomains = constraintSummary.getColumnDomains()
                    .orElse(Collections.emptyList());
            List<Object> conjuncts = columnDomains.stream().map(PredicateBuilder::debug).collect(Collectors.toList());
            StringBuilder where = new StringBuilder("WHERE ");
            String debug = Joiner.on(" AND\n").appendTo(where, conjuncts).toString();
            LOG.debug(debug);

        }

        ImmutableList.Builder<PredicateNode<?>> disjunctsBuilder = ImmutableList.builder();

        if (constraintSummary.isNone()) return CompoundPredicateNode.EMPTY;

        if (!constraintSummary.isAll()) {
            List<TupleDomain.ColumnDomain<ColumnHandle>> columnDomains = constraintSummary.getColumnDomains()
                    .orElse(Collections.emptyList());
            for (TupleDomain.ColumnDomain<ColumnHandle> columnDomain : columnDomains) {
                CyodaColumnHandle columnHandle = (CyodaColumnHandle) columnDomain.getColumn();
                int position = columnHandle.getOrdinalPosition();
                Domain domain = columnDomain.getDomain();

                if (domain.isNone()) {
                    disjunctsBuilder.add(CompoundPredicateNode.EMPTY);
                } else if (domain.isAll()) {
                    //noinspection UnnecessaryContinue
                    continue;  // Just to be explicit that we are considering this case
                } else if (domain.isOnlyNull()) {
                    disjunctsBuilder.add(leaf(newIsNullPredicateAny(columnHandle)));
                } else if (domain.getValues().isAll() && domain.isNullAllowed()) {
                    disjunctsBuilder.add(leaf(newIsNotNullPredicateAny(columnHandle)));
                } else if (domain.isSingleValue()) {
                    Predicate<?> predicate = createEqualsPredicate(columnHandle, domain.getSingleValue());
                    disjunctsBuilder.add(leaf(predicate));
                } else {
                    int count = domain.getValues().getValuesProcessor().transform(
                            ranges -> {
                                // Add disjuncts for ranges --> an IN LIST
                                List<Object> singleValues = new ArrayList<>();

                                int disjuncts = 0;
                                // Add disjuncts for ranges
                                for (Range range : ranges.getOrderedRanges()) {
                                    checkState(!range.isAll()); // Already checked
                                    if (range.isSingleValue()) {
                                        singleValues.add(range.getSingleValue());
                                    } else {
                                        ImmutableList.Builder<PredicateNode<?>> conjuncts = ImmutableList.builder();
                                        if (!range.isLowUnbounded()) {
                                            Predicate.ComparisonOp op = (range.isLowInclusive())
                                                    ? Predicate.ComparisonOp.GREATER_EQUAL : Predicate.ComparisonOp.GREATER;
                                            LeafPredicateNode<?> leaf = leaf(createComparisonPredicate(columnHandle, op, range.getLowBoundedValue()));
                                            conjuncts.add(leaf);
                                        }
                                        if (!range.isHighUnbounded()) {
                                            Predicate.ComparisonOp op = (range.isHighInclusive())
                                                    ? Predicate.ComparisonOp.LESS_EQUAL : Predicate.ComparisonOp.LESS;
                                            LeafPredicateNode<?> leaf = leaf(createComparisonPredicate(columnHandle, op, range.getHighBoundedValue()));
                                            conjuncts.add(leaf);
                                        }
                                        // If rangeConjuncts is null, then the range was ALL, which should already have been checked for
                                        ImmutableList<PredicateNode<?>> rangeConjuncts = conjuncts.build();
                                        checkState(!rangeConjuncts.isEmpty());
                                        disjunctsBuilder.add(CompoundPredicateNode.of(rangeConjuncts, Connective.AND));
                                        disjuncts++;
                                    }
                                }

                                // Add back all of the possible single values either as an equality or an IN predicate
                                if (singleValues.size() == 1) {
                                    disjunctsBuilder.add(leaf(createEqualsPredicate(columnHandle, singleValues.get(0))));
                                } else if (singleValues.size() > 1) {
                                    disjunctsBuilder.add(leaf(Predicate.newInListPredicate(columnHandle, new DiscreteValues() {
                                        @Override
                                        public boolean isWhiteList() {
                                            return true;
                                        }

                                        @Override
                                        public Collection<Object> getValues() {
                                            return singleValues;
                                        }
                                    })));
                                }
                                disjuncts += singleValues.size();

                                checkState(disjuncts > 0, "[Cyoda] Expected that we have some disjuncts");
                                // Add nullability disjuncts
                                if (domain.isNullAllowed()) {
                                    disjunctsBuilder.add(leaf(newIsNotNullPredicateAny(columnHandle)));
                                }

                                return disjuncts;
                            },

                            discreteValues -> {
                                boolean negate = !discreteValues.isWhiteList();
                                Predicate<?> predicate = Predicate.newInListPredicate(columnHandle, discreteValues).negate(negate);
                                LeafPredicateNode<?> leaf = leaf(predicate);
                                disjunctsBuilder.add(leaf);
                                return 1;
                            },

                            allOrNone -> {
                                throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "Case should not be reachable");
                            });
                }
            }
        }
        return CompoundPredicateNode.of(disjunctsBuilder.build(), Connective.AND);
    }

    @SuppressWarnings("java:S3776")
    public static List<Predicate<?>> oldSetup(TupleDomain<ColumnHandle> constraintSummary) {
        ImmutableList.Builder<Predicate<?>> builder = ImmutableList.builder();
        if (constraintSummary.isNone()) {
            return Collections.emptyList();
        } else if (!constraintSummary.isAll()) {
            List<TupleDomain.ColumnDomain<ColumnHandle>> columnDomains = constraintSummary.getColumnDomains()
                    .orElse(Collections.emptyList());
            for (TupleDomain.ColumnDomain<ColumnHandle> columnDomain : columnDomains) {
                CyodaColumnHandle columnHandle = (CyodaColumnHandle) columnDomain.getColumn();
                int position = columnHandle.getOrdinalPosition();
                Domain domain = columnDomain.getDomain();
                if (domain.isNone()) {
                    return Collections.emptyList();
                } else //noinspection StatementWithEmptyBody
                    if (domain.isAll()) {
                        // no restriction
                    } else if (domain.isOnlyNull()) {
                        builder.add(newIsNullPredicate(columnHandle));
                    } else if (domain.getValues().isAll() && domain.isNullAllowed()) {
                        builder.add(newIsNotNullPredicate(columnHandle));
                    } else if (domain.isSingleValue()) {
                        Predicate<?> predicate = createEqualsPredicate(columnHandle, domain.getSingleValue());
                        builder.add(predicate);
                    } else {
                        ValueSet valueSet = domain.getValues();
                        if (valueSet instanceof EquatableValueSet) {
                            DiscreteValues discreteValues = valueSet.getDiscreteValues();
                            Predicate<?> predicate = Predicate.newInListPredicate(columnHandle, discreteValues);
                            builder.add(predicate);
                        } else if (valueSet instanceof SortedRangeSet) {
                            Ranges ranges = ((SortedRangeSet) valueSet).getRanges();
                            List<Range> orderedRanges = ranges.getOrderedRanges();
                            Range span = ranges.getSpan();
                            Marker low = span.getLow();
                            if (!low.isLowerUnbounded()) {
                                Predicate.ComparisonOp op = (low.getBound() == Marker.Bound.ABOVE)
                                        ? Predicate.ComparisonOp.GREATER : Predicate.ComparisonOp.GREATER_EQUAL;
                                Predicate<?> predicate = createComparisonPredicate(columnHandle, op, low.getValue());
                                builder.add(predicate);
                            }
                            Marker high = span.getHigh();
                            if (!high.isUpperUnbounded()) {
                                Predicate.ComparisonOp op = (low.getBound() == Marker.Bound.BELOW)
                                        ? Predicate.ComparisonOp.LESS : Predicate.ComparisonOp.LESS_EQUAL;
                                Predicate<?> predicate = createComparisonPredicate(columnHandle, op, high.getValue());
                                builder.add(predicate);
                            }
                        } else {
                            throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "Unexpected domain: " + domain);
                        }
                    }
            }
        }
        return builder.build();
    }

    private static Predicate<?> createComparisonPredicate(
            CyodaColumnHandle columnHandle,
            Predicate.ComparisonOp op,
            Object nativeValue) {
        // TODO: This does not yet cover all cases.
        switch (columnHandle.getDataType()) {
            case LONG:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, Long.class);
            case INTEGER:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, Integer.class);
            case SHORT:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, Short.class);
            case BYTE:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, Byte.class);
            case STRING:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, String.class);
            case DOUBLE:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, Double.class);
            case FLOAT:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, Float.class);
            case BOOLEAN:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, Boolean.class);
            default:
                throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "DataType  " + columnHandle.getDataType() + " not yet supported");
        }
    }

    private static Predicate<?> createEqualsPredicate(CyodaColumnHandle columnHandle, Object nativeValue) {
        return createComparisonPredicate(columnHandle, Predicate.ComparisonOp.EQUAL, nativeValue);
    }

    private static <T extends Comparable<T>> Predicate<T> prestoNativeToPredicate(
            CyodaColumnHandle columnHandle,
            Predicate.ComparisonOp op,
            Object nativeValue,
            Class<T> javaType) {
        SupportedDataType<T> thing = SupportedDataType.ofPrestoNativeValue(columnHandle.getColumnType(), nativeValue, javaType);
        return createComparisonPredicate(columnHandle, op, thing, javaType);
    }


    private static <T extends Comparable<T>> Predicate<T> createEqualsPredicate(CyodaColumnHandle columnHandle, SupportedDataType<T> nativeValue, Class<T> javaType) {
        return createComparisonPredicate(columnHandle, Predicate.ComparisonOp.EQUAL, nativeValue, javaType);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> Predicate<T> createComparisonPredicate(
            CyodaColumnHandle columnHandle,
            Predicate.ComparisonOp op,
            SupportedDataType<T> value,
            Class<T> javaType) {
        Type type = columnHandle.getColumnType();
        // TODO: This does not yet cover all cases.
        switch (value.dataType) {
            case LONG:
                return (Predicate<T>) Predicate.newComparisonPredicate(columnHandle, op, value.asLong());
            case INTEGER:
                return (Predicate<T>) Predicate.newComparisonPredicate(columnHandle, op, value.asInt());
            case SHORT:
                return (Predicate<T>) Predicate.newComparisonPredicate(columnHandle, op, value.asShort());
            case BYTE:
                return (Predicate<T>) Predicate.newComparisonPredicate(columnHandle, op, value.asByte());
            case STRING:
                return (Predicate<T>) Predicate.newComparisonPredicate(columnHandle, op, value.asString());
            case DOUBLE:
                return (Predicate<T>) Predicate.newComparisonPredicate(columnHandle, op, value.asDouble());
            case FLOAT:
                return (Predicate<T>) Predicate.newComparisonPredicate(columnHandle, op, value.asFloat());
            case BOOLEAN:
                return (Predicate<T>) Predicate.newComparisonPredicate(columnHandle, op, value.asBoolean());
            default:
                throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "Unexpected java value for column "
                        + columnHandle.getColumnName() + ": " + value.value + "(" + value.dataType + ")");

        }
    }
}
