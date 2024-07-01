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
//package com.cyoda.connector.client.types;
//
//import com.cyoda.connector.client.logic.converters.impl.BigIntegerPrestoValueConverter;
//import io.trino.spi.block.Block;
//import io.trino.spi.block.BlockBuilder;
//import io.trino.spi.function.SqlFunctionProperties;
//import io.trino.spi.type.AbstractVariableWidthType;
//import io.trino.spi.type.Type;
//import io.airlift.slice.Slice;
//
//import java.math.BigInteger;
//
//import static io.trino.spi.type.TypeSignature.parseTypeSignature;
//
//public final class BigIntegerType extends AbstractVariableWidthType {
//    public static final BigIntegerType BIG_INTEGER_TYPE = new BigIntegerType();
//    public static final String BIG_INTEGER = "biginteger";
//    public static final BigIntegerPrestoValueConverter PRESTO_VALUE_CONVERTER = new BigIntegerPrestoValueConverter();
//
//    private BigIntegerType()
//    {
//        super(parseTypeSignature(BIG_INTEGER), Slice.class);
//    }
//
//    public static boolean isBigIntegerType(Type type)
//    {
//        return type instanceof BigIntegerType;
//    }
//
//    @Override
//    public boolean isComparable()
//    {
//        return true;
//    }
//
//    @Override
//    public boolean isOrderable()
//    {
//        return true;
//    }
//
//    @Override
//    public Object getObjectValue(SqlFunctionProperties properties, Block block, int position)
//    {
//        if (block.isNull(position)) {
//            return null;
//        }
//        return PRESTO_VALUE_CONVERTER.fromSlice(block.getSlice(position, 0, block.getSliceLength(position)));
//    }
//
//    @Override
//    public boolean equalTo(Block leftBlock, int leftPosition, Block rightBlock, int rightPosition)
//    {
//        int leftLength = leftBlock.getSliceLength(leftPosition);
//        int rightLength = rightBlock.getSliceLength(rightPosition);
//        if (leftLength != rightLength) {
//            return false;
//        }
//        return leftBlock.equals(leftPosition, 0, rightBlock, rightPosition, 0, leftLength);
//    }
//
//    @Override
//    public long hash(Block block, int position)
//    {
//        return block.hash(position, 0, block.getSliceLength(position));
//    }
//
//    @Override
//    public int compareTo(Block leftBlock, int leftPosition, Block rightBlock, int rightPosition)
//    {
//        int leftLength = leftBlock.getSliceLength(leftPosition);
//        int rightLength = rightBlock.getSliceLength(rightPosition);
//        return leftBlock.compareTo(leftPosition, 0, leftLength, rightBlock, rightPosition, 0, rightLength);
//    }
//
//    @Override
//    public void appendTo(Block block, int position, BlockBuilder blockBuilder)
//    {
//        if (block.isNull(position)) {
//            blockBuilder.appendNull();
//        }
//        else {
//            block.writeBytesTo(position, 0, block.getSliceLength(position), blockBuilder);
//            blockBuilder.closeEntry();
//        }
//    }
//
//    @Override
//    public Slice getSlice(Block block, int position)
//    {
//        return block.getSlice(position, 0, block.getSliceLength(position));
//    }
//
//    @Override
//    public void writeSlice(BlockBuilder blockBuilder, Slice value)
//    {
//        writeSlice(blockBuilder, value, 0, value.length());
//    }
//
//    @Override
//    public void writeSlice(BlockBuilder blockBuilder, Slice value, int offset, int length)
//    {
//        blockBuilder.writeBytes(value, offset, length).closeEntry();
//    }
//
//    @Override
//    public boolean equals(Object other)
//    {
//        return other == BIG_INTEGER_TYPE;
//    }
//
//    @Override
//    public int hashCode()
//    {
//        return getClass().hashCode();
//    }
//
//    public BigInteger getBigInteger(Block block, int position) {
//        return PRESTO_VALUE_CONVERTER.fromSlice(block.getSlice(position, 0, block.getSliceLength(position)));
//    }
//}
