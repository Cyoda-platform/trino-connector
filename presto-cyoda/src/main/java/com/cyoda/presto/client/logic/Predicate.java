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

import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.client.types.SupportedDataType;
import com.cyoda.presto.client.util.DecimalUtil;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.common.predicate.DiscreteValues;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A predicate which can be used to filter rows based on the value of a column.
 */
@SuppressWarnings("unused")
public class Predicate<T extends Comparable<T>> {

    private final PredicateType type;
    private final CyodaColumnHandle column;

    /**
     * The inclusive lower bound value if this is a Range predicate, or
     * the createEquality value if this is an Equality predicate.
     */
    private final SupportedDataType<T> lower;

    /**
     * The exclusive upper bound value if this is a Range predicate.
     */
    private final SupportedDataType<T> upper;

    /**
     * IN-list values.
     */
    private final Collection<SupportedDataType<T>> inListValues;

    /**
     * @param type   the predicate type
     * @param column the column to which the predicate applies
     * @param lower  the lower bound serialized value if this is a Range predicate,
     *               or the equality value if this is an Equality predicate
     * @param upper  the upper bound serialized value if this is an Equality predicate
     */
    Predicate(PredicateType type, CyodaColumnHandle column, SupportedDataType<T> lower, SupportedDataType<T> upper) {
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
    private Predicate(CyodaColumnHandle column, SortedSet<SupportedDataType<T>> inListValues) {
        this.column = column;
        this.type = PredicateType.IN_LIST;
        this.lower = null;
        this.upper = null;
        this.inListValues = inListValues;
    }

    /**
     * Creates a new {@code CyodaPredicate} on a boolean column.
     *
     * @param column the column schema
     * @param op     the comparison operation
     * @param value  the value to compare against
     */
    static Predicate<Boolean> newComparisonPredicate(CyodaColumnHandle column,
                                                     ComparisonOp op,
                                                     boolean value) {
        checkColumn(column, DataType.BOOLEAN);
        // Create the comparison predicate. Range predicates on boolean values can
        // always be converted to either an equality, an IS NOT NULL (filtering only
        // null values), or NONE (filtering all values).
        SupportedDataType<Boolean> supportedValue = SupportedDataType.of(value, Boolean.class);
        switch (op) {
            case GREATER: {
                // b > true  -> b NONE
                // b > false -> b = true
                if (value) {
                    return none(column);
                } else {
                    return new Predicate<>(PredicateType.EQUALITY, column, supportedValue, null);
                }
            }
            case GREATER_EQUAL: {
                // b >= true  -> b = true
                // b >= false -> b IS NOT NULL
                if (value) {
                    return new Predicate<>(PredicateType.EQUALITY, column, SupportedDataType.of(true, Boolean.class), null);
                } else {
                    return newIsNotNullPredicate(column);
                }
            }
            case EQUAL:
                return new Predicate<>(PredicateType.EQUALITY, column, supportedValue, null);
            case LESS: {
                // b < true  -> b NONE
                // b < false -> b = true
                if (value) {
                    return new Predicate<>(PredicateType.EQUALITY, column, SupportedDataType.of(false, Boolean.class), null);
                } else {
                    return none(column);
                }
            }
            case LESS_EQUAL: {
                // b <= true  -> b IS NOT NULL
                // b <= false -> b = false
                if (value) {
                    return newIsNotNullPredicate(column);
                } else {
                    return new Predicate<>(PredicateType.EQUALITY, column, SupportedDataType.of(false, Boolean.class), null);
                }
            }
            default:
                throw unknownComparisonException();
        }
    }

    /**
     * Creates a new {@code CyodaPredicate} on a boolean column.
     *
     * @param column the column schema
     * @param op     the comparison operation
     * @param value  the value to compare against
     */
    static Predicate<UUID> newComparisonPredicate(CyodaColumnHandle column,
                                                     ComparisonOp op,
                                                     UUID value) {
        checkColumn(column, DataType.UUID_TYPE);

        SupportedDataType<UUID> wrapped = SupportedDataType.of(value, UUID.class);

        BigInteger bigIntValue = convertToBigInteger(value);

        return delegateToBigDecimal(column, op, wrapped, bigIntValue);

    }

    private static <S extends Comparable<S>> Predicate<S> delegateToBigDecimal(CyodaColumnHandle column, ComparisonOp op, SupportedDataType<S> wrapped, BigInteger bigIntValue) {
        // This is to circumvent the check on data type when delegating to bigdecimal.
        // We won't use the predicate created, so this isn't an issue.
        CyodaColumnHandle bigDecimalColumn = new CyodaColumnHandle(
                column.getConnectorId(),
                column.getColumnName(),
                column.getColumnType(),
                DataType.BIG_DECIMAL,
                column.getOrdinalPosition(),
                column.getRequestHandlerKey(),
                column.getIsNullable()
        );
        Predicate<BigDecimal> bigDecimalPredicate = newComparisonPredicate(bigDecimalColumn, op, new BigDecimal(bigIntValue));

        boolean setLower = bigDecimalPredicate.getLower() != null;
        boolean setUpper = bigDecimalPredicate.getUpper() != null;
        return new Predicate<>(bigDecimalPredicate.getType(), column,
                setLower ? wrapped : null,
                setUpper ? wrapped : null
        );
    }


    private static final BigInteger B = BigInteger.ONE.shiftLeft(64); // 2^64
    private static final BigInteger L = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigInteger MAX_LONG_BIGINT = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigInteger MAX_UUID_VALUE = MAX_LONG_BIGINT.add(MAX_LONG_BIGINT.multiply(B));

    public static BigInteger convertToBigInteger(UUID id)
    {
        BigInteger lo = BigInteger.valueOf(id.getLeastSignificantBits());
        BigInteger hi = BigInteger.valueOf(id.getMostSignificantBits());

        // If any of lo/hi parts is negative interpret as unsigned

        if (hi.signum() < 0)
            hi = hi.add(B);

        if (lo.signum() < 0)
            lo = lo.add(B);

        return lo.add(hi.multiply(B));
    }

    public static UUID convertFromBigInteger(BigInteger x)
    {
        BigInteger[] parts = x.divideAndRemainder(B);
        BigInteger hi = parts[0];
        BigInteger lo = parts[1];

        if (L.compareTo(lo) < 0)
            lo = lo.subtract(B);

        if (L.compareTo(hi) < 0)
            hi = hi.subtract(B);

        return new UUID(hi.longValueExact(), lo.longValueExact());
    }
    /**
     * Creates a new comparison predicate on an integer or timestamp column.
     *
     * @param column the column schema
     * @param op     the comparison operation
     * @param value  the value to compare against
     */
    static Predicate<Long> newComparisonPredicate(CyodaColumnHandle column,
                                                  ComparisonOp op,
                                                  long value) {
        checkColumn(column, DataType.BYTE, DataType.CHARACTER, DataType.SHORT, DataType.INTEGER);

        long minValue = minIntValue(column.getDataType());
        long maxValue = maxIntValue(column.getDataType());
        Preconditions.checkArgument(value <= maxValue && value >= minValue,
                "integer value out of range for %s column: %s",
                column.getDataType(), value);

        if (op == ComparisonOp.LESS_EQUAL) {
            if (value == maxValue) {
                // If the value can't be incremented because it is at the top end of the
                // range, then substitute the predicate with an IS NOT NULL predicate.
                // This has the same effect as an inclusive upper bound on the maximum
                // value. If the column is not nullable then the IS NOT NULL predicate
                // is ignored.
                return newIsNotNullPredicate(column);
            }
            value += 1;
            op = ComparisonOp.LESS;
        } else if (op == ComparisonOp.GREATER) {
            if (value == maxValue) {
                return none(column);
            }
            value += 1;
            op = ComparisonOp.GREATER_EQUAL;
        }

        SupportedDataType<Long> wrapped = SupportedDataType.of(value, Long.class);

        switch (op) {
            case GREATER_EQUAL:
                if (value == minValue) {
                    return newIsNotNullPredicate(column);
                } else if (value == maxValue) {
                    return new Predicate<>(PredicateType.EQUALITY, column, wrapped, null);
                }
                return new Predicate<>(PredicateType.RANGE, column, wrapped, null);
            case EQUAL:
                return new Predicate<>(PredicateType.EQUALITY, column, wrapped, null);
            case LESS:
                if (value == minValue) {
                    return none(column);
                }
                return new Predicate<>(PredicateType.RANGE, column, null, wrapped);
            default:
                throw unknownComparisonException();
        }
    }

    /**
     * Creates a new comparison predicate on a BIGINT column. We delegate the logic to BigDecimal.
     *
     * @param column the column schema
     * @param op     the comparison operation
     * @param value  the value to compare against
     */
    static Predicate<BigInteger> newComparisonPredicate(CyodaColumnHandle column,
                                                        ComparisonOp op,
                                                        BigInteger value) {
        checkColumn(column, DataType.BIG_INTEGER);
        SupportedDataType<BigInteger> wrapped = SupportedDataType.of(value, BigInteger.class);
        return delegateToBigDecimal(column, op, wrapped, value);
    }

    /**
     * Creates a new comparison predicate on a Decimal column.
     *
     * @param column the column schema
     * @param op     the comparison operation
     * @param value  the value to compare against
     */
    static Predicate<BigDecimal> newComparisonPredicate(CyodaColumnHandle column,
                                                        ComparisonOp op,
                                                        BigDecimal value) {
        checkColumn(column, DataType.BIG_DECIMAL);

        BigDecimal minValue = DecimalUtil.minValue(value.precision(), value.scale());
        BigDecimal maxValue = DecimalUtil.maxValue(value.precision(), value.scale());
        Preconditions.checkArgument(value.compareTo(maxValue) <= 0 && value.compareTo(minValue) >= 0,
                "Decimal value out of range for %s column: %s",
                column.getDataType(), value);
        BigDecimal smallestValue = DecimalUtil.smallestValue(value.scale());

        if (op == ComparisonOp.LESS_EQUAL) {
            if (value.equals(maxValue)) {
                // If the value can't be incremented because it is at the top end of the
                // range, then substitute the predicate with an IS NOT NULL predicate.
                // This has the same effect as an inclusive upper bound on the maximum
                // value. If the column is not nullable then the IS NOT NULL predicate
                // is ignored.
                return newIsNotNullPredicate(column);
            }
            value = value.add(smallestValue);
            op = ComparisonOp.LESS;
        } else if (op == ComparisonOp.GREATER) {
            if (value.equals(maxValue)) {
                return none(column);
            }
            value = value.add(smallestValue);
            op = ComparisonOp.GREATER_EQUAL;
        }

        SupportedDataType<BigDecimal> wrapped = SupportedDataType.of(value, BigDecimal.class);

        switch (op) {
            case GREATER_EQUAL:
                if (value.equals(minValue)) {
                    return newIsNotNullPredicate(column);
                } else if (value.equals(maxValue)) {
                    return new Predicate<>(PredicateType.EQUALITY, column, wrapped, null);
                }
                return new Predicate<>(PredicateType.RANGE, column, wrapped, null);
            case EQUAL:
                return new Predicate<>(PredicateType.EQUALITY, column, wrapped, null);
            case LESS:
                if (value.equals(minValue)) {
                    return none(column);
                }
                return new Predicate<>(PredicateType.RANGE, column, null, wrapped);
            default:
                throw unknownComparisonException();
        }
    }


    /**
     * Creates a new comparison predicate on a date column.
     *
     * @param column the column schema
     * @param op     the comparison operation
     * @param value  the value to compare against
     */
    static Predicate<Long> newComparisonPredicate(CyodaColumnHandle column,
                                                  ComparisonOp op,
                                                  Date value) {
        checkColumn(column, DataType.DATE);
        long days = value.toInstant().toEpochMilli();
        return newComparisonPredicate(column, op, days);
    }

    /**
     * Creates a new comparison predicate on a float column.
     *
     * @param column the column schema
     * @param op     the comparison operation
     * @param value  the value to compare against
     */
    static Predicate<Float> newComparisonPredicate(CyodaColumnHandle column,
                                                   ComparisonOp op,
                                                   float value) {
        checkColumn(column, DataType.FLOAT);
        if (op == ComparisonOp.LESS_EQUAL) {
            if (value == Float.POSITIVE_INFINITY) {
                return newIsNotNullPredicate(column);
            }
            value = Math.nextAfter(value, Float.POSITIVE_INFINITY);
            op = ComparisonOp.LESS;
        } else if (op == ComparisonOp.GREATER) {
            if (value == Float.POSITIVE_INFINITY) {
                return none(column);
            }
            value = Math.nextAfter(value, Float.POSITIVE_INFINITY);
            op = ComparisonOp.GREATER_EQUAL;
        }

        SupportedDataType<Float> wrapped = SupportedDataType.of(value, Float.class);

        switch (op) {
            case GREATER_EQUAL:
                if (value == Float.NEGATIVE_INFINITY) {
                    return newIsNotNullPredicate(column);
                } else if (value == Float.POSITIVE_INFINITY) {
                    return new Predicate<>(PredicateType.EQUALITY, column, wrapped, null);
                }
                return new Predicate<>(PredicateType.RANGE, column, wrapped, null);
            case EQUAL:
                return new Predicate<>(PredicateType.EQUALITY, column, wrapped, null);
            case LESS:
                if (value == Float.NEGATIVE_INFINITY) {
                    return none(column);
                }
                return new Predicate<>(PredicateType.RANGE, column, null, wrapped);
            default:
                throw unknownComparisonException();
        }
    }

    /**
     * Creates a new comparison predicate on a double column.
     *
     * @param column the column schema
     * @param op     the comparison operation
     * @param value  the value to compare against
     */
    static Predicate<Double> newComparisonPredicate(CyodaColumnHandle column,
                                                    ComparisonOp op,
                                                    double value) {
        checkColumn(column, DataType.DOUBLE);
        if (op == ComparisonOp.LESS_EQUAL) {
            if (value == Double.POSITIVE_INFINITY) {
                return newIsNotNullPredicate(column);
            }
            value = Math.nextAfter(value, Double.POSITIVE_INFINITY);
            op = ComparisonOp.LESS;
        } else if (op == ComparisonOp.GREATER) {
            if (value == Double.POSITIVE_INFINITY) {
                return none(column);
            }
            value = Math.nextAfter(value, Double.POSITIVE_INFINITY);
            op = ComparisonOp.GREATER_EQUAL;
        }

        SupportedDataType<Double> wrapped = SupportedDataType.of(value, Double.class);

        switch (op) {
            case GREATER_EQUAL:
                if (value == Double.NEGATIVE_INFINITY) {
                    return newIsNotNullPredicate(column);
                } else if (value == Double.POSITIVE_INFINITY) {
                    return new Predicate<>(PredicateType.EQUALITY, column, wrapped, null);
                }
                return new Predicate<>(PredicateType.RANGE, column, wrapped, null);
            case EQUAL:
                return new Predicate<>(PredicateType.EQUALITY, column, wrapped, null);
            case LESS:
                if (value == Double.NEGATIVE_INFINITY) {
                    return none(column);
                }
                return new Predicate<>(PredicateType.RANGE, column, null, wrapped);
            default:
                throw unknownComparisonException();
        }
    }

    /**
     * Creates a new comparison predicate on a string column.
     *
     * @param column the column schema
     * @param op     the comparison operation
     * @param value  the value to compare against
     */
    static Predicate<String> newComparisonPredicate(CyodaColumnHandle column,
                                                    ComparisonOp op,
                                                    String value) {
        checkColumn(column, DataType.STRING);

        byte[] bytes = value.getBytes(UTF_8);

        if (op == ComparisonOp.LESS_EQUAL) {
            bytes = Arrays.copyOf(bytes, bytes.length + 1);
            op = ComparisonOp.LESS;
        } else if (op == ComparisonOp.GREATER) {
            bytes = Arrays.copyOf(bytes, bytes.length + 1);
            op = ComparisonOp.GREATER_EQUAL;
        }

        String string = new String(bytes, UTF_8);
        SupportedDataType<String> wrapped = SupportedDataType.of(string, String.class);

        switch (op) {
            case GREATER_EQUAL:
                if (bytes.length == 0) {
                    return newIsNotNullPredicate(column);
                }
                return new Predicate<>(PredicateType.RANGE, column, wrapped, null);
            case EQUAL:
                return new Predicate<>(PredicateType.EQUALITY, column, wrapped, null);
            case LESS:
                if (bytes.length == 0) {
                    return none(column);
                }
                return new Predicate<>(PredicateType.RANGE, column, null, wrapped);
            default:
                throw unknownComparisonException();
        }
    }


    @SuppressWarnings("java:S1452")
    static Predicate<?> newInListPredicate(CyodaColumnHandle columnHandle, DiscreteValues discreteValues) {
        // TODO: This does not yet cover all cases.
        switch (columnHandle.getDataType()) {
            case LONG:
                return newInListPredicate(columnHandle, discreteValues, Long.class);
            case INTEGER:
                return newInListPredicate(columnHandle, discreteValues, Integer.class);
            case SHORT:
                return newInListPredicate(columnHandle, discreteValues, Short.class);
            case BYTE:
                return newInListPredicate(columnHandle, discreteValues, Byte.class);
            case STRING:
                return newInListPredicate(columnHandle, discreteValues, String.class);
            case DOUBLE:
                return newInListPredicate(columnHandle, discreteValues, Double.class);
            case FLOAT:
                return newInListPredicate(columnHandle, discreteValues, Float.class);
            case BOOLEAN:
                return newInListPredicate(columnHandle, discreteValues, Boolean.class);
            default:
                throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "DataType  " + columnHandle.getDataType() + " not yet supported");
        }
    }

