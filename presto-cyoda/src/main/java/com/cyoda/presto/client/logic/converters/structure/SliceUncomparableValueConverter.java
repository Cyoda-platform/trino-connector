package com.cyoda.presto.client.logic.converters.structure;

import com.cyoda.presto.client.types.IDataType;
import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.type.Type;
import io.airlift.slice.Slice;

import javax.annotation.Nonnull;

public abstract class SliceUncomparableValueConverter<T> extends SingleValueConverter<T> {

    public abstract Slice toSlice(@Nonnull T value);
    public abstract @Nonnull T fromSlice(Slice value);

    public SliceUncomparableValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    @Override
    protected void writeValueInternal(Type type, BlockBuilder builder, @Nonnull T value) {
        type.writeSlice(builder, toSlice(value));
    }
}
