package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.types.IDataType;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Int128;
import io.trino.spi.type.Type;

import javax.annotation.Nonnull;

public abstract class LongDecimalTypeValueConverter<T extends Comparable<? super T>> extends ComparableValueConverter<T>{

    public LongDecimalTypeValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    protected abstract Int128 toInt128(T value);
    protected abstract T fromInt128(Int128 value);

    @Override
    public void writeValue(Type type, BlockBuilder builder, @Nonnull T value) {
        type.writeObject(builder, toInt128(value));
    }

    @Override
    public T fromPrestoNative(Object nativeValue) {
        return fromInt128((Int128) nativeValue);
    }


}
