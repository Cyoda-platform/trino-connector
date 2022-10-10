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

package com.cyoda.presto.client.types;

import com.cyoda.presto.client.logic.converters.impl.BigIntegerPrestoValueConverter;
import com.facebook.presto.common.block.Block;
import com.facebook.presto.common.type.AbstractLongType;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.function.BlockIndex;
import com.facebook.presto.spi.function.BlockPosition;
import com.facebook.presto.spi.function.IsNull;
import com.facebook.presto.spi.function.LiteralParameters;
import com.facebook.presto.spi.function.ScalarOperator;
import com.facebook.presto.spi.function.SqlNullable;
import com.facebook.presto.spi.function.SqlType;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import io.airlift.slice.Slice;
import io.airlift.slice.XxHash64;

import java.math.BigInteger;
import java.math.RoundingMode;

import static com.cyoda.presto.client.types.BigIntegerType.BIG_INTEGER_TYPE;
import static com.facebook.presto.common.function.OperatorType.ADD;
import static com.facebook.presto.common.function.OperatorType.BETWEEN;
import static com.facebook.presto.common.function.OperatorType.CAST;
import static com.facebook.presto.common.function.OperatorType.DIVIDE;
import static com.facebook.presto.common.function.OperatorType.EQUAL;
import static com.facebook.presto.common.function.OperatorType.GREATER_THAN;
import static com.facebook.presto.common.function.OperatorType.GREATER_THAN_OR_EQUAL;
import static com.facebook.presto.common.function.OperatorType.HASH_CODE;
import static com.facebook.presto.common.function.OperatorType.INDETERMINATE;
import static com.facebook.presto.common.function.OperatorType.IS_DISTINCT_FROM;
import static com.facebook.presto.common.function.OperatorType.LESS_THAN;
import static com.facebook.presto.common.function.OperatorType.LESS_THAN_OR_EQUAL;
import static com.facebook.presto.common.function.OperatorType.MODULUS;
import static com.facebook.presto.common.function.OperatorType.MULTIPLY;
import static com.facebook.presto.common.function.OperatorType.NEGATION;
import static com.facebook.presto.common.function.OperatorType.NOT_EQUAL;
import static com.facebook.presto.common.function.OperatorType.SATURATED_FLOOR_CAST;
import static com.facebook.presto.common.function.OperatorType.SUBTRACT;
import static com.facebook.presto.common.function.OperatorType.XX_HASH_64;
import static com.facebook.presto.spi.StandardErrorCode.DIVISION_BY_ZERO;
import static com.facebook.presto.spi.StandardErrorCode.INVALID_CAST_ARGUMENT;
import static com.facebook.presto.spi.StandardErrorCode.NUMERIC_VALUE_OUT_OF_RANGE;
import static com.google.common.base.Preconditions.checkState;
import static io.airlift.slice.Slices.utf8Slice;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Math.toIntExact;
import static java.lang.String.format;
import static java.math.RoundingMode.HALF_UP;

public class BigIntegerOperators {
    private static final BigInteger MIN_LONG_AS_BIG_INTEGER = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger MAX_LONG_PLUS_ONE_AS_BIG_INTEGER = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigInteger MIN_INTEGER_AS_BIG_INTEGER = BigInteger.valueOf(Integer.MIN_VALUE);
    private static final BigInteger MAX_INTEGER_PLUS_ONE_AS_BIG_INTEGER = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger MIN_SHORT_AS_BIG_INTEGER = BigInteger.valueOf(Short.MIN_VALUE);
    private static final BigInteger MAX_SHORT_PLUS_ONE_AS_BIG_INTEGER = BigInteger.valueOf(Short.MAX_VALUE);
    private static final BigInteger MIN_BYTE_AS_BIG_INTEGER = BigInteger.valueOf(Byte.MIN_VALUE);
    private static final BigInteger MAX_BYTE_PLUS_ONE_AS_BIG_INTEGER = BigInteger.valueOf(Byte.MAX_VALUE);

    private static final BigIntegerPrestoValueConverter converter = new BigIntegerPrestoValueConverter();
    private BigIntegerOperators()
    {
    }

