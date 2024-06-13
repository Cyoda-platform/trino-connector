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
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.predicate.Domain;
import io.trino.spi.predicate.Range;
import io.trino.spi.predicate.TupleDomain;
import io.trino.spi.TrinoException;
import io.trino.spi.StandardErrorCode;
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Collections.nCopies;

/**
 * This mimics the code in
 * io.trino.raptor.systemtables.PreparedStatementBuilder and returns the SQL string generated.
 *
 */
public class PredicateBuilderDebugger {

    private PredicateBuilderDebugger() {}

    static String debug(TupleDomain<ColumnHandle> constraintSummary) {
        List<TupleDomain.ColumnDomain<ColumnHandle>> columnDomains = constraintSummary.getColumnDomains()
                .orElse(Collections.emptyList());
        List<Object> conjuncts = columnDomains.stream().map(PredicateBuilderDebugger::debug).collect(Collectors.toList());
        StringBuilder where = new StringBuilder("WHERE ");
        return Joiner.on(" AND\n").appendTo(where, conjuncts).toString();
    }

    private static String debug(
            TupleDomain.ColumnDomain<ColumnHandle> columnDomain) {
        Domain domain = columnDomain.getDomain();
        ColumnHandle columnHandle = columnDomain.getColumn();

        String columnName = ((CyodaColumnHandle)columnHandle).getColumnName();
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
                    String predicate = columnName + (discreteValues.isInclusive() ? "" : " NOT") + " IN (" + values + ")";
                    if (domain.isNullAllowed()) {
                        predicate = "(" + predicate + " OR " + columnName + " IS NULL)";
                    }
                    return predicate;
                },

                allOrNone -> {
                    throw new TrinoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "Case should not be reachable");
                });
    }

    private static String toBindPredicate(String columnName, String operator) {
        return format("%s %s ?", columnName, operator);
    }
}
