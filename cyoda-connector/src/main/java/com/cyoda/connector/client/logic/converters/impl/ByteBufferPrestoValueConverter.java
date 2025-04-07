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

import com.cyoda.connector.client.logic.converters.structure.SliceComparableValueConverter;
import com.cyoda.connector.client.types.DataType;
import com.google.common.io.BaseEncoding;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import java.nio.ByteBuffer;

public class ByteBufferPrestoValueConverter extends SliceComparableValueConverter<ByteBuffer> {
    @Inject
    public ByteBufferPrestoValueConverter() {
        super(DataType.BYTE_BUFFER);
    }

    @Override
    public String stringify(ByteBuffer value) {
        byte[] b = new byte[value.remaining()];
        try {
            value.get(b);
        } finally {
            value.rewind();
        }
        return "0" + 'x' + BaseEncoding.base16().encode(b);
    }

    @Override
    public Slice toSlice(@Nonnull ByteBuffer value) {
        return Slices.wrappedHeapBuffer(value);
    }

    @Nonnull
    @Override
    public ByteBuffer fromSlice(Slice value) {
        return value.toByteBuffer();
    }

}
