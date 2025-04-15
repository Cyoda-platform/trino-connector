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

package com.cyoda.connector.client.logic.converters.impl;

import com.cyoda.connector.client.logic.converters.structure.SliceUncomparableValueConverter;
import com.cyoda.connector.client.types.DataType;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.Base64;

public class ByteArrayPrestoValueConverter extends SliceUncomparableValueConverter<byte[]> {
    public ByteArrayPrestoValueConverter() {
        super(DataType.BYTE_ARRAY);
    }

    @Override
    public Slice toSlice(@Nonnull byte[] value) {
        return Slices.wrappedBuffer(value);
    }

    @Nonnull
    @Override
    public byte[] fromSlice(Slice value) {
        ByteBuffer byteBuffer = value.toByteBuffer();
        byte[] m = new byte[byteBuffer.remaining()];
        try {
            byteBuffer.get(m);
        } finally {
            byteBuffer.rewind();
        }
        return m;
    }

    @Override
    public byte[] fromOtherCyodaType(Object value, String columnName) {
        return Base64.getDecoder().decode((String) value);
    }

    @Override
    public String stringify(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }
}
