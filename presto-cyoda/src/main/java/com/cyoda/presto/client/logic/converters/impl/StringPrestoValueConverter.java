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

public class StringPrestoValueConverter extends StringTypeValueConverter<String> {

    @Inject
    public StringPrestoValueConverter() {
        super(DataType.STRING);
    }

    @Override
    protected String fromStr(String value) {
        return value;
    }

    @Override
    protected String toStr(String value) {
        return value;
    }

    @Override
    public Slice toSlice(@Nonnull String value) {
        return Slices.utf8Slice(value);
    }

    @Nonnull
    @Override
    public String fromSlice(Slice value) {
        return value.toStringUtf8();
    }

    @Override
    public boolean areConsecutive(String a, String b) {
        if (a.length() + 1 != b.length() || b.charAt(b.length() - 1) != 0) {
            return false;
        }
        return a.equals(b.substring(0, b.length() - 1));
    }

    @Override
    public String fromOtherCyodaType(Object value, String columnName) {
    //a reasonable shortcut to make this type a failsafe for objects
        return value.toString();
    }
}
