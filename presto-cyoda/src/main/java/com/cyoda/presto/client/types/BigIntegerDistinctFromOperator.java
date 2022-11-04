package com.cyoda.presto.client.types;

import com.facebook.presto.common.block.Block;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.spi.function.BlockIndex;
import com.facebook.presto.spi.function.BlockPosition;
import com.facebook.presto.spi.function.IsNull;
import com.facebook.presto.spi.function.ScalarOperator;
import com.facebook.presto.spi.function.SqlType;
import io.airlift.slice.Slice;

import java.math.BigInteger;

import static com.cyoda.presto.client.types.BigIntegerType.BIG_INTEGER_TYPE;
import static com.facebook.presto.common.function.OperatorType.IS_DISTINCT_FROM;

@ScalarOperator(IS_DISTINCT_FROM)
public class BigIntegerDistinctFromOperator {
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean isDistinctFrom(
            @SqlType(BigIntegerType.BIG_INTEGER) Slice left,
            @IsNull boolean leftNull,
            @SqlType(BigIntegerType.BIG_INTEGER) Slice right,
            @IsNull boolean rightNull) {
        if (leftNull != rightNull) {
            return true;
        }
        if (leftNull) {
            return false;
        }
        return BigIntegerOperators.notEqual(left, right);
    }

    @SqlType(StandardTypes.BOOLEAN)
    public static boolean isDistinctFrom(
            @BlockPosition @SqlType(value = BigIntegerType.BIG_INTEGER, nativeContainerType = Slice.class) Block leftBlock,
            @BlockIndex int leftPosition,
            @BlockPosition @SqlType(value = BigIntegerType.BIG_INTEGER, nativeContainerType = Slice.class) Block rightBlock,
            @BlockIndex int rightPosition) {
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