    static <T extends Comparable<T>> Predicate<T> newInListPredicate(
            final CyodaColumnHandle columnHandle,
            final DiscreteValues discreteValues,
            final Class<T> javaType) {
        Type type = columnHandle.getColumnType();
        SortedSet<SupportedDataType<T>> javaValues = discreteValues.getValues().stream()
                .map(nativeValue -> SupportedDataType.ofPrestoNativeValue(type, nativeValue, javaType))
                .sorted()
                .collect(Collectors.toCollection(TreeSet::new));
        return newInListPredicate(columnHandle, javaValues);
    }

    /**
     * Creates a new IN list predicate.
     * <p>
     * The list must contain values of the correct type for the column.
     *
     * @param column the column that the predicate applies to
     * @param values list of values which the column values must match
     *               the type of values, must match the type of the column
     * @return an IN list predicate
     */
    static <T extends Comparable<T>> Predicate<T> newInListPredicate(
            final CyodaColumnHandle column,
            final SortedSet<SupportedDataType<T>> values
    ) {
        if (values.isEmpty()) {
            return none(column);
        }
        return buildInList(column, values);
    }


    /**
     * Creates a new {@code IS NOT NULL} predicate.
     *
     * @param column the column that the predicate applies to
     * @return an {@code IS NOT NULL} predicate
     */
    public static <T extends Comparable<T>> Predicate<T> newIsNotNullPredicate(CyodaColumnHandle column) {
        return new Predicate<>(PredicateType.IS_NOT_NULL, column, null, null);
    }

