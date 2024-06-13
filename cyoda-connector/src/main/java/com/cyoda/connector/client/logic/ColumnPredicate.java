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

package com.cyoda.connector.client.logic;

import com.cyoda.connector.client.logic.converters.PrestoValueConverter;
import com.cyoda.connector.handles.CyodaColumnHandle;
import io.trino.spi.TrinoException;
import io.trino.spi.StandardErrorCode;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A predicate which can be used to filter rows based on the value of a column.
 * Adopted from org.apache.kudu.client.KuduPredicate of org.apache.kudu:kudu-client
 */
@SuppressWarnings("unused")
public class ColumnPredicate<T extends Comparable<? super T>> {

    private final PredicateType type;
    private final CyodaColumnHandle column;

    /**
     * The inclusive lower bound value if this is a Range predicate, or
     * the createEquality value if this is an Equality predicate.
     */
    private final T lower;

    /**
     * The exclusive upper bound value if this is a Range predicate.
     */
    private final T upper;

    /**
     * IN-list values.
     */
    private final SortedSet<T> inListValues;

    private final PrestoValueConverter<T> converter;

    /**
     * @param type   the predicate type
     * @param column the column to which the predicate applies
     * @param lower  the lower bound serialized value if this is a Range predicate,
     *               or the equality value if this is an Equality predicate
     * @param upper  the upper bound serialized value if this is an Equality predicate
     */
    public ColumnPredicate(PredicateType type, CyodaColumnHandle column, T lower, T upper) {
        this.type = type;
        this.column = column;
        this.lower = lower;
        this.upper = upper;
        this.inListValues = null;
        converter = (PrestoValueConverter<T>) column.getConverter();
    }

    /**
     * Constructor for IN list predicate.
     *
     * @param column       the column to which the predicate applies
     * @param inListValues the encoded IN list values
     */
    public ColumnPredicate(CyodaColumnHandle column, SortedSet<T> inListValues) {
        this.column = column;
        this.type = PredicateType.IN_LIST;
        this.lower = null;
        this.upper = null;
        this.inListValues = inListValues;
        converter = (PrestoValueConverter<T>) column.getConverter();
    }

    /**
     * Builds an IN list predicate from a collection of raw values. The collection
     * must be sorted and deduplicated.
     *
     * @param column the column
     * @param values     the IN list values
     * @return an IN list predicate
     */
    public static <T extends Comparable<? super T>> ColumnPredicate<T> buildInList(
            CyodaColumnHandle column, SortedSet<T> values
    ) {
        switch (values.size()) {
            case 0:
                return none(column);
            case 1:
                return new ColumnPredicate<>(PredicateType.EQUALITY, column, values.iterator().next(), null);
            default:
                return new ColumnPredicate<>(column, values);
        }
    }

    /**
     * Factory function for a {@code None} predicate.
     *
     * @param column the column to which the predicate applies
     * @return a None predicate
     */
    public static <T extends Comparable<? super T>> ColumnPredicate<T> none(CyodaColumnHandle column) {
        return new ColumnPredicate<>(PredicateType.NONE, column, null, null);
    }

    /**
     * Factory function for a predicate that filters nothing on the given column
     *
     * @param column the column to which the predicate applies
     * @return a ALL predicate
     */
    public static <T extends Comparable<? super T>> ColumnPredicate<T> all(CyodaColumnHandle column) {
        return new ColumnPredicate<>(PredicateType.ALL, column, null, null);
    }

    /**
     * Creates a new {@code IS NOT NULL} predicate.
     *
     * @param column the column that the predicate applies to
     * @return an {@code IS NOT NULL} predicate
     */
    public static <T extends Comparable<? super T>> ColumnPredicate<T> isNotNull(CyodaColumnHandle column) {
        if (!column.getIsNullable()){
            return all(column);
        }
        return new ColumnPredicate<>(PredicateType.IS_NOT_NULL, column, null, null);
    }

    /**
     * Creates a new {@code IS NULL} predicate.
     *
     * @param column the column that the predicate applies to
     * @return an {@code IS NULL} predicate
     */
    public static <T extends Comparable<T>> ColumnPredicate<T> isNull(CyodaColumnHandle column) {
        if (!column.getIsNullable()) {
            return none(column);
        }
        return new ColumnPredicate<>(PredicateType.IS_NULL, column, null, null);
    }

    public String getColumnName() {
        return column.getColumnName();
    }

    public CyodaColumnHandle getColumn(){
        return column;
    }

    public PredicateType getType() {
        return type;
    }

    /**
     * @return the lower bound.
     */
    public T getLower() {
        return lower;
    }

