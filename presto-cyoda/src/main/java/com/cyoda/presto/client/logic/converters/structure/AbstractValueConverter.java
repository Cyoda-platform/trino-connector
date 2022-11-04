package com.cyoda.presto.client.logic.converters.structure;

import com.cyoda.presto.client.logic.converters.PrestoValueConverter;
import com.cyoda.presto.client.types.IDataType;
import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.type.Type;

import javax.annotation.Nonnull;

public abstract class AbstractValueConverter<T> implements PrestoValueConverter<T> {
    private final IDataType<T> dataType;

    public abstract void writeValue(Type type, BlockBuilder builder, @Nonnull T value);
    public AbstractValueConverter(IDataType<T> dataType){
        this.dataType = dataType;
    }
    @Override
    public IDataType<T> getDataType() {
        return dataType;
    }

    @Override
    public Class<T> getClazz() {
        return dataType.getJavaType();
    }

    public T fromCyodaNative(@Nonnull Object cyodaNative, String columnName){
        return (T) cyodaNative;
    }

    @Override
    public void writeCyodaNative(Type type, BlockBuilder builder, Object cyodaNative, String columnName){
        if (cyodaNative == null){
            builder.appendNull();
            return;
        }
        writeValue(type, builder, fromCyodaNative(cyodaNative, columnName));
    }

}
