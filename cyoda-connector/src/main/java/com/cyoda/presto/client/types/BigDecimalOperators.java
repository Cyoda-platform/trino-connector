///*
// * Copyright (C) 2022 Cyoda Ltd.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// */
//
//package com.cyoda.presto.client.types;
//
//import com.cyoda.presto.client.logic.converters.impl.BigDecimalPrestoValueConverter;
//import io.trino.spi.block.Block;
//import io.trino.spi.type.AbstractLongType;
//import io.trino.spi.type.StandardTypes;
//import io.trino.spi.TrinoException;
//import io.trino.spi.function.BlockIndex;
//import io.trino.spi.function.BlockPosition;
//import io.trino.spi.function.IsNull;
//import io.trino.spi.function.LiteralParameters;
//import io.trino.spi.function.ScalarOperator;
//import io.trino.spi.function.SqlNullable;
//import io.trino.spi.function.SqlType;
//import com.google.common.primitives.Shorts;
//import com.google.common.primitives.SignedBytes;
//import io.airlift.slice.Slice;
//import io.airlift.slice.XxHash64;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//
//import static com.cyoda.presto.client.types.BigDecimalType.BIG_DECIMAL_TYPE;
//import static io.trino.spi.function.OperatorType.*;
//import static io.trino.spi.StandardErrorCode.*;
//import static com.google.common.base.Preconditions.checkState;
//import static io.airlift.slice.Slices.utf8Slice;
//import static java.lang.Float.floatToRawIntBits;
//import static java.lang.Math.toIntExact;
//import static java.lang.String.format;
//import static java.math.RoundingMode.HALF_UP;
//
//public class BigDecimalOperators {
//    private static final BigDecimal MIN_LONG_AS_BIG_DECIMAL = BigDecimal.valueOf(Long.MIN_VALUE);
//    private static final BigDecimal MAX_LONG_PLUS_ONE_AS_BIG_DECIMAL = BigDecimal.valueOf(Long.MAX_VALUE);
//    private static final BigDecimal MIN_INTEGER_AS_BIG_DECIMAL = BigDecimal.valueOf(Integer.MIN_VALUE);
//    private static final BigDecimal MAX_INTEGER_PLUS_ONE_AS_BIG_DECIMAL = BigDecimal.valueOf(Integer.MAX_VALUE);
//    private static final BigDecimal MIN_SHORT_AS_BIG_DECIMAL = BigDecimal.valueOf(Short.MIN_VALUE);
//    private static final BigDecimal MAX_SHORT_PLUS_ONE_AS_BIG_DECIMAL = BigDecimal.valueOf(Short.MAX_VALUE);
//    private static final BigDecimal MIN_BYTE_AS_BIG_DECIMAL = BigDecimal.valueOf(Byte.MIN_VALUE);
//    private static final BigDecimal MAX_BYTE_PLUS_ONE_AS_BIG_DECIMAL = BigDecimal.valueOf(Byte.MAX_VALUE);
//
//    private static final BigDecimalPrestoValueConverter converter = new BigDecimalPrestoValueConverter();
//    private BigDecimalOperators()
//    {
//    }
//
//    @ScalarOperator(ADD)
//    @SqlType(BigDecimalType.BIG_DECIMAL)
//    public static BigDecimal add(@SqlType(BigDecimalType.BIG_DECIMAL) Slice left, @SqlType(BigDecimalType.BIG_DECIMAL) Slice right)
//    {
//        return toInt128(left).add(toInt128(right));
//    }
//
//    private static BigDecimal toInt128(Slice left) {
//        return converter.fromSlice(left);
//    }
//
//    @ScalarOperator(SUBTRACT)
//    @SqlType(BigDecimalType.BIG_DECIMAL)
//    public static BigDecimal subtract(@SqlType(BigDecimalType.BIG_DECIMAL) Slice left, @SqlType(BigDecimalType.BIG_DECIMAL) Slice right)
//    {
//        return toInt128(left).subtract(toInt128(right));
//    }
//
//    @ScalarOperator(MULTIPLY)
//    @SqlType(BigDecimalType.BIG_DECIMAL)
//    public static BigDecimal multiply(@SqlType(BigDecimalType.BIG_DECIMAL) Slice left, @SqlType(BigDecimalType.BIG_DECIMAL) Slice right)
//    {
//        return toInt128(left).multiply(toInt128(right));
//    }
//
//    @ScalarOperator(DIVIDE)
//    @SqlType(BigDecimalType.BIG_DECIMAL)
//    public static BigDecimal divide(@SqlType(BigDecimalType.BIG_DECIMAL) Slice left, @SqlType(BigDecimalType.BIG_DECIMAL) Slice right)
//    {
//        try {
//            return toInt128(left).divide(toInt128(right));
//        }
//        catch (ArithmeticException e) {
//            throw new TrinoException(DIVISION_BY_ZERO, e);
//        }
//    }
//
//    @ScalarOperator(MODULUS)
//    @SqlType(BigDecimalType.BIG_DECIMAL)
//    public static BigDecimal modulus(@SqlType(BigDecimalType.BIG_DECIMAL) Slice left, @SqlType(BigDecimalType.BIG_DECIMAL) Slice right)
//    {
//        try {
//            return toInt128(left).remainder(toInt128(right));
//        }
//        catch (ArithmeticException e) {
//            throw new TrinoException(DIVISION_BY_ZERO, e);
//        }
//    }
//
//    @ScalarOperator(NEGATION)
//    @SqlType(BigDecimalType.BIG_DECIMAL)
//    public static BigDecimal negate(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value)
//    {
//        return toInt128(value).negate();
//    }
//
//    @SuppressWarnings("java:S1221")
//    @ScalarOperator(EQUAL)
//    @SqlType(StandardTypes.BOOLEAN)
//    @SqlNullable
//    public static Boolean equal(@SqlType(BigDecimalType.BIG_DECIMAL) Slice left, @SqlType(BigDecimalType.BIG_DECIMAL) Slice right)
//    {
//        return left.equals(right);
//    }
//
//    @ScalarOperator(NOT_EQUAL)
//    @SuppressWarnings("FloatingPointEquality")
//    @SqlType(StandardTypes.BOOLEAN)
//    @SqlNullable
//    public static Boolean notEqual(@SqlType(BigDecimalType.BIG_DECIMAL) Slice left, @SqlType(BigDecimalType.BIG_DECIMAL) Slice right)
//    {
//        return !left.equals(right);
//    }
//
//    @ScalarOperator(LESS_THAN)
//    @SqlType(StandardTypes.BOOLEAN)
//    public static boolean lessThan(@SqlType(BigDecimalType.BIG_DECIMAL) Slice left, @SqlType(BigDecimalType.BIG_DECIMAL) Slice right)
//    {
//        return left.compareTo(right) < 0;
//    }
//
//    @ScalarOperator(LESS_THAN_OR_EQUAL)
//    @SqlType(StandardTypes.BOOLEAN)
//    public static boolean lessThanOrEqual(@SqlType(BigDecimalType.BIG_DECIMAL) Slice left, @SqlType(BigDecimalType.BIG_DECIMAL) Slice right)
//    {
//        return left.compareTo(right) <= 0;
//    }
//
//    @ScalarOperator(GREATER_THAN)
//    @SqlType(StandardTypes.BOOLEAN)
//    public static boolean greaterThan(@SqlType(BigDecimalType.BIG_DECIMAL) Slice left, @SqlType(BigDecimalType.BIG_DECIMAL) Slice right)
//    {
//        return left.compareTo(right) > 0;
//    }
//
//    @ScalarOperator(GREATER_THAN_OR_EQUAL)
//    @SqlType(StandardTypes.BOOLEAN)
//    public static boolean greaterThanOrEqual(@SqlType(BigDecimalType.BIG_DECIMAL) Slice left, @SqlType(BigDecimalType.BIG_DECIMAL) Slice right)
//    {
//        return left.compareTo(right) >= 0;
//    }
//
//    @ScalarOperator(BETWEEN)
//    @SqlType(StandardTypes.BOOLEAN)
//    public static boolean between(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value, @SqlType(BigDecimalType.BIG_DECIMAL) Slice min, @SqlType(BigDecimalType.BIG_DECIMAL) Slice max)
//    {
//        return min.compareTo(value) <= 0 && value.compareTo(max) <= 0;
//    }
//
//    @ScalarOperator(CAST)
//    @SqlType(StandardTypes.BOOLEAN)
//    public static boolean castToBoolean(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value)
//    {
//        return !value.equals(BigDecimal.ZERO);
//    }
//
//    @ScalarOperator(CAST)
//    @SqlType(StandardTypes.INTEGER)
//    public static long castToInteger(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value)
//    {
//        try {
//            return toIntExact(toInt128(value).toBigIntegerExact().longValue());
//        }
//        catch (ArithmeticException e) {
//            throw new TrinoException(NUMERIC_VALUE_OUT_OF_RANGE, "Out of range for integer: " + value, e);
//        }
//    }
//
//
//    @ScalarOperator(CAST)
//    @SqlType(StandardTypes.SMALLINT)
//    public static long castToSmallint(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value)
//    {
//        try {
//
//            return Shorts.checkedCast(toInt128(value).toBigIntegerExact().longValue());
//        }
//        catch (IllegalArgumentException e) {
//            throw new TrinoException(NUMERIC_VALUE_OUT_OF_RANGE, "Out of range for smallint: " + value, e);
//        }
//    }
//
//    @ScalarOperator(CAST)
//    @SqlType(StandardTypes.TINYINT)
//    public static long castToTinyint(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value)
//    {
//        try {
//            return SignedBytes.checkedCast(toInt128(value).toBigIntegerExact().longValue());
//        }
//        catch (IllegalArgumentException e) {
//            throw new TrinoException(NUMERIC_VALUE_OUT_OF_RANGE, "Out of range for tinyint: " + value, e);
//        }
//    }
//
//    @ScalarOperator(CAST)
//    @SqlType(StandardTypes.BIGINT)
//    public static long castToLong(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value)
//    {
//        try {
//            return toInt128(value).setScale(0,HALF_UP).longValue();
//        }
//        catch (ArithmeticException e) {
//            throw new TrinoException(INVALID_CAST_ARGUMENT, format("Unable to cast %s to bigint", value), e);
//        }
//    }
//
//    @ScalarOperator(CAST)
//    @SqlType(StandardTypes.REAL)
//    public static long castToReal(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value)
//    {
//        return floatToRawIntBits(((Double) toInt128(value).doubleValue()).floatValue());
//    }
//
//    @ScalarOperator(CAST)
//    @SqlType(StandardTypes.DOUBLE)
//    public static double castToDouble(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value)
//    {
//        return (toInt128(value).doubleValue());
//    }
//
//    @ScalarOperator(CAST)
//    @LiteralParameters("x")
//    @SqlType("varchar(x)")
//    public static Slice castToVarchar(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value)
//    {
//        return utf8Slice(String.valueOf(value));
//    }
//
//    @ScalarOperator(HASH_CODE)
//    @SqlType(StandardTypes.BIGINT)
//    public static long hashCode(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value)
//    {
//        return AbstractLongType.hash(toInt128(value).longValue());
//    }
//
//    @ScalarOperator(INDETERMINATE)
//    @SqlType(StandardTypes.BOOLEAN)
//    public static boolean indeterminate(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value, @IsNull boolean isNull)
//    {
//        return isNull;
//    }
//
//    @ScalarOperator(SATURATED_FLOOR_CAST)
//    @SqlType(StandardTypes.REAL)
//    public static strictfp long saturatedFloorCastToFloat(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value)
//    {
//        float result;
//        BigDecimal minFloat = BigDecimal.valueOf(Float.MIN_VALUE);
//        BigDecimal maxFloat = BigDecimal.valueOf(Float.MAX_VALUE);
//        BigDecimal bd = toInt128(value);
//        if (toInt128(value).compareTo(minFloat) <= 0) {
//            result = Float.MIN_VALUE;
//        }
//        else if (bd.compareTo(maxFloat) >= 0) {
//            result = Float.MAX_VALUE;
//        }
//        else {
//            result = bd.floatValue();
//            if (BigDecimal.valueOf(result).compareTo(bd) > 0) {
//                result = Math.nextDown(result);
//            }
//            checkState(BigDecimal.valueOf(result).compareTo(bd) <= 0);
//        }
//        return floatToRawIntBits(result);
//    }
//
//    @ScalarOperator(SATURATED_FLOOR_CAST)
//    @SqlType(StandardTypes.INTEGER)
//    public static long saturatedFloorCastToInteger(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value)
//    {
//        return saturatedFloorCastToLong(toInt128(value), Integer.MIN_VALUE, MIN_INTEGER_AS_BIG_DECIMAL, Integer.MAX_VALUE, MAX_INTEGER_PLUS_ONE_AS_BIG_DECIMAL);
//    }
//
//    @ScalarOperator(SATURATED_FLOOR_CAST)
//    @SqlType(StandardTypes.SMALLINT)
//    public static long saturatedFloorCastToSmallint(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value)
//    {
//        return saturatedFloorCastToLong(toInt128(value), Short.MIN_VALUE, MIN_SHORT_AS_BIG_DECIMAL, Short.MAX_VALUE, MAX_SHORT_PLUS_ONE_AS_BIG_DECIMAL);
//    }
//
//    @ScalarOperator(SATURATED_FLOOR_CAST)
//    @SqlType(StandardTypes.TINYINT)
//    public static long saturatedFloorCastToTinyint(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value)
//    {
//        return saturatedFloorCastToLong(toInt128(value), Byte.MIN_VALUE, MIN_BYTE_AS_BIG_DECIMAL, Byte.MAX_VALUE, MAX_BYTE_PLUS_ONE_AS_BIG_DECIMAL);
//    }
//
//    private static long saturatedFloorCastToLong(BigDecimal value, long minValue, BigDecimal minValueAsBigDecimal, long maxValue, BigDecimal maxValuePlusOneAsBigDecimal)
//    {
//        if (value.compareTo(minValueAsBigDecimal) <= 0) {
//            return minValue;
//        }
//        if (value.add(BigDecimal.ONE).compareTo(maxValuePlusOneAsBigDecimal) >= 0) {
//            return maxValue;
//        }
//        return value.setScale(0, RoundingMode.FLOOR).longValue();
//    }
//
//    @ScalarOperator(XX_HASH_64)
//    @SqlType(StandardTypes.BIGINT)
//    public static long xxHash64(@SqlType(BigDecimalType.BIG_DECIMAL) Slice value)
//    {
//        return XxHash64.hash(toInt128(value).longValue());
//    }
//}
