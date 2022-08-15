package com.cyoda.presto.client.logic.converters.structure;

import com.cyoda.presto.client.logic.converters.PrestoValueConverter;
import com.cyoda.presto.client.types.IDataType;
import com.facebook.airlift.json.JsonCodec;
import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.type.Type;

import javax.annotation.Nonnull;

import static com.cyoda.presto.client.types.DataTypeValue.cleanUpJson;

public abstract class SingleValueConverter<T> implements PrestoValueConverter<T> {

    private final IDataType<T> dataType;

    public SingleValueConverter(IDataType<T> dataType){
        this.dataType = dataType;
    }

    public IDataType<T> getDataType() {
        return dataType;
    }

    public Class<T> getClazz() {
        return dataType.getJavaType();
    }

    protected abstract void writeValueInternal(Type type, BlockBuilder builder, @Nonnull T value);

    public void writeValue(Type type, BlockBuilder builder, T value){
        if (value == null){
            builder.appendNull();
        } else {
            writeValueInternal(type, builder, value);
        }
    }

    @Override
    public String stringify(T value){
        // Let Jackson do the work, so that we have consistent formatting
        final String json = JsonCodec.jsonCodec(getClazz()).toJson(value);
        return cleanUpJson(json);
    }
}
