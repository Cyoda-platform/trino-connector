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

import com.cyoda.presto.client.logic.converters.PrestoValueConverterProvider;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.client.types.DataTypeValue;
import com.cyoda.presto.client.types.impl.LocalDateDataType;
import com.cyoda.presto.client.util.DecimalUtil;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.common.predicate.DiscreteValues;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.google.common.base.Preconditions;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.logic.ColumnPredicate.ComparisonOp.EQUAL;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ColumnPredicateUtils {

    private ColumnPredicateUtils() {
        // Utils class
    }

    /**
     * Creates a new {@code CyodaPredicate} on a boolean column.
     *
     * @param column the column schema
     * @param op     the comparison operation
     * @param value  the value to compare against
     */
    static ColumnPredicate<Boolean> newComparisonPredicate(CyodaColumnHandle column,
                                                           ColumnPredicate.ComparisonOp op,
                                                           boolean value) {
        checkColumn(column, DataType.BOOLEAN);
        // Create the comparison predicate. Range predicates on boolean values can
        // always be converted to either an equality, an IS NOT NULL (filtering only
        // null values), or NONE (filtering all values).
        DataTypeValue<Boolean> supportedValue = DataTypeValue.of(value);
        switch (op) {
            case GREATER: {
                // b > true  -> b NONE
                // b > false -> b = true
                if (value) {
                    return none(column);
                } else {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, DataTypeValue.of(true), null);
                }
            }
            case GREATER_EQUAL: {
                // b >= true  -> b = true
                // b >= false -> b IS NOT NULL
                if (value) {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, DataTypeValue.of(true), null);
                } else {
                    return newIsNotNullPredicate(column);
                }
            }
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, supportedValue, null);
            case LESS: {
                // b < true  -> b NONE
                // b < false -> b = true
                if (value) {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, DataTypeValue.of(false), null);
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
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, DataTypeValue.of(false), null);
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
    static ColumnPredicate<UUID> newComparisonPredicate(CyodaColumnHandle column,
                                                        ColumnPredicate.ComparisonOp op,
                                                        UUID value) {
        checkColumn(column, DataType.UUID_TYPE);

        BigInteger bigIntValue = convertToBigInteger(value);
        return delegateToBigDecimal(column, op, bigIntValue)
                .cloneTo(column,item->{
                    BigInteger bigInteger = item.asBigDecimal().toBigIntegerExact();
                    return convertFromBigInteger(bigInteger);
                });
    }

    private static ColumnPredicate<BigDecimal> delegateToBigDecimal(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, BigInteger bigIntValue) {
        // This is to circumvent the check on data type when delegating to BigDecimal.
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
        return newComparisonPredicate(bigDecimalColumn, op, new BigDecimal(bigIntValue));
    }

    private static ColumnPredicate<Long> delegateToLong(CyodaColumnHandle columnIn, ColumnPredicate.ComparisonOp op, long value) {
        // This is to circumvent the check on data type when delegating to long.
        // We won't use the predicate created, so this isn't an issue.
        CyodaColumnHandle longColumn = new CyodaColumnHandle(
                columnIn.getConnectorId(),
                columnIn.getColumnName(),
                columnIn.getColumnType(),
                DataType.LONG,
                columnIn.getOrdinalPosition(),
                columnIn.getRequestHandlerKey(),
                columnIn.getIsNullable()
        );
        long minValue = minValueOfIntType(columnIn.getDataType());
        long maxValue = maxValueOfIntType(columnIn.getDataType());

        Preconditions.checkArgument(value <= maxValue && value >= minValue,
                "integer value out of range for %s column: %s",
                columnIn.getDataType(), value);

        if (op == ColumnPredicate.ComparisonOp.LESS_EQUAL) {
            if (value == maxValue) {
                // If the value can't be incremented because it is at the top end of the
                // range, then substitute the predicate with an IS NOT NULL predicate.
                // This has the same effect as an inclusive upper bound on the maximum
                // value. If the column is not nullable then the IS NOT NULL predicate
                // is ignored.
                return newIsNotNullPredicate(longColumn);
            }
            value += 1;
            op = ColumnPredicate.ComparisonOp.LESS;
        } else if (op == ColumnPredicate.ComparisonOp.GREATER) {
            if (value == maxValue) {
                return none(longColumn);
            }
            value += 1;
            op = ColumnPredicate.ComparisonOp.GREATER_EQUAL;
        }


        switch (op) {
            case GREATER_EQUAL:
                if (value == minValue) {
                    return newIsNotNullPredicate(longColumn);
                } else if (value == maxValue) {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, longColumn, DataTypeValue.of(value), null);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, longColumn, DataTypeValue.of(value), null);
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, longColumn, DataTypeValue.of(value), null);
            case LESS:
                if (value == minValue) {
                    return none(longColumn);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, longColumn, null, DataTypeValue.of(value));
            default:
                throw unknownComparisonException();
        }
    }


    private static final BigInteger B = BigInteger.ONE.shiftLeft(64); // 2^64
    private static final BigInteger L = BigInteger.valueOf(Long.MAX_VALUE);

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


    static ColumnPredicate<Character> newComparisonPredicate(CyodaColumnHandle column,
                                                             ColumnPredicate.ComparisonOp op,
                                                             char value) {
        CyodaColumnHandle stringHandle = new CyodaColumnHandle(
                column.getConnectorId(),
                column.getColumnName(),
                column.getColumnType(),
                DataType.STRING,
                column.getOrdinalPosition(),
                column.getRequestHandlerKey()
        );
        ColumnPredicate<String> stringColumnPredicate = newComparisonPredicate(stringHandle, op, String.valueOf(value));

        return stringColumnPredicate.cloneTo(column, item->{
            String s = item.asString();
            char[] chars = s.toCharArray();
            if ( chars.length != 1 ) throw new IllegalStateException("Corrupted converted predicate from String to Character "+ s);
            return chars[0];
        });
    }
    static ColumnPredicate<Byte> newComparisonPredicate(CyodaColumnHandle column,
                                                        ColumnPredicate.ComparisonOp op,
                                                        byte value) {
        return delegateToLong(column, op, value).cloneTo(column, item->item.parseToLong().byteValue());
    }
    static ColumnPredicate<Short> newComparisonPredicate(CyodaColumnHandle column,
                                                         ColumnPredicate.ComparisonOp op,
                                                         short value) {
        return delegateToLong(column, op, value).cloneTo(column, item -> item.asLong().shortValue());
    }

    static ColumnPredicate<Integer> newComparisonPredicate(CyodaColumnHandle column,
                                                           ColumnPredicate.ComparisonOp op,
                                                           int value) {
        return delegateToLong(column, op, value).cloneTo(column, item->item.asLong().intValue());
    }

    static ColumnPredicate<Long> newComparisonPredicate(CyodaColumnHandle column,
                                                        ColumnPredicate.ComparisonOp op,
                                                        long value) {
        return delegateToLong(column, op, value);
    }

    /**
     * Creates a new comparison predicate on a BIGINT column. We delegate the logic to BigDecimal.
     *
     * @param column the column schema
     * @param op     the comparison operation
     * @param value  the value to compare against
     */
    static ColumnPredicate<BigInteger> newComparisonPredicate(CyodaColumnHandle column,
                                                              ColumnPredicate.ComparisonOp op,
                                                              BigInteger value) {
        checkColumn(column, DataType.BIG_INTEGER);
        return delegateToBigDecimal(column, op, value)
                .cloneTo(column,item->item.asBigDecimal().toBigIntegerExact());
    }

    static ColumnPredicate<ChronoLocalDate> newComparisonPredicate(CyodaColumnHandle column,
                                                                   ColumnPredicate.ComparisonOp op,
                                                                   LocalDate value) {

        DataType dataType = DataType.LOCAL_DATE;
        checkColumn(column, dataType);
        long par = PrestoValueConverterProvider.getPrestoValueConverter(LocalDateDataType.INSTANCE).toLong(value);
        return delegateToLong(column, op, par)
                .cloneTo(column,item-> LocalDate.ofEpochDay(item.parseToLong()));
    }

    static ColumnPredicate<LocalDateTime> newComparisonPredicate(CyodaColumnHandle column,
                                                                 ColumnPredicate.ComparisonOp op,
                                                                 LocalDateTime value) {
        DataType dataType = DataType.LOCAL_DATE_TIME;
        checkColumn(column, dataType);
        return delegateToLong(column, op, value.toInstant(ZoneOffset.UTC).toEpochMilli())
                .cloneTo(column,item->LocalDateTime.ofInstant(Instant.ofEpochMilli(item.parseToLong()), ZoneId.of("UTC")));
    }
    static ColumnPredicate<ZonedDateTime> newComparisonPredicate(CyodaColumnHandle column,
                                                                 ColumnPredicate.ComparisonOp op,
                                                                 ZonedDateTime value) {
        DataType dataType = DataType.ZONED_DATE_TIME;
        checkColumn(column, dataType);
        return delegateToLong(column, op, value.toInstant().toEpochMilli())
                .cloneTo(column,item->ZonedDateTime.ofInstant(Instant.ofEpochMilli(item.parseToLong()),value.getZone()));
    }
    static ColumnPredicate<Year> newComparisonPredicate(CyodaColumnHandle column,
                                                        ColumnPredicate.ComparisonOp op,
                                                        Year value) {
        DataType dataType = DataType.YEAR;
        checkColumn(column, dataType);
        return delegateToLong(column, op, value.getValue())
                .cloneTo(column,item->Year.of(item.parseToLong().intValue()));
    }
    static ColumnPredicate<YearMonth> newComparisonPredicate(CyodaColumnHandle column,
                                                             ColumnPredicate.ComparisonOp op,
                                                             YearMonth value) {
        DataType dataType = DataType.YEAR_MONTH;
        checkColumn(column, dataType);
        return delegateToLong(column, op, value.atEndOfMonth().toEpochDay())
                .cloneTo(column,item->YearMonth.from(LocalDate.ofEpochDay(item.parseToLong())));
    }
    static ColumnPredicate<LocalTime> newComparisonPredicate(CyodaColumnHandle column,
                                                             ColumnPredicate.ComparisonOp op,
                                                             LocalTime value) {
        DataType dataType = DataType.LOCAL_TIME;
        checkColumn(column, dataType);
        return delegateToLong(column, op, value.toNanoOfDay())
                .cloneTo(column,item->LocalTime.ofNanoOfDay(item.parseToLong()));
    }


    /**
     * Creates a new comparison predicate on a Decimal column.
     *
     * @param column the column schema
     * @param op     the comparison operation
     * @param value  the value to compare against
     */
    static ColumnPredicate<BigDecimal> newComparisonPredicate(CyodaColumnHandle column,
                                                              ColumnPredicate.ComparisonOp op,
                                                              BigDecimal value) {
        checkColumn(column, DataType.BIG_DECIMAL);

        BigDecimal minValue = DecimalUtil.minValue(value.precision(), value.scale());
        BigDecimal maxValue = DecimalUtil.maxValue(value.precision(), value.scale());
        Preconditions.checkArgument(value.compareTo(maxValue) <= 0 && value.compareTo(minValue) >= 0,
                "Decimal value out of range for %s column: %s",
                column.getDataType(), value);
        BigDecimal smallestValue = DecimalUtil.smallestValue(value.scale());

        if (op == ColumnPredicate.ComparisonOp.LESS_EQUAL) {
            if (value.equals(maxValue)) {
                // If the value can't be incremented because it is at the top end of the
                // range, then substitute the predicate with an IS NOT NULL predicate.
                // This has the same effect as an inclusive upper bound on the maximum
                // value. If the column is not nullable then the IS NOT NULL predicate
                // is ignored.
                return newIsNotNullPredicate(column);
            }
            value = value.add(smallestValue);
            op = ColumnPredicate.ComparisonOp.LESS;
        } else if (op == ColumnPredicate.ComparisonOp.GREATER) {
            if (value.equals(maxValue)) {
                return none(column);
            }
            value = value.add(smallestValue);
            op = ColumnPredicate.ComparisonOp.GREATER_EQUAL;
        }

        DataTypeValue<BigDecimal> wrapped = DataTypeValue.of(value);

        switch (op) {
            case GREATER_EQUAL:
                if (value.equals(minValue)) {
                    return newIsNotNullPredicate(column);
                } else if (value.equals(maxValue)) {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, wrapped, null);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, wrapped, null);
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, wrapped, null);
            case LESS:
                if (value.equals(minValue)) {
                    return none(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, null, wrapped);
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
    static ColumnPredicate<Date> newComparisonPredicate(CyodaColumnHandle column,
                                                        ColumnPredicate.ComparisonOp op,
                                                        Date value) {
        checkColumn(column, DataType.DATE);
        long days = value.toInstant().toEpochMilli();
        return delegateToLong(column, op, days)
                .cloneTo(column,item->Date.from(Instant.ofEpochMilli(item.parseToLong())));
    }

    /**
     * Creates a new comparison predicate on a float column.
     *
     * @param column the column schema
     * @param op     the comparison operation
     * @param value  the value to compare against
     */
    static ColumnPredicate<Float> newComparisonPredicate(CyodaColumnHandle column,
                                                         ColumnPredicate.ComparisonOp op,
                                                         float value) {
        checkColumn(column, DataType.FLOAT);
        if (op == ColumnPredicate.ComparisonOp.LESS_EQUAL) {
            if (value == Float.POSITIVE_INFINITY) {
                return newIsNotNullPredicate(column);
            }
            value = Math.nextAfter(value, Float.POSITIVE_INFINITY);
            op = ColumnPredicate.ComparisonOp.LESS;
        } else if (op == ColumnPredicate.ComparisonOp.GREATER) {
            if (value == Float.POSITIVE_INFINITY) {
                return none(column);
            }
            value = Math.nextAfter(value, Float.POSITIVE_INFINITY);
            op = ColumnPredicate.ComparisonOp.GREATER_EQUAL;
        }

        DataTypeValue<Float> wrapped = DataTypeValue.of(value);

        switch (op) {
            case GREATER_EQUAL:
                if (value == Float.NEGATIVE_INFINITY) {
                    return newIsNotNullPredicate(column);
                } else if (value == Float.POSITIVE_INFINITY) {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, wrapped, null);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, wrapped, null);
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, wrapped, null);
            case LESS:
                if (value == Float.NEGATIVE_INFINITY) {
                    return none(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, null, wrapped);
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
    static ColumnPredicate<Double> newComparisonPredicate(CyodaColumnHandle column,
                                                          ColumnPredicate.ComparisonOp op,
                                                          double value) {
        checkColumn(column, DataType.DOUBLE);
        if (op == ColumnPredicate.ComparisonOp.LESS_EQUAL) {
            if (value == Double.POSITIVE_INFINITY) {
                return newIsNotNullPredicate(column);
            }
            value = Math.nextAfter(value, Double.POSITIVE_INFINITY);
            op = ColumnPredicate.ComparisonOp.LESS;
        } else if (op == ColumnPredicate.ComparisonOp.GREATER) {
            if (value == Double.POSITIVE_INFINITY) {
                return none(column);
            }
            value = Math.nextAfter(value, Double.POSITIVE_INFINITY);
            op = ColumnPredicate.ComparisonOp.GREATER_EQUAL;
        }

        DataTypeValue<Double> wrapped = DataTypeValue.of(value);

        switch (op) {
            case GREATER_EQUAL:
                if (value == Double.NEGATIVE_INFINITY) {
                    return newIsNotNullPredicate(column);
                } else if (value == Double.POSITIVE_INFINITY) {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, wrapped, null);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, wrapped, null);
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, wrapped, null);
            case LESS:
                if (value == Double.NEGATIVE_INFINITY) {
                    return none(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, null, wrapped);
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
    static ColumnPredicate<String> newComparisonPredicate(CyodaColumnHandle column,
                                                          ColumnPredicate.ComparisonOp op,
                                                          String value) {
        checkColumn(column, DataType.STRING);

        byte[] bytes = value.getBytes(UTF_8);

        if (op == ColumnPredicate.ComparisonOp.LESS_EQUAL) {
            bytes = Arrays.copyOf(bytes, bytes.length + 1);
            op = ColumnPredicate.ComparisonOp.LESS;
        } else if (op == ColumnPredicate.ComparisonOp.GREATER) {
            bytes = Arrays.copyOf(bytes, bytes.length + 1);
            op = ColumnPredicate.ComparisonOp.GREATER_EQUAL;
        }

        String string = new String(bytes, UTF_8);
        DataTypeValue<String> wrapped = DataTypeValue.of(string);

        switch (op) {
            case GREATER_EQUAL:
                if (bytes.length == 0) {
                    return newIsNotNullPredicate(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, wrapped, null);
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, wrapped, null);
            case LESS:
                if (bytes.length == 0) {
                    return none(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, null, wrapped);
            default:
                throw unknownComparisonException();
        }
    }

    public static ColumnPredicate<ByteBuffer> newComparisonPredicate(CyodaColumnHandle column,
                                                                     ColumnPredicate.ComparisonOp op,
                                                                     byte[] value) {
        return newComparisonPredicate(column,op,ByteBuffer.wrap(value));
    }

    /**
     * Creates a new comparison predicate on a binary column.
     * @param column the column schema
     * @param op the comparison operation
     * @param valueIn the value to compare against
     */
    public static ColumnPredicate<ByteBuffer> newComparisonPredicate(CyodaColumnHandle column,
                                                                     ColumnPredicate.ComparisonOp op,
                                                                     ByteBuffer valueIn) {
        checkColumn(column,  DataType.BYTE_BUFFER, DataType.BYTE_ARRAY);

        byte[] value = new byte[valueIn.remaining()];
        try {
            valueIn.get(value);
        } finally {
            valueIn.rewind();
        }
        if (op == ColumnPredicate.ComparisonOp.LESS_EQUAL) {
            value = Arrays.copyOf(value, value.length + 1);
            op = ColumnPredicate.ComparisonOp.LESS;
        } else if (op == ColumnPredicate.ComparisonOp.GREATER) {
            value = Arrays.copyOf(value, value.length + 1);
            op = ColumnPredicate.ComparisonOp.GREATER_EQUAL;
        }

        DataTypeValue<ByteBuffer> wrapped = DataTypeValue.of(ByteBuffer.wrap(value));

        switch (op) {
            case GREATER_EQUAL:
                if (value.length == 0) {
                    return newIsNotNullPredicate(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, wrapped, null);
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, wrapped, null);
            case LESS:
                if (value.length == 0) {
                    return none(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, null, wrapped);
            default:
                throw unknownComparisonException();
        }
    }

    public static <S extends Comparable<? super S>> ColumnPredicate<S> newComparisonPredicateFromNative(
            CyodaColumnHandle columnHandle,
            ColumnPredicate.ComparisonOp op,
            Object nativeValue,
            Class<S> clazz) {
        ColumnPredicate<?> columnPredicate = newComparisonPredicateFromNative(columnHandle, op, nativeValue);
        //noinspection unchecked
        return (ColumnPredicate<S>) columnPredicate;
    }

    @SuppressWarnings("java:S1452") // We need a wildcard here.
    public static ColumnPredicate<?> newComparisonPredicateFromNative(
            CyodaColumnHandle columnHandle,
            ColumnPredicate.ComparisonOp op,
            Object nativeValue) {
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
            case UUID_TYPE:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, UUID.class);
            case BIG_DECIMAL:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, BigDecimal.class);
            case BIG_INTEGER:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, BigInteger.class);
            case LOCAL_DATE:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, LocalDate.class);
            case LOCAL_DATE_TIME:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, LocalDateTime.class);
            case CHARACTER:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, Character.class);
            case DATE:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, Date.class);
            case ZONED_DATE_TIME:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, ZonedDateTime.class );
            case YEAR:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, Year.class);
            case YEAR_MONTH:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, YearMonth.class);
            case LOCAL_TIME:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, LocalTime.class);
            case BYTE_BUFFER:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, ByteBuffer.class);
            case BYTE_ARRAY:
                return prestoNativeToPredicate(columnHandle, op, nativeValue, ByteBuffer.class);
            default:
                throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "DataType  " + columnHandle.getDataType() + " not yet supported");
        }
    }

    private static <T extends Comparable<? super T>> ColumnPredicate<T> prestoNativeToPredicate(
            CyodaColumnHandle columnHandle,
            ColumnPredicate.ComparisonOp op,
            Object nativeValue,
            Class<T> javaType) {
        DataTypeValue<T> thing = DataTypeValue.ofPrestoNativeValue(columnHandle.getColumnType(), nativeValue, javaType);
        return newComparisonPredicate(columnHandle, op, thing);
    }


    @SuppressWarnings("unchecked")
    private static <T extends Comparable<? super T>> ColumnPredicate<T> newComparisonPredicate(
            CyodaColumnHandle columnHandle,
            ColumnPredicate.ComparisonOp op,
            DataTypeValue<T> value) {
        switch (value.supportedDataType.getDataType()) {
            case LONG:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asLong());
            case INTEGER:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asInt());
            case SHORT:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asShort());
            case BYTE:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asByte());
            case STRING:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asString());
            case DOUBLE:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asDouble());
            case FLOAT:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asFloat());
            case BOOLEAN:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asBoolean());
            case UUID_TYPE:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asUUID());
            case BIG_DECIMAL:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asBigDecimal());
            case BIG_INTEGER:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asBigInteger());
            case LOCAL_DATE:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asLocalDate());
            case LOCAL_DATE_TIME:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asLocalDateTime());
            case CHARACTER:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asChar());
            case DATE:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asDate());
            case ZONED_DATE_TIME:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asZonedDateTime());
            case YEAR:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asYear());
            case YEAR_MONTH:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asYearMonth());
            case LOCAL_TIME:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asLocalTime());
            case BYTE_BUFFER:
                return (ColumnPredicate<T>) newComparisonPredicate(columnHandle, op, value.asByteBuffer());
            default:
                throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "Unexpected java value for column "
                        + columnHandle.getColumnName() + ": " + value.value + "(" + value.supportedDataType + ")");

        }
    }

    public static <S extends Comparable<? super S>> ColumnPredicate<S> newEqualsPredicate(CyodaColumnHandle columnHandle, Object nativeValue, Class<S> clazz) {
        return newComparisonPredicateFromNative(columnHandle, EQUAL, nativeValue,clazz);
    }

    @SuppressWarnings("java:S1452")
    static ColumnPredicate<?> newInListPredicateFromDiscrete(CyodaColumnHandle columnHandle, DiscreteValues discreteValues) {
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

    static <T extends Comparable<T>> ColumnPredicate<T> newInListPredicate(
            final CyodaColumnHandle columnHandle,
            final DiscreteValues discreteValues,
            final Class<T> javaType) {
        Type type = columnHandle.getColumnType();
        SortedSet<DataTypeValue<T>> javaValues = discreteValues.getValues().stream()
                .map(nativeValue -> DataTypeValue.ofPrestoNativeValue(type, nativeValue, javaType))
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
    static <T extends Comparable<T>> ColumnPredicate<T> newInListPredicate(
            final CyodaColumnHandle column,
            final SortedSet<DataTypeValue<T>> values
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
    public static <T extends Comparable<? super T>> ColumnPredicate<T> newIsNotNullPredicate(CyodaColumnHandle column) {
        return new ColumnPredicate<>(ColumnPredicate.PredicateType.IS_NOT_NULL, column, null, null);
    }

    public static ColumnPredicate<Any> newIsNotNullPredicateAny(CyodaColumnHandle column) {
        return newIsNotNullPredicate(column);
    }

    /**
     * Creates a new {@code IS NULL} predicate.
     *
     * @param column the column that the predicate applies to
     * @return an {@code IS NULL} predicate
     */
    public static <T extends Comparable<T>> ColumnPredicate<T> newIsNullPredicate(CyodaColumnHandle column) {
        if (!column.getIsNullable()) {
            return none(column);
        }
        return new ColumnPredicate<>(ColumnPredicate.PredicateType.IS_NULL, column, null, null);
    }

    public static ColumnPredicate<Any> newIsNullPredicateAny(CyodaColumnHandle column) {
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
    static <T extends Comparable<? super T>> ColumnPredicate<T> none(CyodaColumnHandle column) {
        return new ColumnPredicate<>(ColumnPredicate.PredicateType.NONE, column, null, null);
    }

    /**
     * Factory function for a predicate that filters nothing on the given column
     *
     * @param column the column to which the predicate applies
     * @return a ALL predicate
     */
    static ColumnPredicate<Any> all(CyodaColumnHandle column) {
        return new ColumnPredicate<>(ColumnPredicate.PredicateType.ALL, column, null, null);
    }

    /**
     * Builds an IN list predicate from a collection of raw values. The collection
     * must be sorted and deduplicated.
     *
     * @param column the column
     * @param values the IN list values
     * @return an IN list predicate
     */
    public static <T extends Comparable<? super T>> ColumnPredicate<T> buildInList(
            CyodaColumnHandle column, SortedSet<DataTypeValue<T>> values
    ) {
        // IN (true, false) predicates can be simplified to IS NOT NULL.
        if (column.getDataType() == DataType.BOOLEAN && values.size() > 1) {
            return newIsNotNullPredicate(column);
        }

        switch (values.size()) {
            case 0:
                return none(column);
            case 1:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, values.iterator().next(), null);
            default:
                return new ColumnPredicate<>(column, values);
        }
    }

    /**
     * Returns the maximum value for the integer type.
     *
     * @param dataType an integer type
     * @return the maximum value
     */
    static long maxValueOfIntType(DataType dataType) {
        switch (dataType) {
            case BYTE:
                return Byte.MAX_VALUE;
            case SHORT:
                return Short.MAX_VALUE;
            case INTEGER:
                return Integer.MAX_VALUE;
            case YEAR:
                return Year.MAX_VALUE;
            case YEAR_MONTH:
                // TODO: Define a constanct
                return YearMonth.from(LocalDate.MAX.atStartOfDay()).atEndOfMonth().toEpochDay();
            case LOCAL_DATE:
                return LocalDate.MAX.toEpochDay();
            case ZONED_DATE_TIME:
            case LOCAL_DATE_TIME:
            case DATE: // Unsure
            case LONG:
                return Long.MAX_VALUE;
            default:
                throw new IllegalArgumentException("type must be an integer type");
        }
    }

    /**
     * Returns the minimum value for the integer-based type.
     *
     * @param dataType an integer type
     * @return the minimum value
     */
    static long minValueOfIntType(DataType dataType) {
        switch (dataType) {
            case BYTE:
                return Byte.MIN_VALUE;
            case SHORT:
                return Short.MIN_VALUE;
            case INTEGER:
                return Integer.MIN_VALUE;
            case YEAR:
                return Year.MIN_VALUE;
            case YEAR_MONTH:
                // TODO: Define a constanct
                return YearMonth.from(LocalDate.MIN.atStartOfDay()).atEndOfMonth().toEpochDay();
            case LOCAL_DATE:
                return LocalDate.MIN.toEpochDay();
            case ZONED_DATE_TIME:
            case LOCAL_DATE_TIME:
            case DATE: // Unsure
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

    public static <T extends Comparable<T>> ColumnPredicate<T> nothing() {
        return new ColumnPredicate<>(null, null, null, null);
    }

}
