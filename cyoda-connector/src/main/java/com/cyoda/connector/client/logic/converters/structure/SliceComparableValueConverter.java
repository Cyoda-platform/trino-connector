package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.types.IDataType;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.predicate.NullableValue;
import io.trino.spi.type.Type;
import io.airlift.slice.Slice;

import javax.annotation.Nonnull;

public abstract class SliceComparableValueConverter<T extends Comparable<? super T>>
        extends ComparableValueConverter<T> {

    public abstract Slice toSlice(@Nonnull T value);
    public abstract @Nonnull T fromSlice(Slice value);

    public SliceComparableValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    @Override
    public void writeValue(Type type, BlockBuilder builder, @Nonnull T value) {
        type.writeSlice(builder, toSlice(value));
    }
    @Override
    public T fromPrestoNative(Object nativeValue) {
        return fromSlice((Slice) nativeValue);
    }

    @Override
    public NullableValue toNullableValue(Type type, Object value) {
        return new NullableValue(type, toSlice((T)value));
    }
}
