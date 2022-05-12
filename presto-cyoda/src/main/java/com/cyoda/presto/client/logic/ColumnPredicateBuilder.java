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

import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.predicate.DiscreteValues;
import com.facebook.presto.common.predicate.Domain;
import com.facebook.presto.common.predicate.Range;
import com.facebook.presto.common.predicate.TupleDomain;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.cyoda.presto.client.logic.LeafPredicateNode.leaf;
import static com.cyoda.presto.client.logic.PredicateBuilderDebugger.debug;
import static com.cyoda.presto.client.logic.ColumnPredicateUtils.*;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.nCopies;

/**
 * Adopted from Presto's PreparedStatementBuilder#toPredicate
 */
public class ColumnPredicateBuilder {

    private static final SupplierLogger LOG = SupplierLogger.get(ColumnPredicateBuilder.class);

    private ColumnPredicateBuilder() {
    }

    @SuppressWarnings("java:S3252") // Silly warning
    public static ColumnPredicateNode<Any> setupConstraintPredicates(TupleDomain<CyodaColumnHandle> constraintSummary) {

        LOG.debug("Taken from PredicateBuilderDebugger: %s",() -> debug(constraintSummary));

        ImmutableList.Builder<ColumnPredicateNode<?>> conjunctsBuilder = ImmutableList.builder();
        ImmutableList.Builder<String> sqlConjunctsBuilder = ImmutableList.builder();

        if (constraintSummary.isNone()) return CompoundPredicateNode.empty(null);

        if (!constraintSummary.isAll()) {
            List<TupleDomain.ColumnDomain<CyodaColumnHandle>> columnDomains = constraintSummary.getColumnDomains()
                    .orElse(Collections.emptyList());
            for (TupleDomain.ColumnDomain<CyodaColumnHandle> columnDomain : columnDomains) {
                CyodaColumnHandle columnHandle = columnDomain.getColumn();
                String columnName = columnHandle.getColumnName();
                Domain domain = columnDomain.getDomain();

                if (domain.isNone()) { // values.isNone() && !nullAllowed
                    conjunctsBuilder.add(CompoundPredicateNode.empty(null));
                    sqlConjunctsBuilder.add("FALSE");
                } else {
                    if (domain.isOnlyNull()) { // values.isNone() && isNullAllowed
                        conjunctsBuilder.add(leaf(null,newIsNullPredicateAny(columnHandle)));
                        sqlConjunctsBuilder.add(columnName + " IS NULL");
                    } else if (domain.getValues().isAll() && domain.isNullAllowed()) {
                        sqlConjunctsBuilder.add("TRUE");
                    } else if (domain.getValues().isAll() && !domain.isNullAllowed()) {
                        conjunctsBuilder.add(leaf(null,newIsNotNullPredicateAny(columnHandle)));
                        sqlConjunctsBuilder.add(columnName + " IS NOT NULL");
                    } else if (domain.isSingleValue()) {

                        Object singleValue = domain.getSingleValue();
                        ColumnPredicate<?> columnPredicate = createDumbEqualsPredicate(columnHandle, singleValue);
                        conjunctsBuilder.add(unTypedLeaf(null, columnPredicate));
                        sqlConjunctsBuilder.add(columnHandle.getColumnName()+" = ?");
                    } else {
                        int count = domain.getValues().getValuesProcessor().transform(
                                ranges -> {
                                    // Add disjuncts for ranges --> an IN LIST
                                    List<Object> singleValues = new ArrayList<>();
                                    List<String> disjunctSql = new ArrayList<>();
                                    ImmutableList.Builder<ColumnPredicateNode<?>> disjunctsBuilder = ImmutableList.builder();


                                    int disjuncts = 0;
                                    // Add disjuncts for ranges
                                    for (Range range : ranges.getOrderedRanges()) {
                                        checkState(!range.isAll()); // Already checked
                                        if (range.isSingleValue()) {
                                            singleValues.add(range.getSingleValue());
                                        } else {
                                            List<ColumnPredicateNode<?>> rangeConjuncts = new ArrayList<>();
                                            List<String> rangeConjunctsColumnNames = new ArrayList<>();
                                            if (!range.isLowUnbounded()) {
                                                ColumnPredicate.ComparisonOp op = (range.isLowInclusive())
                                                        ? ColumnPredicate.ComparisonOp.GREATER_EQUAL : ColumnPredicate.ComparisonOp.GREATER;
                                                ColumnPredicate<?> columnPredicate = newComparisonPredicateFromNative(columnHandle, op, range.getLowBoundedValue());
                                                LeafPredicateNode<?> leaf = unTypedLeaf(null, columnPredicate);
                                                rangeConjuncts.add(leaf);
                                                rangeConjunctsColumnNames.add(columnName);
                                            }
                                            if (!range.isHighUnbounded()) {
                                                ColumnPredicate.ComparisonOp op = (range.isHighInclusive())
                                                        ? ColumnPredicate.ComparisonOp.LESS_EQUAL : ColumnPredicate.ComparisonOp.LESS;
                                                ColumnPredicate<?> columnPredicate = newComparisonPredicateFromNative(columnHandle, op, range.getHighBoundedValue());
                                                LeafPredicateNode<?> leaf = unTypedLeaf(null, columnPredicate);
                                                rangeConjuncts.add(leaf);
                                                rangeConjunctsColumnNames.add(columnName);
                                            }
                                            // If rangeConjuncts is null, then the range was ALL, which should already have been checked for
                                            checkState(!rangeConjuncts.isEmpty());
                                            disjunctsBuilder.add(CompoundPredicateNode.of(null,rangeConjuncts, Connective.AND));
                                            disjunctSql.add("(" + Joiner.on(" AND ").join(rangeConjunctsColumnNames) + ")");
                                            disjuncts++;
                                        }
                                    }

                                    // Add back all of the possible single values either as an equality or an IN predicate
                                    if (singleValues.size() == 1) {
                                        ColumnPredicate<?> equalsColumnPredicate = createDumbEqualsPredicate(columnHandle, singleValues.get(0));
                                        disjunctsBuilder.add(unTypedLeaf(null, equalsColumnPredicate));
                                        disjunctSql.add(columnName +" = ?");
                                    } else if (singleValues.size() > 1) {
                                        disjunctsBuilder.add(unTypedLeaf(null,newInListPredicateFromDiscrete(columnHandle, new DiscreteValues() {
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
                                        disjunctsBuilder.add(leaf(null,newIsNotNullPredicateAny(columnHandle)));
                                        disjunctSql.add(columnName + " IS NULL");
                                    }

                                    sqlConjunctsBuilder.add("(" + Joiner.on(" OR ").join(disjunctSql) + ")");
                                    List<ColumnPredicateNode<?>> disjunctions = disjunctsBuilder.build();
                                    CompoundPredicateNode compoundPredicate = CompoundPredicateNode.of(null,disjunctions, Connective.OR);
                                    conjunctsBuilder.add(compoundPredicate);
                                    return disjuncts;
                                },

                                discreteValues -> {
                                    boolean negate = !discreteValues.isWhiteList();
                                    ColumnPredicate<?> columnPredicate = newInListPredicateFromDiscrete(columnHandle, discreteValues).negate(negate);
                                    LeafPredicateNode<?> leaf = unTypedLeaf(null, columnPredicate);
                                    conjunctsBuilder.add(leaf);

                                    String values = Joiner.on(",").join(nCopies(discreteValues.getValues().size(), "?"));
                                    String predicateString = columnName + (discreteValues.isWhiteList() ? "" : " NOT") + " IN (" + values + ")";
                                    if (domain.isNullAllowed()) {
                                        predicateString = "(" + columnPredicate + " OR " + columnName + " IS NULL)";
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
        return CompoundPredicateNode.of(null,conjunctsBuilder.build(), Connective.AND);
    }

    private static String assembleSql(List<String> conjuncts) {
        StringBuilder where = new StringBuilder("WHERE ");
        return Joiner.on(" AND\n").appendTo(where, conjuncts).toString();
    }


    private static ColumnPredicate<?> createDumbEqualsPredicate(CyodaColumnHandle columnHandle, Object nativeValue) {
        return newComparisonPredicateFromNative(columnHandle, ColumnPredicate.ComparisonOp.EQUAL, nativeValue);
    }

    private static LeafPredicateNode<?> unTypedLeaf(CompoundPredicateNode parent, ColumnPredicate columnPredicate) {
        return leaf(parent,columnPredicate);
    }

}