    public static Predicate<Any> newIsNotNullPredicateAny(CyodaColumnHandle column) {
        return newIsNotNullPredicate(column);
    }

    /**
     * Creates a new {@code IS NULL} predicate.
     *
     * @param column the column that the predicate applies to
     * @return an {@code IS NULL} predicate
     */
    public static <T extends Comparable<T>> Predicate<T> newIsNullPredicate(CyodaColumnHandle column) {
        if (!column.getIsNullable()) {
            return none(column);
        }
        return new Predicate<>(PredicateType.IS_NULL, column, null, null);
    }

    public static Predicate<Any> newIsNullPredicateAny(CyodaColumnHandle column) {
        return newIsNullPredicate(column);
    }


    private static IllegalArgumentException unknownComparisonException() {
        return new IllegalArgumentException("unknown comparison op");
    }

    /**
     * Factory function for a {@code None} predicate.
     *
     * @param column the column to which the predicate applies
     * @return a None predicate
     */
    static <T extends Comparable<T>> Predicate<T> none(CyodaColumnHandle column) {
        return new Predicate<>(PredicateType.NONE, column, null, null);
    }

    /**
     * Factory function for a {@code None} predicate.
     *
     * @param column the column to which the predicate applies
     * @return a None predicate
     */
    static Predicate<Any> all(CyodaColumnHandle column) {
        return new Predicate<>(PredicateType.ALL, column, null, null);
    }

