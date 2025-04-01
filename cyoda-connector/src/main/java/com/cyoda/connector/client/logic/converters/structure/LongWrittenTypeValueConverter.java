package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.types.IDataType;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Type;

import javax.annotation.Nonnull;

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

}
