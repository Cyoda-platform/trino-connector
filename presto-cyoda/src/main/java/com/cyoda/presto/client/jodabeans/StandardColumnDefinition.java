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
import com.cyoda.presto.client.types.CompoundDataType;
import com.cyoda.presto.client.types.DataType;
import io.trino.spi.type.StandardTypes;
import io.trino.spi.type.TypeSignature;
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
    private final CompoundDataType dataType;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("pos", pos)
                .add("fieldName", fieldName)
                .add("dataType", dataType)
                .toString();
    }

    public StandardColumnDefinition(int pos, String fieldName, CompoundDataType dateType) {
        this.pos = pos;
        this.fieldName = fieldName;
        this.dataType = dateType;
    }

    public StandardColumnDefinition(int pos, String fieldName, DataType mainType, DataType... typeParams) {
        this.pos = pos;
        this.fieldName = fieldName;
        this.dataType = new CompoundDataType(fieldName, mainType, typeParams);
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
    public CompoundDataType getDataType() {
        return dataType;
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
                    pos,colDef.getFieldName(),colDef.getDataType()
            );
            defsBuilder.add(thisColDef);
            pos += 1;
            return this;
        }


        public Builder addAll(List<ColumnDefinition> coldefs) {
            coldefs.forEach(this::add);
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
                        CompoundDataType dataType = CompoundDataType.of(metaProperty);
                        return (ColumnDefinition) new StandardColumnDefinition(pos+i,fieldName,dataType);
                    }).collect(Collectors.toList());
        }
    }



}