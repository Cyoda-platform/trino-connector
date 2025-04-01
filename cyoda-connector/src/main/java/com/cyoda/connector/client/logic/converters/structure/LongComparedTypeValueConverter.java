package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.types.IDataType;

public abstract class LongComparedTypeValueConverter<T extends Comparable<? super T>> extends LongWrittenTypeValueConverter<T>{
    public LongComparedTypeValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    public abstract long minValueOfIntType();
    public abstract long maxValueOfIntType();

    @Override
    public boolean areConsecutive(T a, T b) {
        return toLong(b) - toLong(a) == 1;
    }

}
