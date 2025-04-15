package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.types.IDataType;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.block.IntArrayBlock;
import io.trino.spi.block.LongArrayBlock;
import io.trino.spi.type.Type;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class IntWrittenTypeValueConverter<T extends Comparable<? super T>> extends ComparableValueConverter<T>{


    public abstract Integer toInt(@Nonnull T value);
    @Nonnull
    public abstract T fromInt(Integer value);

    public IntWrittenTypeValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    @Override
    public void writeValue(Type type, BlockBuilder builder, @Nonnull T value) {
        type.writeLong(builder, toInt(value).longValue());
    }
    @Override
    public T fromPrestoNative(Object nativeValue) {
        return fromInt(((Long) nativeValue).intValue());
    }


    @Override
    public List<T> blockToNativeList(Object nativeBlock, Type trinoType) {
        IntArrayBlock intArrayBlock = (IntArrayBlock) nativeBlock;
        List<T> list = new ArrayList<>();
        for (int i = 0; i < intArrayBlock.getPositionCount(); i++) {
            list.add(fromInt(intArrayBlock.getInt(i)));
        }
        return list;
    }
}
