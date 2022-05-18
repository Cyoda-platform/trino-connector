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

import com.cyoda.presto.client.logic.converters.PrestoValueConverter;
import com.facebook.presto.common.type.Type;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import javax.annotation.Nonnull;
import java.util.Locale;

import static com.cyoda.presto.client.types.DataTypeValue.OBJECT_MAPPER_SUPPLIER;
import static com.cyoda.presto.client.types.DataTypeValue.cleanUpJson;

abstract class SliceValueConverter<T> implements PrestoValueConverter<T> {

    abstract Class<T> getClazz();

    @Override
    public Slice toSlice(@Nonnull Type type, @Nonnull T value) {
        try {
            String json = OBJECT_MAPPER_SUPPLIER.get().writerFor(Locale.class).writeValueAsString(value);
            String clean = cleanUpJson(json);
            return Slices.utf8Slice(clean);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot convert to json",e);
        }
    }

    @Nonnull
    @Override
    public T fromSlice(@Nonnull Type type, Slice value) {
        try {
            return OBJECT_MAPPER_SUPPLIER.get().readerFor(Locale.class).readValue(value.toStringUtf8());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot convert from json",e);
        }
    }

}