    @ScalarOperator(ADD)
    @SqlType(BigIntegerType.BIG_INTEGER)
    public static BigInteger add(@SqlType(BigIntegerType.BIG_INTEGER) Slice left, @SqlType(BigIntegerType.BIG_INTEGER) Slice right)
    {
        return toBigInteger(left).add(toBigInteger(right));
    }

    private static BigInteger toBigInteger(Slice left) {
        return converter.fromSlice(left);
    }

    @ScalarOperator(SUBTRACT)
    @SqlType(BigIntegerType.BIG_INTEGER)
    public static BigInteger subtract(@SqlType(BigIntegerType.BIG_INTEGER) Slice left, @SqlType(BigIntegerType.BIG_INTEGER) Slice right)
    {
        return toBigInteger(left).subtract(toBigInteger(right));
    }

    @ScalarOperator(MULTIPLY)
    @SqlType(BigIntegerType.BIG_INTEGER)
    public static BigInteger multiply(@SqlType(BigIntegerType.BIG_INTEGER) Slice left, @SqlType(BigIntegerType.BIG_INTEGER) Slice right)
    {
        return toBigInteger(left).multiply(toBigInteger(right));
    }

    @ScalarOperator(DIVIDE)
    @SqlType(BigIntegerType.BIG_INTEGER)
    public static BigInteger divide(@SqlType(BigIntegerType.BIG_INTEGER) Slice left, @SqlType(BigIntegerType.BIG_INTEGER) Slice right)
    {
        try {
            return toBigInteger(left).divide(toBigInteger(right));
        }
        catch (ArithmeticException e) {
            throw new PrestoException(DIVISION_BY_ZERO, e);
        }
    }

    @ScalarOperator(MODULUS)
    @SqlType(BigIntegerType.BIG_INTEGER)
    public static BigInteger modulus(@SqlType(BigIntegerType.BIG_INTEGER) Slice left, @SqlType(BigIntegerType.BIG_INTEGER) Slice right)
    {
        try {
            return toBigInteger(left).remainder(toBigInteger(right));
        }
        catch (ArithmeticException e) {
            throw new PrestoException(DIVISION_BY_ZERO, e);
        }
    }

    @ScalarOperator(NEGATION)
    @SqlType(BigIntegerType.BIG_INTEGER)
    public static BigInteger negate(@SqlType(BigIntegerType.BIG_INTEGER) Slice value)
    {
        return toBigInteger(value).negate();
    }

    @SuppressWarnings("java:S1221")
    @ScalarOperator(EQUAL)
    @SqlType(StandardTypes.BOOLEAN)
    @SqlNullable
    public static Boolean equal(@SqlType(BigIntegerType.BIG_INTEGER) Slice left, @SqlType(BigIntegerType.BIG_INTEGER) Slice right)
    {
        return left.equals(right);
    }

    @ScalarOperator(NOT_EQUAL)
    @SuppressWarnings("FloatingPointEquality")
    @SqlType(StandardTypes.BOOLEAN)
    @SqlNullable
    public static Boolean notEqual(@SqlType(BigIntegerType.BIG_INTEGER) Slice left, @SqlType(BigIntegerType.BIG_INTEGER) Slice right)
    {
        return !left.equals(right);
    }

    @ScalarOperator(LESS_THAN)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean lessThan(@SqlType(BigIntegerType.BIG_INTEGER) Slice left, @SqlType(BigIntegerType.BIG_INTEGER) Slice right)
    {
        return left.compareTo(right) < 0;
    }

    @ScalarOperator(LESS_THAN_OR_EQUAL)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean lessThanOrEqual(@SqlType(BigIntegerType.BIG_INTEGER) Slice left, @SqlType(BigIntegerType.BIG_INTEGER) Slice right)
    {
        return left.compareTo(right) <= 0;
    }

    @ScalarOperator(GREATER_THAN)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean greaterThan(@SqlType(BigIntegerType.BIG_INTEGER) Slice left, @SqlType(BigIntegerType.BIG_INTEGER) Slice right)
    {
        return left.compareTo(right) > 0;
    }

