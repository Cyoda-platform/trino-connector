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

import com.cyoda.presto.client.logic.converters.structure.StringTypeValueConverter;
import com.cyoda.presto.client.types.DataType;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;

public class CharacterPrestoValueConverter extends StringTypeValueConverter<Character> {

    @Inject
    public CharacterPrestoValueConverter() {
        super(DataType.CHARACTER);
    }

    @Override
    protected Character fromStr(String value) {
        char[] chars = value.toCharArray();
        if ( chars.length != 1 ) throw new IllegalStateException("Corrupted converted predicate from String to Character \"" + value + "\"");
        return chars[0];
    }

    @Override
    protected String toStr(Character value) {
        return String.valueOf(value);
    }

    @Override
    public Slice toSlice(@Nonnull Character value) {
        return Slices.wrappedBuffer(value.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Nonnull
    @Override
    public Character fromSlice(Slice value) {
        return new String(value.getBytes(),StandardCharsets.UTF_8).charAt(0);
    }

}
