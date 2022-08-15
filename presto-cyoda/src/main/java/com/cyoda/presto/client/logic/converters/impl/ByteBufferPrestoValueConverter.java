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
import com.cyoda.presto.client.logic.converters.structure.SliceComparableValueConverter;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.client.types.DataTypeValue;
import com.cyoda.presto.handles.CyodaColumnHandle;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

public class ByteBufferPrestoValueConverter extends SliceComparableValueConverter<ByteBuffer> {
    @Inject
    public ByteBufferPrestoValueConverter() {
        super(DataType.BYTE_BUFFER);
    }

    @Override
    protected ColumnPredicate<ByteBuffer> newComparisonPredicate(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, ByteBuffer value) {
        byte[] arrayValue = new byte[value.remaining()];
        try {
            value.get(arrayValue);
        } finally {
            value.rewind();
        }
        if (op == ColumnPredicate.ComparisonOp.LESS_EQUAL) {
            arrayValue = Arrays.copyOf(arrayValue, arrayValue.length + 1);
            op = ColumnPredicate.ComparisonOp.LESS;
        } else if (op == ColumnPredicate.ComparisonOp.GREATER) {
            arrayValue = Arrays.copyOf(arrayValue, arrayValue.length + 1);
            op = ColumnPredicate.ComparisonOp.GREATER_EQUAL;
        }

        DataTypeValue<ByteBuffer> wrapped = DataTypeValue.of(ByteBuffer.wrap(arrayValue));

        switch (op) {
            case GREATER_EQUAL:
                if (arrayValue.length == 0) {
                    return notNullPredicate(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, wrapped, null);
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, wrapped, null);
            case LESS:
                if (arrayValue.length == 0) {
                    return nonePredicate(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, null, wrapped);
            default:
                throw unsupportedComparison(column, op);
        }
    }

    @Override
    public String stringify(ByteBuffer value) {
        byte[] b = new byte[value.remaining()];
        try {
            value.get(b);
        } finally {
            value.rewind();
        }
        return Base64.getEncoder().encodeToString(b);
    }

    @Override
    public Slice toSlice(@Nonnull ByteBuffer value) {
        return Slices.wrappedBuffer(value);
    }

    @Nonnull
    @Override
    public ByteBuffer fromSlice(Slice value) {
        return value.toByteBuffer();
    }

    @Override
    public ByteBuffer toObject(Object nativeValue) {
        return fromSlice((Slice) nativeValue);
    }
}
