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
import com.cyoda.presto.client.logic.converters.PrestoValueConverterProvider;
import com.cyoda.presto.client.types.DataType;
import com.facebook.presto.common.type.Type;
import io.airlift.slice.Slice;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

public interface SliceCheck<T> {

    default Type getType() {
        return mock(Type.class);
    }
     default void doSliceTest(T expected) {
         PrestoValueConverter<T> converter = getConverter();
         Slice slice = converter.toSlice(expected);
        T actual = converter.fromSlice(slice);
        assertEquals(expected,actual);
    }

    default PrestoValueConverter<T> getConverter() {
        return PrestoValueConverterProvider.getPrestoValueConverter(getDataType());
    }

    DataType getDataType();
}
