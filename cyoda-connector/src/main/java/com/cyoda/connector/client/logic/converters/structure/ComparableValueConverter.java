package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.types.IDataType;

public abstract class ComparableValueConverter<T extends Comparable<? super T>> extends SingleValueConverter<T>{

    public ComparableValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    @Override //enforcing definition of this function in subclasses
    public abstract T fromPrestoNative(Object nativeValue);


}
