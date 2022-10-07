package com.cyoda.presto.client.logic.converters.structure;

import com.cyoda.presto.client.types.IDataType;
import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.type.Type;

import javax.annotation.Nonnull;

public abstract class LongWrittenTypeValueConverter<T extends Comparable<? super T>> extends ComparableValueConverter<T>{


    public abstract long toLong(@Nonnull T value);
    @Nonnull
    public abstract T fromLong(long value);

    public LongWrittenTypeValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    @Override
    public void writeValue(Type type, BlockBuilder builder, @Nonnull T value) {
        builder.writeLong(toLong(value)).closeEntry();
    }
    @Override
    public T fromPrestoNative(Object nativeValue) {
        return fromLong((Long) nativeValue);
    }

}