    @ScalarOperator(GREATER_THAN_OR_EQUAL)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean greaterThanOrEqual(@SqlType(BigIntegerType.BIG_INTEGER) Slice left, @SqlType(BigIntegerType.BIG_INTEGER) Slice right)
    {
        return left.compareTo(right) >= 0;
    }

    @ScalarOperator(BETWEEN)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean between(@SqlType(BigIntegerType.BIG_INTEGER) Slice value, @SqlType(BigIntegerType.BIG_INTEGER) Slice min, @SqlType(BigIntegerType.BIG_INTEGER) Slice max)
    {
        return min.compareTo(value) <= 0 && value.compareTo(max) <= 0;
    }

    @ScalarOperator(CAST)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean castToBoolean(@SqlType(BigIntegerType.BIG_INTEGER) Slice value)
    {
        return !value.equals(BigInteger.ZERO);
    }

    @ScalarOperator(CAST)
    @SqlType(StandardTypes.INTEGER)
    public static long castToInteger(@SqlType(BigIntegerType.BIG_INTEGER) Slice value)
    {
        try {
            return toIntExact(toBigInteger(value).longValue());
        }
        catch (ArithmeticException e) {
            throw new PrestoException(NUMERIC_VALUE_OUT_OF_RANGE, "Out of range for integer: " + value, e);
        }
    }


    @ScalarOperator(CAST)
    @SqlType(StandardTypes.SMALLINT)
    public static long castToSmallint(@SqlType(BigIntegerType.BIG_INTEGER) Slice value)
    {
        try {

            return Shorts.checkedCast(toBigInteger(value).longValue());
        }
        catch (IllegalArgumentException e) {
            throw new PrestoException(NUMERIC_VALUE_OUT_OF_RANGE, "Out of range for smallint: " + value, e);
        }
    }

    @ScalarOperator(CAST)
    @SqlType(StandardTypes.TINYINT)
    public static long castToTinyint(@SqlType(BigIntegerType.BIG_INTEGER) Slice value)
    {
        try {
            return SignedBytes.checkedCast(toBigInteger(value).longValue());
        }
        catch (IllegalArgumentException e) {
            throw new PrestoException(NUMERIC_VALUE_OUT_OF_RANGE, "Out of range for tinyint: " + value, e);
        }
    }

    @ScalarOperator(CAST)
    @SqlType(StandardTypes.BIGINT)
    public static long castToLong(@SqlType(BigIntegerType.BIG_INTEGER) Slice value)
    {
        try {
            return toBigInteger(value).longValue();
        }
        catch (ArithmeticException e) {
            throw new PrestoException(INVALID_CAST_ARGUMENT, format("Unable to cast %s to bigint", value), e);
        }
    }

    @ScalarOperator(CAST)
    @SqlType(StandardTypes.REAL)
    public static long castToReal(@SqlType(BigIntegerType.BIG_INTEGER) Slice value)
    {
        return floatToRawIntBits(((Double) toBigInteger(value).doubleValue()).floatValue());
    }

    @ScalarOperator(CAST)
    @SqlType(StandardTypes.DOUBLE)
    public static double castToDouble(@SqlType(BigIntegerType.BIG_INTEGER) Slice value)
    {
        return (toBigInteger(value).doubleValue());
    }

    @ScalarOperator(CAST)
    @LiteralParameters("x")
    @SqlType("varchar(x)")
    public static Slice castToVarchar(@SqlType(BigIntegerType.BIG_INTEGER) Slice value)
    {
        return utf8Slice(String.valueOf(value));
    }

    @ScalarOperator(HASH_CODE)
    @SqlType(StandardTypes.BIGINT)
    public static long hashCode(@SqlType(BigIntegerType.BIG_INTEGER) Slice value)
    {
        return AbstractLongType.hash(toBigInteger(value).longValue());
    }

    @ScalarOperator(INDETERMINATE)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean indeterminate(@SqlType(BigIntegerType.BIG_INTEGER) Slice value, @IsNull boolean isNull)
    {
        return isNull;
    }