    /**
     * @return the upper bound.
     */
    public T getUpper() {
        return upper;
    }

    /**
     * @return the IN list values. Always kept sorted and de-duplicated.
     */
    public Collection<T> getInListValues() {
        return inListValues;
    }

    /**
     * Merges another {@code ColumnPredicate} into this one, returning a new
     * {@code ColumnPredicate} which matches the logical intersection ({@code AND})
     * of the input predicates.
     *
     * @param other the predicate to merge with this predicate
     * @return a new predicate that is the logical intersection
     */
    @SuppressWarnings("java:S3776")
    public ColumnPredicate<T> merge(ColumnPredicate<T> other) {
        Preconditions.checkArgument(column.getColumnName().equals(other.getColumnName()),
                "predicates from different column names may not be merged");

        // First, consider other.type == NONE, IS_NOT_NULL, or IS_NULL
        // NONE predicates dominate.
        if (other.type == PredicateType.NONE) {
            return other;
        }
        if (other.type == PredicateType.ALL) {
            return this;
        }

        // NOT NULL is dominated by all other predicates,
        // except IS NULL, for which the merge is NONE.
        if (other.type == PredicateType.IS_NOT_NULL) {
            return type == PredicateType.IS_NULL ? none(column) : this;
        }

        // NULL merged with any predicate type besides itself is NONE.
        if (other.type == PredicateType.IS_NULL) {
            return type == PredicateType.IS_NULL ? this : none(column);
        }

        // Now other.type == EQUALITY, RANGE, or IN_LIST.
        switch (type) {
            case NONE:
                return this;
            case ALL:
            case IS_NOT_NULL:
                return other;
            case IS_NULL:
                return none(column);
            case EQUALITY: {
                if (other.type == PredicateType.EQUALITY) {
                    if (lower != null && other.lower != null && lower.compareTo(other.lower) != 0) {
                        return none(this.column);
                    } else {
                        return this;
                    }
                } else if (other.type == PredicateType.RANGE) {
                    if (other.rangeContains(lower)) {
                        return this;
                    } else {
                        return none(this.column);
                    }
                } else {
                    Preconditions.checkState(other.type == PredicateType.IN_LIST);
                    return other.merge(this);
                }
            }
            case RANGE: {
                if (other.type == PredicateType.EQUALITY || other.type == PredicateType.IN_LIST) {
                    return other.merge(this);
                } else {
                    Preconditions.checkState(other.type == PredicateType.RANGE);
                    T newLower = other.lower == null ||
                            (lower != null && lower.compareTo(other.lower) >= 0) ? lower : other.lower;
                    T newUpper = other.upper == null ||
                            (upper != null && upper.compareTo(other.upper) <= 0) ? upper : other.upper;
                    if (newLower != null && newUpper != null && newLower.compareTo(newUpper) >= 0) {
                        return none(column);
                    } else {
                        if (newLower != null && newUpper != null && converter.areConsecutive(newLower, newUpper)) {
                            return new ColumnPredicate<>(PredicateType.EQUALITY, column, newLower, null);
                        } else {
                            return new ColumnPredicate<>(PredicateType.RANGE, column, newLower, newUpper);
                        }
                    }
                }
            }
            case IN_LIST: {
                if (other.type == PredicateType.EQUALITY) {
                    if (this.inListContains(other.lower)) {
                        return other;
                    } else {
                        return none(column);
                    }
                } else if (other.type == PredicateType.RANGE) {
                    TreeSet<T> values = new TreeSet<>();
                    for (T value : Optional.ofNullable(inListValues).orElse(Collections.emptySortedSet())) {
                        if (other.rangeContains(value)) {
                            values.add(value);
                        }
                    }
                    return buildInList(column, values);
                } else {
                    Preconditions.checkState(other.type == PredicateType.IN_LIST);
                    TreeSet<T> values = new TreeSet<>();
                    for (T value : Optional.ofNullable(inListValues).orElse(Collections.emptySortedSet())) {
                        if (other.inListContains(value)) {
                            values.add(value);
                        }
                    }
                    return buildInList(column, values);
                }
            }
            default:
                throw new TrinoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, String.format("unknown predicate type %s", this));
        }
    }

    /**
     * @param value the value to check for
     * @return {@code true} if this IN list predicate contains the value
     */
    boolean inListContains(T value) {
        return Optional.ofNullable(inListValues).map(it -> it.contains(value)).orElse(false);
    }

    /**
     * @param value the value to check
     * @return {@code true} if this RANGE predicate contains the value
     */
    boolean rangeContains(T value) {
        return (lower == null || value.compareTo(lower) >= 0) &&
                (upper == null || value.compareTo(upper) < 0);
    }


    /**
     * This is not meant for general use, but only locally in this package
     *
     * @return a new instance of the Predicate, with the opposite logic.
     */
    ColumnPredicate<T> negate() {
        return this.negate(true);
    }

    ColumnPredicate<T> negate(boolean negate) {
        if (!negate) return this;
        switch (type) {
            case NONE:
                return new ColumnPredicate<>(PredicateType.ALL, column, lower, upper);
            case ALL:
                return new ColumnPredicate<>(PredicateType.NONE, column, lower, upper);
            case EQUALITY:
                return new ColumnPredicate<>(PredicateType.INEQUALITY, column, lower, upper);
            case INEQUALITY:
                return new ColumnPredicate<>(PredicateType.EQUALITY, column, lower, upper);
            case RANGE:
                return new ColumnPredicate<>(PredicateType.NOT_RANGE, column, lower, upper);
            case IS_NULL:
                return new ColumnPredicate<>(PredicateType.IS_NOT_NULL, column, lower, upper);
            case IS_NOT_NULL:
                return new ColumnPredicate<>(PredicateType.IS_NULL, column, lower, upper);
            case IN_LIST:
                return new ColumnPredicate<>(PredicateType.NOT_IN_LIST, column, lower, upper);
            case NOT_IN_LIST:
                return new ColumnPredicate<>(PredicateType.IN_LIST, column, lower, upper);
            default:
                throw new TrinoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "Type" + type + " cannot be negated");
        }
    }

    @Override
    public String toString() {
        String columnName = getColumnName();
        switch (type) {
            case EQUALITY:
                return String.format("`%s` = %s", columnName,
                        converter.stringify(lower));
            case RANGE: {
                if (lower == null) {
                    return String.format("`%s` < %s", columnName, converter.stringify(upper));
                } else if (upper == null) {
                    return String.format("`%s` >= %s", columnName, converter.stringify(lower));
                } else {
                    return String.format("`%s` >= %s AND `%s` < %s",
                            columnName, converter.stringify(lower),
                            columnName, converter.stringify(upper));
                }
            }
            case IN_LIST: {

                ImmutableList.Builder<String> builder = ImmutableList.builder();
                Iterator<T> iterator = Optional.ofNullable(inListValues).map(Set::iterator).orElse(Collections.emptyIterator());
                while (iterator.hasNext()) {
                    builder.add(Optional.ofNullable(converter.stringify((T) iterator.next())).orElse("NULL"));
                }
                return String.format("`%s` IN (%s)", columnName, Joiner.on(", ").join(builder.build()));
            }
            case IS_NOT_NULL:
                return String.format("`%s` IS NOT NULL", columnName);
            case IS_NULL:
                return String.format("`%s` IS NULL", columnName);
            case NONE:
                return String.format("`%s` NONE", columnName);
            case ALL:

            default:
                throw new IllegalArgumentException(String.format("unknown predicate type %s", type));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnPredicate<?> columnPredicate = (ColumnPredicate<?>) o;
        return type == columnPredicate.type &&
                Objects.equal(column, columnPredicate.column) &&
                Objects.equal(lower, columnPredicate.lower) &&
                Objects.equal(upper, columnPredicate.upper) &&
                Objects.equal(inListValues, columnPredicate.inListValues);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, column, lower, upper, inListValues);
    }

    /**
     * The predicate type.
     */
    public enum PredicateType {
        /**
         * A predicate which filters all rows.
         */
        NONE,
        /**
         * A predicate which doesn't filter at all.
         */
        ALL,
        /**
         * A predicate which filters all rows not equal to a value.
         */
        EQUALITY,
        /**
         * A predicate which filters all rows equal to a value.
         */
        INEQUALITY,
        /**
         * A predicate which filters all rows not in a range.
         */
        RANGE,
        /**
         * A predicate which filters all rows not in a range.
         */
        NOT_RANGE,
        /**
         * A predicate which filters all null rows.
         */
        IS_NOT_NULL,
        /**
         * A predicate which filters all non-null rows.
         */
        IS_NULL,
        /**
         * A predicate which filters all rows not matching a list of values.
         */
        IN_LIST,
        /**
         * A predicate which filters all rows matching a list of values.
         */
        NOT_IN_LIST,
    }

    /**
     * The comparison operator of a predicate.
     */
    public enum ComparisonOp {
        GREATER,
        GREATER_EQUAL,
        EQUAL,
        LESS,
        LESS_EQUAL,
    }
}
