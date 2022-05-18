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

import com.cyoda.presto.client.logic.ColumnPredicate;
import com.cyoda.presto.client.logic.ColumnPredicateUtils;
import com.cyoda.presto.client.logic.converters.ComparablePrestoValueConverter;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.common.predicate.DiscreteValues;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.VarbinaryType;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public class ByteBufferPrestoValueConverter implements ComparablePrestoValueConverter<ByteBuffer> {
    @Override
    public Class<ByteBuffer> getClazz() {
        return ByteBuffer.class;
    }

    @Override
    public ColumnPredicate<ByteBuffer> newInListPredicate(CyodaColumnHandle columnHandle, DiscreteValues discreteValues) {
        return ColumnPredicateUtils.newInListPredicate(columnHandle, discreteValues, ByteBuffer.class);
    }

    @Override
    public Slice toSlice(@Nonnull Type type, @Nonnull ByteBuffer value) {
        return Slices.wrappedBuffer(value);
    }

    @Nonnull
    @Override
    public ByteBuffer fromSlice(@Nonnull Type type, Slice value) {
        return value.toByteBuffer();
    }

    @Override
    public ByteBuffer toObject(Object nativeValue) {
        return fromSlice(VarbinaryType.VARBINARY,(Slice) nativeValue);
    }
}
