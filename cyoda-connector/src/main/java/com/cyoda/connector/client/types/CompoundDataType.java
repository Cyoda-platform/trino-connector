package com.cyoda.connector.client.types;

import com.cyoda.connector.client.logic.converters.PrestoConverterFactory;
import com.cyoda.connector.client.logic.converters.PrestoValueConverter;
import io.trino.spi.type.Type;
import io.trino.spi.type.TypeManager;
import io.trino.spi.type.TypeSignature;
import io.trino.spi.type.TypeSignatureParameter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.joda.beans.MetaProperty;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class CompoundDataType {
    private final DataType mainType;
    private final DataType[] typeParams;
    private final String columnName; // For exception messages

    @JsonCreator
    public CompoundDataType(@JsonProperty("columnName") String columnName,
                            @JsonProperty("mainType") DataType mainType,
                            @JsonProperty("typeParams") DataType... typeParams){
        this.columnName = columnName;
        if (mainType == null)
            throw new IllegalArgumentException("No DataType for field " + columnName);
        this.mainType = mainType;
        if (typeParams.length != mainType.getTypeParametersCount())
            throw new IllegalArgumentException(String.format("Invalid type parameters set (%s) for field %s(%s)",
                    Arrays.toString(typeParams), columnName, mainType));
        this.typeParams = typeParams;
    }

    public static CompoundDataType of(ParameterizedType type, String columnName){
        return of(type, columnName, DataType::fromClassFSToString);
    }

    private static CompoundDataType of(ParameterizedType type, String columnName, BiFunction<Class<?>, String, DataType> dataTypeFinder){
        DataType mainType = plainTypeToDataType(type.getRawType(), columnName, dataTypeFinder);
        DataType[] typeParams = new DataType[mainType.getTypeParametersCount()];
        if (mainType.getTypeParametersCount() > 0) {
            java.lang.reflect.Type[] actualTypeArguments = type.getActualTypeArguments();
            if (actualTypeArguments.length < mainType.getTypeParametersCount())
                throw new IllegalArgumentException(String.format("Error creating column \"%s\"(%s): Not matching type arguments for type %s",
                        columnName, mainType, type));
            for (int i = 0; i < mainType.getTypeParametersCount(); i++) {
                typeParams[i] = plainTypeToDataType(actualTypeArguments[i], columnName, dataTypeFinder);
            }
        }
        return new CompoundDataType(columnName, mainType, typeParams);
    }

    private static DataType plainTypeToDataType(java.lang.reflect.Type type, String columnName, BiFunction<Class<?>, String, DataType> dataTypeFinder){
        if (type instanceof Class<?>){
            return dataTypeFinder.apply((Class<?>) type, columnName);
        } else if (type instanceof GenericArrayType) {
            java.lang.reflect.Type elementType = ((GenericArrayType)type).getGenericComponentType();
            if (byte.class.equals(elementType))
                return DataType.BYTE_ARRAY;
        }
        throw new RuntimeException("Type " + type.getTypeName() + " is not supported");
    }

    public static CompoundDataType of(MetaProperty<?> metaProperty){
        String fieldName = metaProperty.name();
        java.lang.reflect.Type genericType = metaProperty.propertyGenericType();
        if ( genericType instanceof ParameterizedType) {
            ParameterizedType myType = (ParameterizedType) genericType;
            return of(myType, fieldName, DataType::fromClassFSToObject);
        } else {
            return new CompoundDataType(fieldName, DataType.fromClassFSToObject(metaProperty.propertyType(),fieldName));
        }
    }

    @JsonProperty
    public DataType getMainType() {
        return mainType;
    }

    @JsonProperty
    public DataType[] getTypeParams() {
        return typeParams;
    }

    @JsonProperty
    public String getColumnName() {
        return columnName;
    }

    public PrestoValueConverter<?> getConverter(){
        return PrestoConverterFactory.getConverter(mainType, typeParams, columnName);
    }

    public Type toPrestoType(TypeManager typeManager){
        if (mainType.getTypeParametersCount() == 0) {
            return typeManager.getType(toTypeSignature(mainType));
        } else {
            List<TypeSignatureParameter> attrs = Arrays.stream(typeParams)
                    .map(CompoundDataType::toTypeSignature)
                    .map(TypeSignatureParameter::typeParameter)
                    .collect(Collectors.toList());
            return typeManager.getParameterizedType(mainType.getTypeString(), attrs);
        }
    }

    private static TypeSignature toTypeSignature(DataType dataType) {
        if (dataType.getStaticParams().isEmpty())
            return new TypeSignature(dataType.getTypeString());
        else
            return new TypeSignature(dataType.getTypeString(), dataType.getStaticParams());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("columnName", columnName)
                .add("mainType", mainType)
                .add("typeParams", Arrays.toString(typeParams))
                .toString();
    }
}