    @ScalarOperator(SATURATED_FLOOR_CAST)
    @SqlType(StandardTypes.REAL)
    public static strictfp long saturatedFloorCastToFloat(@SqlType(BigIntegerType.BIG_INTEGER) Slice value)
    {
        float result = toBigInteger(value).floatValue();
        return floatToRawIntBits(result);
    }

    @ScalarOperator(SATURATED_FLOOR_CAST)
    @SqlType(StandardTypes.INTEGER)
    public static long saturatedFloorCastToInteger(@SqlType(BigIntegerType.BIG_INTEGER) Slice value)
    {
        return saturatedFloorCastToLong(toBigInteger(value), Integer.MIN_VALUE, MIN_INTEGER_AS_BIG_INTEGER, Integer.MAX_VALUE, MAX_INTEGER_PLUS_ONE_AS_BIG_INTEGER);
    }

    @ScalarOperator(SATURATED_FLOOR_CAST)
    @SqlType(StandardTypes.SMALLINT)
    public static long saturatedFloorCastToSmallint(@SqlType(BigIntegerType.BIG_INTEGER) Slice value)
    {
        return saturatedFloorCastToLong(toBigInteger(value), Short.MIN_VALUE, MIN_SHORT_AS_BIG_INTEGER, Short.MAX_VALUE, MAX_SHORT_PLUS_ONE_AS_BIG_INTEGER);
    }

    @ScalarOperator(SATURATED_FLOOR_CAST)
    @SqlType(StandardTypes.TINYINT)
    public static long saturatedFloorCastToTinyint(@SqlType(BigIntegerType.BIG_INTEGER) Slice value)
    {
        return saturatedFloorCastToLong(toBigInteger(value), Byte.MIN_VALUE, MIN_BYTE_AS_BIG_INTEGER, Byte.MAX_VALUE, MAX_BYTE_PLUS_ONE_AS_BIG_INTEGER);
    }

    private static long saturatedFloorCastToLong(BigInteger value, long minValue, BigInteger minValueAsBigInteger, long maxValue, BigInteger maxValuePlusOneAsBigInteger)
    {
        if (value.compareTo(minValueAsBigInteger) <= 0) {
            return minValue;
        }
        if (value.add(BigInteger.ONE).compareTo(maxValuePlusOneAsBigInteger) >= 0) {
            return maxValue;
        }
        return value.longValue();
    }

    @ScalarOperator(IS_DISTINCT_FROM)
    public static class BigIntegerDistinctFromOperator
    {
        @SqlType(StandardTypes.BOOLEAN)
        public static boolean isDistinctFrom(
                @SqlType(BigIntegerType.BIG_INTEGER) Slice left,
                @IsNull boolean leftNull,
                @SqlType(BigIntegerType.BIG_INTEGER) Slice right,
                @IsNull boolean rightNull)
        {
            if (leftNull != rightNull) {
                return true;
            }
            if (leftNull) {
                return false;
            }
            return notEqual(left, right);
        }

        @SqlType(StandardTypes.BOOLEAN)
        public static boolean isDistinctFrom(
                @BlockPosition @SqlType(value = BigIntegerType.BIG_INTEGER, nativeContainerType = BigInteger.class) Block leftBlock,
                @BlockIndex int leftPosition,
                @BlockPosition @SqlType(value = BigIntegerType.BIG_INTEGER, nativeContainerType = BigInteger.class) Block rightBlock,
                @BlockIndex int rightPosition)
        {
            if (leftBlock.isNull(leftPosition) != rightBlock.isNull(rightPosition)) {
                return true;
            }
            if (leftBlock.isNull(leftPosition)) {
                return false;
            }
            BigInteger left = BIG_INTEGER_TYPE.getBigInteger(leftBlock, leftPosition);
            BigInteger right = BIG_INTEGER_TYPE.getBigInteger(rightBlock, rightPosition);

            return !left.equals(right);
        }
    }

    @ScalarOperator(XX_HASH_64)
    @SqlType(StandardTypes.BIGINT)
    public static long xxHash64(@SqlType(BigIntegerType.BIG_INTEGER) Slice value)
    {
        return XxHash64.hash(toBigInteger(value).longValue());
    }
}
