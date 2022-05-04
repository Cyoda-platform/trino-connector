/*
 * Copyright (C) 2022 Cyoda Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.cyoda.presto.client.jodabeans;

import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.client.types.SupportedDataType;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.TypeSignature;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectMetaBean;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StandardColumnDefinition implements ColumnDefinition {

    private final int pos;
    private final String fieldName;
    private final String fieldTypeString;
    private final DataType dataType;
    private final TypeSignature parType;
    private final TypeSignature mapValueType;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("pos", pos)
                .add("fieldName", fieldName)
                .add("fieldTypeString", fieldTypeString)
                .add("dataType", dataType)
                .add("parType", parType)
                .toString();
    }

    public StandardColumnDefinition(int pos, String fieldName, String fieldTypeString, DataType dateType, TypeSignature parType, TypeSignature mapValueType) {
        this.pos = pos;
        this.fieldName = fieldName;
        this.fieldTypeString = fieldTypeString;
        this.dataType = dateType;
        this.parType = parType;
        this.mapValueType = mapValueType;
    }

    @Override
    public int getPos() {
        return pos;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String getFieldTypeString() {
        return fieldTypeString;
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public TypeSignature getParType() {
        return parType;
    }

    @Override
    public TypeSignature getMapValuetype() {
        return mapValueType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final ImmutableList.Builder<ColumnDefinition> defsBuilder;
        int pos = 0;
        private Builder() {
            this.defsBuilder = ImmutableList.builder();
        }
        public Builder add(DirectMetaBean metaBean) {
            List<ColumnDefinition> fromMeta = of(pos,metaBean);
            pos += fromMeta.size();
            defsBuilder.addAll(fromMeta);
            return this;
        }
        public Builder add(ColumnDefinition colDef) {
            StandardColumnDefinition thisColDef = new StandardColumnDefinition(
                    pos,colDef.getFieldName(),colDef.getFieldTypeString(),colDef.getDataType(),
                    colDef.getParType(),colDef.getMapValuetype()
            );
            defsBuilder.add(thisColDef);
            pos += 1;
            return this;
        }
        public List<ColumnDefinition> build() {
            return defsBuilder.build();
        }

        private static List<ColumnDefinition> of(int pos, DirectMetaBean metaBean) {
            Map<String, MetaProperty<?>> metaPropertyMap = metaBean.metaPropertyMap();

            String[] keys = metaPropertyMap.keySet().toArray(new String[0]);
            return IntStream
                    .range(0, metaPropertyMap.size())
                    .mapToObj(i -> {
                        String key = keys[i];
                        MetaProperty<?> metaProperty = metaPropertyMap.get(key);
                        String fieldName = metaProperty.name();
                        java.lang.reflect.Type genericType = metaProperty.propertyGenericType();
                        if ( genericType instanceof ParameterizedType) {
                            ParameterizedType myType = (ParameterizedType) genericType;
                            if (Map.class.isAssignableFrom((Class<?>)myType.getRawType()) ) {
                                return handleMap(pos, i, fieldName, myType);
                            }
                            if (List.class.isAssignableFrom((Class<?>)myType.getRawType()) ) {
                                return handleCollection(myType, i, fieldName, DataType.LIST);
                            }
                            if (Set.class.isAssignableFrom((Class<?>)myType.getRawType()) ) {
                                return handleCollection(myType, i, fieldName, DataType.SET);
                            }
                            throw new IllegalArgumentException("Not yet done");
                        } else {
                            DataType dataType = DataType.fromClass(metaProperty.propertyType()).orElse(DataType.OBJECT);
                            String fieldTypeString = dataType.getTypeString();
                            return (ColumnDefinition) new StandardColumnDefinition(i,fieldName,fieldTypeString,dataType,null,null);
                        }
                    }).collect(Collectors.toList());
        }

        private static StandardColumnDefinition handleCollection(ParameterizedType myType, int i, String fieldName, DataType list) {
            java.lang.reflect.Type valueType = myType.getActualTypeArguments()[0];
            if (! (valueType instanceof Class) ) throw new UnsupportedOperationException("Not done yet");
            DataType valueDataType = DataType.fromClass((Class<?>) valueType).orElse(DataType.OBJECT);
            return new StandardColumnDefinition(i, fieldName, StandardTypes.ARRAY, list,
                    SupportedDataType.toPrestoTypeSignature(valueDataType), null);
        }

        private static StandardColumnDefinition handleMap(int pos, int i, String fieldName, ParameterizedType myType) {
            java.lang.reflect.Type keyType = myType.getActualTypeArguments()[0];
            java.lang.reflect.Type valueType = myType.getActualTypeArguments()[1];
            DataType keyDataType = DataType.fromClass((Class<?>)keyType).orElse(DataType.OBJECT);
            DataType valueDataType = DataType.fromClass((Class<?>)valueType).orElse(DataType.OBJECT);
            return new StandardColumnDefinition(
                    i + pos,
                    fieldName,
                    StandardTypes.MAP,
                    DataType.MAP,
                    SupportedDataType.toPrestoTypeSignature(keyDataType),
                    SupportedDataType.toPrestoTypeSignature(valueDataType)
            );
        }
    }



}