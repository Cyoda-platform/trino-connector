package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.types.IDataType;
import io.airlift.json.JsonCodec;
import io.airlift.json.ObjectMapperProvider;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Type;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Suppliers;

import javax.annotation.Nonnull;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

public abstract class SingleValueConverter<T> extends AbstractValueConverter<T> {

    public static final Supplier<ObjectMapper> OBJECT_MAPPER_SUPPLIER = Suppliers.memoize(
            () -> new ObjectMapperProvider().get().enable(INDENT_OUTPUT))::get;

    public SingleValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    protected static String cleanUpJson(@Nonnull String json) {
        String result;
        // We get additional quotes from jackson, if the field is an Optional
        if ( isSingleElementJson(json) || json.startsWith("\"")) {
            result = json.substring(1, json.length() - 1);
        } else {
            result = json;
        }
        return result;
    }

    private static boolean isSingleElementJson(String json) {
        if ( json.startsWith("{") ) {
            try {
                Map<String,?> map = OBJECT_MAPPER_SUPPLIER.get().reader().forType(Map.class).readValue(json);
                return map.size() == 1;
            } catch (JsonProcessingException e) {
                return false;
            }
        } else return false;
    }

    @Override
    public T fromCyodaNative(@Nonnull Object cyodaNative, String columnName) {
        return fromCyodaNative(cyodaNative, columnName, false);
    }

    public T fromCyodaNative(Object cyodaNative, String columnName, boolean fromCollection) {
        if (cyodaNative == null) return null;
        if (!fromCollection && cyodaNative instanceof List<?> list){
            if (list.size() == 1)
                return fromCyodaNative(list.getFirst(), columnName);
        }
        if (!getClazz().isAssignableFrom(cyodaNative.getClass())) {
            LOG.debug(String.format("Column type mismatch \"%s\"\nExpected %s \nReceived: %s. \nTrying to convert...",
                    columnName, getClazz().getName(), cyodaNative.getClass().getName()));
            return fromOtherCyodaType(cyodaNative, columnName);
        }
        return super.fromCyodaNative(cyodaNative, columnName);
    }

    public void writeCyodaNativeFromCollection(Type type, BlockBuilder builder, Object cyodaNative, String columnName){
        if (cyodaNative == null) {
            builder.appendNull();
        } else {
            writeValue(type, builder, fromCyodaNative(cyodaNative, columnName, true));
        }
    }

//    @Override
//    public String stringify(T value){
//        // Let Jackson do the work, so that we have consistent formatting
//        final String json = JsonCodec.jsonCodec(getClazz()).toJson(value);
//        return cleanUpJson(json);
//    }
}
