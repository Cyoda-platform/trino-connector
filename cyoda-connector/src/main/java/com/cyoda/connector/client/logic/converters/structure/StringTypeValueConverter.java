package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.types.IDataType;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import javax.annotation.Nonnull;

public abstract class StringTypeValueConverter<T extends Comparable<? super T>> extends SliceComparableValueConverter<T>{

    public StringTypeValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    protected abstract T fromStr(String value);
    protected abstract String toStr(T value);

    @Override
    public String stringify(T value) {
        return toStr(value);
    }

    @Override
    public Slice toSlice(@Nonnull T value) {
        return Slices.utf8Slice(toStr(value));
    }

    @Nonnull
    @Override
    public T fromSlice(Slice value) {
        return fromStr(value.toStringUtf8());
    }

}
