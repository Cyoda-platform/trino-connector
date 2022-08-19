package com.cyoda.presto.client.logic.converters.structure;

import com.cyoda.presto.client.logic.converters.PrestoValueConverter;
import io.airlift.slice.Slice;

import javax.annotation.Nonnull;

public interface SliceWrittenTypeValueConverter<T> extends PrestoValueConverter<T> {


    Slice toSlice(@Nonnull T value);
    @Nonnull T fromSlice(Slice value);

}
