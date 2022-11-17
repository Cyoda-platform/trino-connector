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
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.predicate.DiscreteValues;
import io.trino.spi.predicate.Domain;
import io.trino.spi.predicate.Range;
import io.trino.spi.predicate.TupleDomain;
import io.trino.spi.TrinoException;
import io.trino.spi.StandardErrorCode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.cyoda.presto.client.logic.PredicateBuilderDebugger.debug;
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
    public static CompoundPredicateNode setupConstraintPredicates(TupleDomain<ColumnHandle> constraintSummary) {

        LOG.debug("Taken from PredicateBuilderDebugger: %s",() -> debug(constraintSummary));

        CompoundPredicateNode.Builder conjunctsBuilder = CompoundPredicateNode.builder(Connective.AND);
        ImmutableList.Builder<String> sqlConjunctsBuilder = ImmutableList.builder();

        if (constraintSummary.isNone()) return CompoundPredicateNode.empty(null);

        if (!constraintSummary.isAll()) {
            List<TupleDomain.ColumnDomain<ColumnHandle>> columnDomains = constraintSummary.getColumnDomains()
                    .orElse(Collections.emptyList());
            for (TupleDomain.ColumnDomain<ColumnHandle> columnDomain : columnDomains) {
                CyodaColumnHandle columnHandle = (CyodaColumnHandle) columnDomain.getColumn();
                String columnName = columnHandle.getColumnName();
                Domain domain = columnDomain.getDomain();

                if (domain.isNone()) { // values.isNone() && !nullAllowed
                    conjunctsBuilder.addMember(CompoundPredicateNode.empty(null));
                    sqlConjunctsBuilder.add("FALSE");
                } else {
                    if (domain.isOnlyNull()) { // values.isNone() && isNullAllowed
                        conjunctsBuilder.addLeaf(ColumnPredicate.isNull(columnHandle));
                        sqlConjunctsBuilder.add(columnName + " IS NULL");
                    } else if (domain.getValues().isAll() && domain.isNullAllowed()) {
                        sqlConjunctsBuilder.add("TRUE");
                    } else if (domain.getValues().isAll() && !domain.isNullAllowed()) {
                        conjunctsBuilder.addLeaf(ColumnPredicate.isNotNull(columnHandle));
                        sqlConjunctsBuilder.add(columnName + " IS NOT NULL");
                    } else if (domain.isSingleValue()) {

                        Object singleValue = domain.getSingleValue();
                        ColumnPredicate<?> columnPredicate = createDumbEqualsPredicate(columnHandle, singleValue);
                        conjunctsBuilder.addLeaf(columnPredicate);
                        sqlConjunctsBuilder.add(columnHandle.getColumnName()+" = ?");
                    } else {
                        int count = domain.getValues().getValuesProcessor().transform(
                                ranges -> {
                                    // Add disjuncts for ranges --> an IN LIST
                                    List<Object> singleValues = new ArrayList<>();
                                    List<String> disjunctSql = new ArrayList<>();
                                    CompoundPredicateNode.Builder disjunctsBuilder = CompoundPredicateNode.builder(Connective.OR);


                                    int disjuncts = 0;
                                    // Add disjuncts for ranges
                                    for (Range range : ranges.getOrderedRanges()) {
                                        checkState(!range.isAll()); // Already checked
                                        if (range.isSingleValue()) {
                                            singleValues.add(range.getSingleValue());
                                        } else {
                                            CompoundPredicateNode.Builder rangeConjuncts = CompoundPredicateNode.builder(Connective.AND);
                                            List<String> rangeConjunctsColumnNames = new ArrayList<>();
                                            if (!range.isLowUnbounded()) {
                                                ColumnPredicate.ComparisonOp op = (range.isLowInclusive())
                                                        ? ColumnPredicate.ComparisonOp.GREATER_EQUAL : ColumnPredicate.ComparisonOp.GREATER;
                                                ColumnPredicate<?> columnPredicate = columnHandle.newComparisonPredicateFromNative(op, range.getLowBoundedValue());
                                                rangeConjuncts.addLeaf(columnPredicate);
                                                rangeConjunctsColumnNames.add(columnName);
                                            }
                                            if (!range.isHighUnbounded()) {
                                                ColumnPredicate.ComparisonOp op = (range.isHighInclusive())
                                                        ? ColumnPredicate.ComparisonOp.LESS_EQUAL : ColumnPredicate.ComparisonOp.LESS;
                                                ColumnPredicate<?> columnPredicate = columnHandle.newComparisonPredicateFromNative(op, range.getHighBoundedValue());
                                                rangeConjuncts.addLeaf(columnPredicate);
                                                rangeConjunctsColumnNames.add(columnName);
                                            }
                                            // If rangeConjuncts is null, then the range was ALL, which should already have been checked for
                                            CompoundPredicateNode rangeConjunctsResult = rangeConjuncts.build();
                                            checkState(!rangeConjunctsResult.isEmpty());
                                            disjunctsBuilder.addMember(rangeConjunctsResult);
                                            disjunctSql.add("(" + Joiner.on(" AND ").join(rangeConjunctsColumnNames) + ")");
                                            disjuncts++;
                                        }
                                    }

                                    // Add back all of the possible single values either as an equality or an IN predicate
                                    if (singleValues.size() == 1) {
                                        ColumnPredicate<?> equalsColumnPredicate = createDumbEqualsPredicate(columnHandle, singleValues.get(0));
                                        disjunctsBuilder.addLeaf(equalsColumnPredicate);
                                        disjunctSql.add(columnName +" = ?");
                                    } else if (singleValues.size() > 1) {
                                        disjunctsBuilder.addLeaf(columnHandle.newInListPredicateFromDiscrete(new DiscreteValues() {
                                            @Override
                                            public boolean isInclusive() {
                                                return true;
                                            }

                                            @Override
                                            public int getValuesCount() {
                                                return singleValues.size();
                                            }

                                            @Override
                                            public Collection<Object> getValues() {
                                                return singleValues;
                                            }
                                        }));
                                        disjunctSql.add(columnName + " IN (" + Joiner.on(",").join(nCopies(singleValues.size(), "?")) + ")");
                                    }
                                    disjuncts += singleValues.size();

                                    checkState(disjuncts > 0, "[Cyoda] Expected that we have some disjuncts");
                                    // Add nullability disjuncts
                                    if (domain.isNullAllowed()) {
                                        disjunctsBuilder.addLeaf(ColumnPredicate.<Any>isNotNull(columnHandle));
                                        disjunctSql.add(columnName + " IS NULL");
                                    }

                                    sqlConjunctsBuilder.add("(" + Joiner.on(" OR ").join(disjunctSql) + ")");
                                    conjunctsBuilder.addMember(disjunctsBuilder.build());
                                    return disjuncts;
                                },

                                discreteValues -> {
                                    boolean negate = !discreteValues.isInclusive();
                                    ColumnPredicate<?> columnPredicate = columnHandle.newInListPredicateFromDiscrete(discreteValues).negate(negate);
                                    conjunctsBuilder.addLeaf(columnPredicate);

                                    String values = Joiner.on(",").join(nCopies(discreteValues.getValues().size(), "?"));
                                    String predicateString = columnName + (discreteValues.isInclusive() ? "" : " NOT") + " IN (" + values + ")";
                                    if (domain.isNullAllowed()) {
                                        predicateString = "(" + columnPredicate + " OR " + columnName + " IS NULL)";
                                    }
                                    sqlConjunctsBuilder.add(predicateString);

                                    return 1;
                                },

                                allOrNone -> {
                                    throw new TrinoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "Case should not be reachable");
                                });
                        LOG.debug("Have established %d disjuncts", count);
                    }
                }
            }
            LOG.debug("Equivalent SQL:\n%s",() -> assembleSql(sqlConjunctsBuilder.build()));
        }
        return conjunctsBuilder.build();
    }

    private static String assembleSql(List<String> conjuncts) {
        StringBuilder where = new StringBuilder("WHERE ");
        return Joiner.on(" AND\n").appendTo(where, conjuncts).toString();
    }


    private static ColumnPredicate<?> createDumbEqualsPredicate(CyodaColumnHandle columnHandle, Object nativeValue) {
        return columnHandle.newComparisonPredicateFromNative(ColumnPredicate.ComparisonOp.EQUAL, nativeValue);
    }
}
