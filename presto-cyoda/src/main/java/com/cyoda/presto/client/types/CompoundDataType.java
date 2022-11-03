package com.cyoda.presto.client.types;

import com.cyoda.presto.client.jodabeans.StandardColumnDefinition;
import com.cyoda.presto.client.logic.converters.PrestoConverterFactory;
import com.cyoda.presto.client.logic.converters.PrestoValueConverter;
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.TypeSignature;
import com.facebook.presto.common.type.TypeSignatureParameter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.joda.beans.MetaProperty;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
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
        return of(type, columnName, DataType::fromClassExact);
    }

    private static CompoundDataType of(ParameterizedType type, String columnName, BiFunction<Class<?>, String, DataType> dataTypeFinder){
        DataType mainType = dataTypeFinder.apply((Class<?>) type.getRawType(), columnName);
        DataType[] typeParams = new DataType[mainType.getTypeParametersCount()];
        if (mainType.getTypeParametersCount() > 0) {
            java.lang.reflect.Type[] actualTypeArguments = type.getActualTypeArguments();
            if (actualTypeArguments.length < mainType.getTypeParametersCount())
                throw new IllegalArgumentException(String.format("Error creating column \"%s\"(%s): Not matching type arguments for type %s",
                        columnName, mainType, type));
            for (int i = 0; i < mainType.getTypeParametersCount(); i++) {
                typeParams[i] = dataTypeFinder.apply((Class<?>) actualTypeArguments[i],columnName);
            }
        }
        return new CompoundDataType(columnName, mainType, typeParams);
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
            if (mainType.getStaticParams().isEmpty())
                return typeManager.getType(new TypeSignature(mainType.getTypeString()));
            else
                return typeManager.getParameterizedType(mainType.getTypeString(), mainType.getStaticParams());
        } else {
            List<TypeSignatureParameter> attrs = Arrays.stream(typeParams)
                    .map(DataType::getTypeString)
                    .map(TypeSignature::new)
                    .map(TypeSignatureParameter::of)
                    .collect(Collectors.toList());
            return typeManager.getParameterizedType(mainType.getTypeString(), attrs);
        }
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
