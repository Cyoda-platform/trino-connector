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

package com.cyoda.presto.client.logic.converters.structure;

import com.cyoda.presto.client.types.IDataType;
import com.cyoda.presto.logging.SupplierLogger;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Type;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import javax.annotation.Nonnull;

public abstract class SliceJsonValueConverter<T> extends SingleValueConverter<T> {

    private static final SupplierLogger LOG = SupplierLogger.get(SliceJsonValueConverter.class);

    public SliceJsonValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    public Slice toSlice(@Nonnull T value) {
        String res;
        try {
            String json = OBJECT_MAPPER_SUPPLIER.get().writerFor(getClazz()).writeValueAsString(value);
            res = cleanUpJson(json);

        } catch (JsonProcessingException e) {
            LOG.error(e,"Cannot convert to json");
            res = value.toString();
        }
        return Slices.utf8Slice(res);
    }

    @Nonnull
    public T fromSlice(Slice value) {
        try {
            return OBJECT_MAPPER_SUPPLIER.get().readerFor(getClazz()).readValue(value.toStringUtf8());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot convert from json",e);
        }
    }

    @Override
    public void writeValue(Type type, BlockBuilder builder, @Nonnull T value) {
        type.writeSlice(builder, toSlice(value));
    }

}
