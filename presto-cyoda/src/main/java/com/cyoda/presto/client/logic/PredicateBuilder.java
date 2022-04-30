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
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.predicate.DiscreteValues;
import com.facebook.presto.common.predicate.Domain;
import com.facebook.presto.common.predicate.Range;
import com.facebook.presto.common.predicate.TupleDomain;
import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.cyoda.presto.client.logic.LeafPredicateNode.leaf;
import static com.cyoda.presto.client.logic.Predicate.newIsNotNullPredicateAny;
import static com.cyoda.presto.client.logic.Predicate.newIsNullPredicateAny;
import static com.cyoda.presto.client.logic.PredicateBuilderDebugger.debug;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.nCopies;

/**
 * Adopted from Presto's PreparedStatementBuilder#toPredicate
 */
public class PredicateBuilder {

    private static final SupplierLogger LOG = SupplierLogger.get(PredicateBuilder.class);

    private PredicateBuilder() {
    }

    public static PredicateNode<Any> setupConstraintPredicates(TupleDomain<ColumnHandle> constraintSummary) {

        LOG.debug("Taken from PredicateBuilderDebugger: %s",() -> debug(constraintSummary));

        ImmutableList.Builder<PredicateNode<?>> conjunctsBuilder = ImmutableList.builder();
        ImmutableList.Builder<String> sqlConjunctsBuilder = ImmutableList.builder();

        if (constraintSummary.isNone()) return CompoundPredicateNode.EMPTY;

        if (!constraintSummary.isAll()) {
            List<TupleDomain.ColumnDomain<ColumnHandle>> columnDomains = constraintSummary.getColumnDomains()
                    .orElse(Collections.emptyList());
            for (TupleDomain.ColumnDomain<ColumnHandle> columnDomain : columnDomains) {
                CyodaColumnHandle columnHandle = (CyodaColumnHandle) columnDomain.getColumn();
                String columnName = columnHandle.getColumnName();
                Domain domain = columnDomain.getDomain();

                if (domain.isNone()) { // values.isNone() && !nullAllowed
                    conjunctsBuilder.add(CompoundPredicateNode.EMPTY);
                    sqlConjunctsBuilder.add("FALSE");
                } else {
                    if (domain.isOnlyNull()) { // values.isNone() && isNullAllowed
                        conjunctsBuilder.add(leaf(newIsNullPredicateAny(columnHandle)));
                        sqlConjunctsBuilder.add(columnName + " IS NULL");
                    } else if (domain.getValues().isAll() && domain.isNullAllowed()) {
                        sqlConjunctsBuilder.add("TRUE");
                    } else if (domain.getValues().isAll() && !domain.isNullAllowed()) {
                        conjunctsBuilder.add(leaf(newIsNotNullPredicateAny(columnHandle)));
                        sqlConjunctsBuilder.add(columnName + " IS NOT NULL");
                    } else if (domain.isSingleValue()) {
                        Predicate<?> predicate = createEqualsPredicate(columnHandle, domain.getSingleValue());
                        conjunctsBuilder.add(leaf(predicate));
                        sqlConjunctsBuilder.add(columnHandle.getColumnName()+" = ?");
                    } else {
                        int count = domain.getValues().getValuesProcessor().transform(
                                ranges -> {
                                    // Add disjuncts for ranges --> an IN LIST
                                    List<Object> singleValues = new ArrayList<>();
                                    List<String> disjunctSql = new ArrayList<>();
                                    ImmutableList.Builder<PredicateNode<?>> disjunctsBuilder = ImmutableList.builder();


                                    int disjuncts = 0;
                                    // Add disjuncts for ranges
                                    for (Range range : ranges.getOrderedRanges()) {
                                        checkState(!range.isAll()); // Already checked
                                        if (range.isSingleValue()) {
                                            singleValues.add(range.getSingleValue());
                                        } else {
                                            List<PredicateNode<?>> rangeConjuncts = new ArrayList<>();
                                            List<String> rangeConjunctsColumnNames = new ArrayList<>();
                                            if (!range.isLowUnbounded()) {
                                                Predicate.ComparisonOp op = (range.isLowInclusive())
                                                        ? Predicate.ComparisonOp.GREATER_EQUAL : Predicate.ComparisonOp.GREATER;
                                                LeafPredicateNode<?> leaf = leaf(createComparisonPredicate(columnHandle, op, range.getLowBoundedValue()));
                                                rangeConjuncts.add(leaf);
                                                rangeConjunctsColumnNames.add(columnName);
                                            }
                                            if (!range.isHighUnbounded()) {
                                                Predicate.ComparisonOp op = (range.isHighInclusive())
                                                        ? Predicate.ComparisonOp.LESS_EQUAL : Predicate.ComparisonOp.LESS;
                                                LeafPredicateNode<?> leaf = leaf(createComparisonPredicate(columnHandle, op, range.getHighBoundedValue()));
                                                rangeConjuncts.add(leaf);
                                                rangeConjunctsColumnNames.add(columnName);
                                            }
                                            // If rangeConjuncts is null, then the range was ALL, which should already have been checked for
                                            checkState(!rangeConjuncts.isEmpty());
                                            disjunctsBuilder.add(CompoundPredicateNode.of(rangeConjuncts, Connective.AND));
                                            disjunctSql.add("(" + Joiner.on(" AND ").join(rangeConjunctsColumnNames) + ")");
                                            disjuncts++;
                                        }
                                    }

                                    // Add back all of the possible single values either as an equality or an IN predicate
                                    if (singleValues.size() == 1) {
                                        disjunctsBuilder.add(leaf(createEqualsPredicate(columnHandle, singleValues.get(0))));
                                        disjunctSql.add(columnName +" = ?");
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
                                        disjunctSql.add(columnName + " IN (" + Joiner.on(",").join(nCopies(singleValues.size(), "?")) + ")");
                                    }
                                    disjuncts += singleValues.size();

                                    checkState(disjuncts > 0, "[Cyoda] Expected that we have some disjuncts");
                                    // Add nullability disjuncts
                                    if (domain.isNullAllowed()) {
                                        disjunctsBuilder.add(leaf(newIsNotNullPredicateAny(columnHandle)));
                                        disjunctSql.add(columnName + " IS NULL");
                                    }

                                    sqlConjunctsBuilder.add("(" + Joiner.on(" OR ").join(disjunctSql) + ")");
                                    List<PredicateNode<?>> disjunctions = disjunctsBuilder.build();
                                    CompoundPredicateNode compoundPredicate = CompoundPredicateNode.of(disjunctions, Connective.OR);
                                    conjunctsBuilder.add(compoundPredicate);
                                    return disjuncts;
                                },

                                discreteValues -> {
                                    boolean negate = !discreteValues.isWhiteList();
                                    Predicate<?> predicate = Predicate.newInListPredicate(columnHandle, discreteValues).negate(negate);
                                    LeafPredicateNode<?> leaf = leaf(predicate);
                                    conjunctsBuilder.add(leaf);

                                    String values = Joiner.on(",").join(nCopies(discreteValues.getValues().size(), "?"));
                                    String predicateString = columnName + (discreteValues.isWhiteList() ? "" : " NOT") + " IN (" + values + ")";
                                    if (domain.isNullAllowed()) {
                                        predicateString = "(" + predicate + " OR " + columnName + " IS NULL)";
                                    }
                                    sqlConjunctsBuilder.add(predicateString);

                                    return 1;
                                },

                                allOrNone -> {
                                    throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "Case should not be reachable");
                                });
                        LOG.debug("Have established %d disjuncts", count);
                    }
                }
            }
            LOG.debug("Equivalent SQL:\n%s",() -> assembleSql(sqlConjunctsBuilder.build()));
        }
        return CompoundPredicateNode.of(conjunctsBuilder.build(), Connective.AND);
    }

    private static String assembleSql(List<String> conjuncts) {
        StringBuilder where = new StringBuilder("WHERE ");
        return Joiner.on(" AND\n").appendTo(where, conjuncts).toString();
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

    @SuppressWarnings("java:S1452")
    public static Predicate<?> createEqualsPredicate(CyodaColumnHandle columnHandle, Object nativeValue) {
        return createComparisonPredicate(columnHandle, Predicate.ComparisonOp.EQUAL, nativeValue);
    }

    private static <T extends Comparable<T>> Predicate<T> prestoNativeToPredicate(
            CyodaColumnHandle columnHandle,
            Predicate.ComparisonOp op,
            Object nativeValue,
            Class<T> javaType) {
        SupportedDataType<T> thing = SupportedDataType.ofPrestoNativeValue(columnHandle.getColumnType(), nativeValue, javaType);
        return createComparisonPredicate(columnHandle, op, thing);
    }


    @SuppressWarnings("unused")
    public static <T extends Comparable<T>> Predicate<T> createEqualsPredicate(CyodaColumnHandle columnHandle, SupportedDataType<T> nativeValue) {
        return createComparisonPredicate(columnHandle, Predicate.ComparisonOp.EQUAL, nativeValue);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> Predicate<T> createComparisonPredicate(
            CyodaColumnHandle columnHandle,
            Predicate.ComparisonOp op,
            SupportedDataType<T> value) {
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
