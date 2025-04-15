package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.types.IDataType;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.block.LongArrayBlock;
import io.trino.spi.type.Type;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class LongWrittenTypeValueConverter<T extends Comparable<? super T>> extends ComparableValueConverter<T>{


    public abstract Long toLong(@Nonnull T value);
    @Nonnull
    public abstract T fromLong(Long value);

    public LongWrittenTypeValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    @Override
    public void writeValue(Type type, BlockBuilder builder, @Nonnull T value) {
        type.writeLong(builder, toLong(value));
    }
    @Override
    public T fromPrestoNative(Object nativeValue) {
        return fromLong((Long) nativeValue);
    }

    @Override
    public List<T> blockToNativeList(Object nativeBlock, Type trinoType) {
        LongArrayBlock block = (LongArrayBlock) nativeBlock;
        ArrayList<T> res = new ArrayList<>();
        for (int i = 0; i < block.getPositionCount(); i++) {
            res.add(fromLong(block.getLong(i)));
        }
        return res;
    }
}