    /**
     * Builds an IN list predicate from a collection of raw values. The collection
     * must be sorted and deduplicated.
     *
     * @param column the column
     * @param values the IN list values
     * @return an IN list predicate
     */
    private static <T extends Comparable<T>> Predicate<T> buildInList(
            CyodaColumnHandle column, SortedSet<SupportedDataType<T>> values
    ) {
        // IN (true, false) predicates can be simplified to IS NOT NULL.
        if (column.getDataType() == DataType.BOOLEAN && values.size() > 1) {
            return newIsNotNullPredicate(column);
        }

        switch (values.size()) {
            case 0:
                return Predicate.none(column);
            case 1:
                return new Predicate<>(PredicateType.EQUALITY, column, values.iterator().next(), null);
            default:
                return new Predicate<>(column, values);
        }
    }

    /**
     * Returns the maximum value for the integer type.
     *
     * @param dataType an integer type
     * @return the maximum value
     */
    static long maxIntValue(DataType dataType) {
        switch (dataType) {
            case BYTE:
                return Byte.MAX_VALUE;
            case SHORT:
                return Short.MAX_VALUE;
            case INTEGER:
                return Integer.MAX_VALUE;
            case LONG:
                return Long.MAX_VALUE;
            default:
                throw new IllegalArgumentException("type must be an integer type");
        }
    }

