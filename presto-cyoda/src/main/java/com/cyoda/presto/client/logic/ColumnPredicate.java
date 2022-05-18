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

import com.cyoda.presto.client.types.DataTypeValue;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import static com.cyoda.presto.client.logic.ColumnPredicateUtils.buildInList;
import static com.cyoda.presto.client.logic.ColumnPredicateUtils.none;

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
    private final DataTypeValue<T> lower;

    /**
     * The exclusive upper bound value if this is a Range predicate.
     */
    private final DataTypeValue<T> upper;

    /**
     * IN-list values.
     */
    private final SortedSet<DataTypeValue<T>> inListValues;

    /**
     * @param type   the predicate type
     * @param column the column to which the predicate applies
     * @param lower  the lower bound serialized value if this is a Range predicate,
     *               or the equality value if this is an Equality predicate
     * @param upper  the upper bound serialized value if this is an Equality predicate
     */
    ColumnPredicate(PredicateType type, CyodaColumnHandle column, DataTypeValue<T> lower, DataTypeValue<T> upper) {
        this.type = type;
        this.column = column;
        this.lower = lower;
        this.upper = upper;
        this.inListValues = null;
    }

    /**
     * Constructor for IN list predicate.
     *
     * @param column       the column to which the predicate applies
     * @param inListValues the encoded IN list values
     */
    public ColumnPredicate(CyodaColumnHandle column, SortedSet<DataTypeValue<T>> inListValues) {
        this.column = column;
        this.type = PredicateType.IN_LIST;
        this.lower = null;
        this.upper = null;
        this.inListValues = inListValues;
    }


    public <S extends Comparable<? super S>> ColumnPredicate<S> cloneTo(CyodaColumnHandle column, Function<DataTypeValue<T>,S> func ) {
        Optional<S> lowerCast = Optional.ofNullable(this.getLower()).map(func);
        Optional<S> upperCast = Optional.ofNullable(this.getUpper()).map(func);
        return new ColumnPredicate<>(this.getType(),column,
                lowerCast.map(DataTypeValue::of).orElse(null),
                upperCast.map(DataTypeValue::of).orElse(null)
        );
    }

    public CyodaColumnHandle getColumn() {
        return column;
    }

    public PredicateType getType() {
        return type;
    }

    /**
     * @return the lower bound.
     */
    public DataTypeValue<T> getLower() {
        return lower;
    }

    /**
     * @return the upper bound.
     */
    public DataTypeValue<T> getUpper() {
        return upper;
    }

    /**
     * @return the IN list values. Always kept sorted and de-duplicated.
     */
    public Collection<DataTypeValue<T>> getInListValues() {
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
    ColumnPredicate<T> merge(ColumnPredicate<T> other) {
        Preconditions.checkArgument(column.equals(other.column),
                "predicates from different columns may not be merged");

        // First, consider other.type == NONE, IS_NOT_NULL, or IS_NULL
        // NONE predicates dominate.
        if (other.type == PredicateType.NONE) {
            return other;
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
                    DataTypeValue<T> newLower = other.lower == null ||
                            (lower != null && lower.compareTo(other.lower) >= 0) ? lower : other.lower;
                    DataTypeValue<T> newUpper = other.upper == null ||
                            (upper != null && upper.compareTo(other.upper) <= 0) ? upper : other.upper;
                    if (newLower != null && newUpper != null && newLower.compareTo(newUpper) >= 0) {
                        return none(column);
                    } else {
                        if (newLower != null && newUpper != null && areConsecutive(newLower, newUpper)) {
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
                    TreeSet<DataTypeValue<T>> values = new TreeSet<>();
                    for (DataTypeValue<T> value : Optional.ofNullable(inListValues).orElse(Collections.emptySortedSet())) {
                        if (other.rangeContains(value)) {
                            values.add(value);
                        }
                    }
                    return buildInList(column, values);
                } else {
                    Preconditions.checkState(other.type == PredicateType.IN_LIST);
                    TreeSet<DataTypeValue<T>> values = new TreeSet<>();
                    for (DataTypeValue<T> value : Optional.ofNullable(inListValues).orElse(Collections.emptySortedSet())) {
                        if (other.inListContains(value)) {
                            values.add(value);
                        }
                    }
                    return buildInList(column, values);
                }
            }
            default:
                throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, String.format("unknown predicate type %s", this));
        }
    }

    /**
     * @param value the value to check for
     * @return {@code true} if this IN list predicate contains the value
     */
    boolean inListContains(DataTypeValue<T> value) {
        return Optional.ofNullable(inListValues).map(it -> it.contains(value)).orElse(false);
    }

    /**
     * @param value the value to check
     * @return {@code true} if this RANGE predicate contains the value
     */
    boolean rangeContains(DataTypeValue<T> value) {
        return (lower == null || value.compareTo(lower) >= 0) &&
                (upper == null || value.compareTo(upper) < 0);
    }


    /**
     * Returns true if increment(a) == b.
     *
     * @param a the value which would be incremented
     * @param b the target value
     * @return true if increment(a) == b
     */
    @SuppressWarnings("java:S3776")
    private boolean areConsecutive(DataTypeValue<T> a, DataTypeValue<T> b) {
        switch (a.supportedDataType.getDataType()) {
            case BOOLEAN:
                return false;
            case BYTE: {
                byte m = a.asByte();
                byte n = b.asByte();
                return m < n && m + (byte) 1 == n;
            }
            case SHORT: {
                short m = a.asShort();
                short n = b.asShort();
                return m < n && m + (short) 1 == n;
            }
            case INTEGER: {
                int m = a.asInt();
                int n = b.asInt();
                return m < n && m + 1 == n;
            }
            case LONG: {
                long m = a.asLong();
                long n = b.asLong();
                return m < n && m + 1 == n;
            }
            case DATE: {
                long m = a.asDate().getTime();
                long n = b.asDate().getTime();
                return m < n && m + 1 == n;
            }
            case BIG_INTEGER: {
                BigInteger m = a.asBigInteger();
                BigInteger n = b.asBigInteger();
                return m.compareTo(n) < 0 && m.add(BigInteger.ONE).equals(n);
            }

            case FLOAT: {
                float m = a.asFloat();
                float n = b.asFloat();
                return m < n && Math.nextAfter(m, Float.POSITIVE_INFINITY) == n;
            }
            case DOUBLE: {
                double m = a.asDouble();
                double n = b.asDouble();
                return m < n && Math.nextAfter(m, Double.POSITIVE_INFINITY) == n;
            }
            case BIG_DECIMAL: {
                BigDecimal m = a.asBigDecimal();
                BigDecimal n = b.asBigDecimal();
                return m.compareTo(n) < 0 && m.add(BigDecimal.ONE).equals(n);

            }
            case STRING: {
                String m = a.asString();
                String n = b.asString();
                if (m.length() + 1 != n.length() || n.charAt(n.length() - 1) != 0) {
                    return false;
                }
                return m.equals(n.substring(0, n.length() - 1));
            }
            case BYTE_ARRAY: {
                byte[] m = a.asByteArray();
                byte[] n = b.asByteArray();
                if (m.length + 1 != n.length || n[m.length] != 0) {
                    return false;
                }
                for (int i = 0; i < m.length; i++) {
                    if (m[i] != n[i]) {
                        return false;
                    }
                }
                return true;
            }
            case BYTE_BUFFER: {
                ByteBuffer mBuffer = a.asByteBuffer();
                ByteBuffer nBuffer = b.asByteBuffer();
                byte[] m = new byte[mBuffer.remaining()];
                try {
                    mBuffer.get(m);
                } finally {
                    mBuffer.rewind();
                }
                byte[] n = new byte[nBuffer.remaining()];
                try {
                    nBuffer.get(n);
                } finally {
                    nBuffer.rewind();
                }
                if (m.length + 1 != n.length || n[m.length] != 0) {
                    return false;
                }
                for (int i = 0; i < m.length; i++) {
                    if (m[i] != n[i]) {
                        return false;
                    }
                }
                return true;
            }
            default:
                throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, String.format("unknown column type %s", column.getColumnType()));
        }
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
                throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "Type" + type + " cannot be negated");
        }
    }

    @Override
    public String toString() {
        switch (type) {
            case EQUALITY:
                return String.format("`%s` = %s", column.getColumnName(),
                        valueToString(lower));
            case RANGE: {
                if (lower == null) {
                    return String.format("`%s` < %s", column.getColumnName(), valueToString(upper));
                } else if (upper == null) {
                    return String.format("`%s` >= %s", column.getColumnName(), valueToString(lower));
                } else {
                    return String.format("`%s` >= %s AND `%s` < %s",
                            column.getColumnName(), valueToString(lower),
                            column.getColumnName(), valueToString(upper));
                }
            }
            case IN_LIST: {

                ImmutableList.Builder<String> builder = ImmutableList.builder();
                Iterator<DataTypeValue<T>> iterator = Optional.ofNullable(inListValues).map(Set::iterator).orElse(Collections.emptyIterator());
                while (iterator.hasNext()) {
                    builder.add(Optional.ofNullable(valueToString(iterator.next())).orElse("NULL"));
                }
                return String.format("`%s` IN (%s)", column.getColumnName(), Joiner.on(", ").join(builder.build()));
            }
            case IS_NOT_NULL:
                return String.format("`%s` IS NOT NULL", column.getColumnName());
            case IS_NULL:
                return String.format("`%s` IS NULL", column.getColumnName());
            case NONE:
                return String.format("`%s` NONE", column.getColumnName());
            default:
                throw new IllegalArgumentException(String.format("unknown predicate type %s", type));
        }
    }

    private String valueToString(DataTypeValue<?> value) {
        if ( value == null ) return null;
        if ( value.value instanceof String ) {
            return "\"" + value.value + '"';
        }
        if ( value.value instanceof ByteBuffer ) {
            ByteBuffer byteBuffer = (ByteBuffer) value.value;
            byte[] m = new byte[byteBuffer.remaining()];
            try {
                byteBuffer.get(m);
            } finally {
                byteBuffer.rewind();
            }
            return hex(m);
        }
        return value.stringify().orElse(null);
    }

    public static String hex(byte[] bytes) {
        return "0" + 'x' + BaseEncoding.base16().encode(bytes);
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
