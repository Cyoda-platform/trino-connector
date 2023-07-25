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

package com.cyoda.presto.client.logic.converters.impl;

import com.cyoda.presto.client.logic.converters.structure.SliceJsonValueConverter;
import com.cyoda.presto.client.types.DataType;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.airlift.slice.Slices;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Type;

@SuppressWarnings({"ALL","java:S3740"})
public class ClassPrestoValueConverter extends SliceJsonValueConverter<Class> {

    public ClassPrestoValueConverter() {
        super(DataType.CLASS);
    }

    @Override
    public void writeCyodaNative(Type type, BlockBuilder builder, Object cyodaNative, String columnName) {
        if (cyodaNative == null){
            builder.appendNull();
            return;
        }
        try {
            String json = OBJECT_MAPPER_SUPPLIER.get().writerFor(String.class).writeValueAsString(cyodaNative.toString());
            String clean = cleanUpJson(json);
            type.writeSlice(builder, Slices.utf8Slice(clean));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot convert to json",e);
        }

    }

    @Override
    public void writeCyodaNativeFromCollection(Type type, BlockBuilder builder, Object cyodaNative, String columnName) {
        writeCyodaNative(type, builder, cyodaNative, columnName);
    }
}
