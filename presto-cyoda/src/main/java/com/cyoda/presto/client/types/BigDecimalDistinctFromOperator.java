//package com.cyoda.presto.client.types;
//
//import io.trino.spi.block.Block;
//import io.trino.spi.type.StandardTypes;
//import io.trino.spi.function.BlockIndex;
//import io.trino.spi.function.BlockPosition;
//import io.trino.spi.function.IsNull;
//import io.trino.spi.function.ScalarOperator;
//import io.trino.spi.function.SqlType;
//import io.airlift.slice.Slice;
//
//import java.math.BigDecimal;
//
//import static com.cyoda.presto.client.types.BigDecimalType.BIG_DECIMAL_TYPE;
//import static io.trino.spi.function.OperatorType.IS_DISTINCT_FROM;
//
//@ScalarOperator(IS_DISTINCT_FROM)
//public class BigDecimalDistinctFromOperator {
//    @SqlType(StandardTypes.BOOLEAN)
//    public static boolean isDistinctFrom(
//            @SqlType(BigDecimalType.BIG_DECIMAL) Slice left,
//            @IsNull boolean leftNull,
//            @SqlType(BigDecimalType.BIG_DECIMAL) Slice right,
//            @IsNull boolean rightNull) {
//        if (leftNull != rightNull) {
//            return true;
//        }
//        if (leftNull) {
//            return false;
//        }
//        return BigDecimalOperators.notEqual(left, right);
//    }
//
//    @SqlType(StandardTypes.BOOLEAN)
//    public static boolean isDistinctFrom(
//            @BlockPosition @SqlType(value = BigDecimalType.BIG_DECIMAL, nativeContainerType = Slice.class) Block leftBlock,
//            @BlockIndex int leftPosition,
//            @BlockPosition @SqlType(value = BigDecimalType.BIG_DECIMAL, nativeContainerType = Slice.class) Block rightBlock,
//            @BlockIndex int rightPosition) {
//        if (leftBlock.isNull(leftPosition) != rightBlock.isNull(rightPosition)) {
//            return true;
//        }
//        if (leftBlock.isNull(leftPosition)) {
//            return false;
//        }
//        BigDecimal left = BIG_DECIMAL_TYPE.getBigDecimal(leftBlock, leftPosition);
//        BigDecimal right = BIG_DECIMAL_TYPE.getBigDecimal(rightBlock, rightPosition);
//
//        return !left.equals(right);
//    }
//}