    /**
     * Returns the minimum value for the integer type.
     *
     * @param dataType an integer type
     * @return the minimum value
     */
    static long minIntValue(DataType dataType) {
        switch (dataType) {
            case BYTE:
                return Byte.MIN_VALUE;
            case SHORT:
                return Short.MIN_VALUE;
            case INTEGER:
                return Integer.MIN_VALUE;
            case LONG:
                return Long.MIN_VALUE;
            default:
                throw new IllegalArgumentException("type must be an integer type");
        }
    }

    /**
     * Checks that the column is one of the expected types.
     *
     * @param column      the column being checked
     * @param passedTypes the expected types (logical OR)
     */
    private static void checkColumn(CyodaColumnHandle column, DataType... passedTypes) {
        for (DataType type : passedTypes) {
            if (column.getDataType().equals(type)) {
                return;
            }
        }
        throw new IllegalArgumentException(String.format("%s's type isn't %s, it's %s",
                column.getColumnName(), Arrays.toString(Arrays.stream(passedTypes).toArray()),
                column.getColumnType().getDisplayName()));
    }

    public static <T extends Comparable<T>> Predicate<T> nothing() {
        return new Predicate<>(null, null, null, null);
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
    public SupportedDataType<T> getLower() {
        return lower;
    }

    /**
     * @return the upper bound.
     */
    public SupportedDataType<T> getUpper() {
        return upper;
    }

    /**
     * @return the IN list values. Always kept sorted and de-duplicated.
     */
    public Collection<SupportedDataType<T>> getInListValues() {
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
    Predicate<T> merge(Predicate<T> other) {
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
                    if (lower != null && lower.compareTo(other.lower) != 0) {
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
                    SupportedDataType<T> newLower = other.lower == null ||
                            (lower != null && lower.compareTo(other.lower) >= 0) ? lower : other.lower;
                    SupportedDataType<T> newUpper = other.upper == null ||
                            (upper != null && upper.compareTo(other.upper) <= 0) ? upper : other.upper;
                    if (newLower != null && newUpper != null && newLower.compareTo(newUpper) >= 0) {
                        return none(column);
                    } else {
                        if (newLower != null && newUpper != null && areConsecutive(newLower, newUpper)) {
                            return new Predicate<>(PredicateType.EQUALITY, column, newLower, null);
                        } else {
                            return new Predicate<>(PredicateType.RANGE, column, newLower, newUpper);
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
                    TreeSet<SupportedDataType<T>> values = new TreeSet<>();
                    for (SupportedDataType<T> value : Optional.ofNullable(inListValues).orElse(Collections.emptySortedSet())) {
                        if (other.rangeContains(value)) {
                            values.add(value);
                        }
                    }
                    return buildInList(column, values);
                } else {
                    Preconditions.checkState(other.type == PredicateType.IN_LIST);
                    TreeSet<SupportedDataType<T>> values = new TreeSet<>();
                    for (SupportedDataType<T> value : Optional.ofNullable(inListValues).orElse(Collections.emptySortedSet())) {
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
    boolean inListContains(SupportedDataType<T> value) {
        return Optional.ofNullable(inListValues).map(it -> it.contains(value)).orElse(false);
    }

    /**
     * @param value the value to check
     * @return {@code true} if this RANGE predicate contains the value
     */
    boolean rangeContains(SupportedDataType<T> value) {
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
    private boolean areConsecutive(SupportedDataType<T> a, SupportedDataType<T> b) {
        switch (a.dataType) {
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
                return m.equals(n.substring(0, m.length() - 1));
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
            default:
                throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, String.format("unknown column type %s", column.getColumnType()));
        }
    }

    /**
     * This is not meant for general use, but only locally in this package
     *
     * @return a new instance of the Predicate, with the opposite logic.
     * TODO: Haven't tested if this makes any sense!
     */
    Predicate<T> negate() {
        return this.negate(true);
    }

    Predicate<T> negate(boolean negate) {
        if (!negate) return this;
        switch (type) {
            case NONE:
                return new Predicate<>(PredicateType.ALL, column, lower, upper);
            case ALL:
                return new Predicate<>(PredicateType.NONE, column, lower, upper);
            case EQUALITY:
                return new Predicate<>(PredicateType.INEQUALITY, column, lower, upper);
            case INEQUALITY:
                return new Predicate<>(PredicateType.EQUALITY, column, lower, upper);
            case RANGE:
                return new Predicate<>(PredicateType.NOT_RANGE, column, lower, upper);
            case IS_NULL:
                return new Predicate<>(PredicateType.IS_NOT_NULL, column, lower, upper);
            case IS_NOT_NULL:
                return new Predicate<>(PredicateType.IS_NULL, column, lower, upper);
            case IN_LIST:
                return new Predicate<>(PredicateType.NOT_IN_LIST, column, lower, upper);
            case NOT_IN_LIST:
                return new Predicate<>(PredicateType.IN_LIST, column, lower, upper);
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

                List<String> strings = Optional.ofNullable(inListValues).orElse(Collections.emptySortedSet()).stream()
                        .map(SupportedDataType::stringify)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
                return String.format("`%s` IN (%s)", column.getColumnName(), Joiner.on(", ").join(strings));
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

    private Optional<String> valueToString(SupportedDataType<?> value) {
        if ( value == null ) return Optional.empty();
        return value.stringify();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Predicate<?> that = (Predicate<?>) o;
        return type == that.type && column.equals(that.column) && Objects.equals(lower, that.lower) && Objects.equals(upper, that.upper) && Objects.equals(inListValues, that.inListValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, column, lower, upper, inListValues);
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
