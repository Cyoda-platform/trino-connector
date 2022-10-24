package com.cyoda.presto.client.types;

import com.facebook.presto.common.block.Block;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.spi.function.BlockIndex;
import com.facebook.presto.spi.function.BlockPosition;
import com.facebook.presto.spi.function.IsNull;
import com.facebook.presto.spi.function.ScalarOperator;
import com.facebook.presto.spi.function.SqlType;
import io.airlift.slice.Slice;

import java.math.BigDecimal;

import static com.cyoda.presto.client.types.BigDecimalType.BIG_DECIMAL_TYPE;
import static com.facebook.presto.common.function.OperatorType.IS_DISTINCT_FROM;

@ScalarOperator(IS_DISTINCT_FROM)
public class BigDecimalDistinctFromOperator {
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean isDistinctFrom(
            @SqlType(BigDecimalType.BIG_DECIMAL) Slice left,
            @IsNull boolean leftNull,
            @SqlType(BigDecimalType.BIG_DECIMAL) Slice right,
            @IsNull boolean rightNull) {
        if (leftNull != rightNull) {
            return true;
        }
        if (leftNull) {
            return false;
        }
        return BigDecimalOperators.notEqual(left, right);
    }

    @SqlType(StandardTypes.BOOLEAN)
    public static boolean isDistinctFrom(
            @BlockPosition @SqlType(value = BigDecimalType.BIG_DECIMAL, nativeContainerType = BigDecimal.class) Block leftBlock,
            @BlockIndex int leftPosition,
            @BlockPosition @SqlType(value = BigDecimalType.BIG_DECIMAL, nativeContainerType = BigDecimal.class) Block rightBlock,
            @BlockIndex int rightPosition) {
        if (leftBlock.isNull(leftPosition) != rightBlock.isNull(rightPosition)) {
            return true;
        }
        if (leftBlock.isNull(leftPosition)) {
            return false;
        }
        BigDecimal left = BIG_DECIMAL_TYPE.getBigDecimal(leftBlock, leftPosition);
        BigDecimal right = BIG_DECIMAL_TYPE.getBigDecimal(rightBlock, rightPosition);

        return !left.equals(right);
    }
}
