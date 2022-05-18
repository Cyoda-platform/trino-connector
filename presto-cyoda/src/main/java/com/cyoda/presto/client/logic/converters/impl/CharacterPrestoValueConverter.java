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

import com.cyoda.presto.client.logic.converters.ComparablePrestoValueConverter;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.VarcharType;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

public class CharacterPrestoValueConverter implements ComparablePrestoValueConverter<Character> {


    @Override
    public Class<Character> getClazz() {
        return Character.class;
    }

    @Override
    public Slice toSlice(@Nonnull Type type, @Nonnull Character value) {
        return Slices.wrappedBuffer(value.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Nonnull
    @Override
    public Character fromSlice(@Nonnull Type type, Slice value) {
        return new String(value.getBytes(),StandardCharsets.UTF_8).charAt(0);
    }

    @Override
    public Character toObject(Object nativeValue) {
        return fromSlice(VarcharType.VARCHAR,(Slice) nativeValue);
    }


}
