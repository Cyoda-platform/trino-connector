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
import com.cyoda.presto.client.types.impl.ByteArrayDataType;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.VarbinaryType;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public class ByteArrayPrestoValueConverter implements PrestoValueConverter<byte[]> {
    @Override
    public Slice toSlice(@Nonnull Type type, @Nonnull byte[] value) {
        return Slices.wrappedBuffer(ByteBuffer.wrap(value));
    }

    @Nonnull
    @Override
    public byte[] fromSlice(@Nonnull Type type, Slice value) {
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
    public byte[] toObject(Object nativeValue) {
        return fromSlice(VarbinaryType.VARBINARY, (Slice) nativeValue);
    }
}
