package com.cyoda.presto.client.logic.converters.structure;

import com.cyoda.presto.client.types.IDataType;

import javax.annotation.Nonnull;
import java.time.Instant;

public abstract class TimestampTypeValueConverter<T extends Comparable<? super T>> extends LongComparedTypeValueConverter<T>{
    private static final long TIMESTAMP_LONG_MULTIPLIER = 1000L;

    protected abstract Instant toInstant(T value);
    protected abstract T fromInstant(Instant value);

    public TimestampTypeValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    @Override
    public long minValueOfIntType() {
        return Long.MIN_VALUE;
    }

    @Override
    public long maxValueOfIntType() {
        return Long.MAX_VALUE;
    }

    @Override
    public long toLong(@Nonnull T value) {
        return toInstant(value).toEpochMilli()*TIMESTAMP_LONG_MULTIPLIER;
    }

    @Nonnull
    @Override
    public T fromLong(long value) {
        return fromInstant(Instant.ofEpochMilli(value/TIMESTAMP_LONG_MULTIPLIER));
    }

}
