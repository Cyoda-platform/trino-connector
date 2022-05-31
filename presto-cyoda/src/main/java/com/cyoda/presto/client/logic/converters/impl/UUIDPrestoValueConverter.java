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
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.VarcharType;
import com.facebook.presto.type.UuidType;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.UUID;

import static com.facebook.presto.common.block.Int128ArrayBlock.INT128_BYTES;
import static io.airlift.slice.SizeOf.SIZE_OF_LONG;
import static io.airlift.slice.Slices.wrappedLongArray;
import static java.lang.String.format;

public class UUIDPrestoValueConverter implements ComparablePrestoValueConverter<UUID> {

    public static final String TYPE_STRING = StandardTypes.VARCHAR; // For Presto
    //public static final String TYPE_STRING = StandardTypes.UUID; // For Trino

    public static final Type TYPE = VarcharType.VARCHAR; // For Presto
    //public static final Type TYPE = UuidType.UUID; // For Trino

    @Override
    public Class<UUID> getClazz() {
        return UUID.class;
    }

    public Slice toSliceForPresto(@Nonnull Type type, @Nonnull UUID value) {
        return Slices.utf8Slice(value.toString());
    }
    public Slice toSliceForTrino(@Nonnull Type type, @Nonnull UUID value) {
        return javaUuidToPrestoUuid(value);
    }
    @Override
    public Slice toSlice(@Nonnull Type type, @Nonnull UUID value) {
        return toSliceForPresto(type, value);
    }

    public UUID fromSliceForPresto(@Nonnull Type type, Slice value) {
        return UUID.fromString(value.toStringUtf8());
    }
    public UUID fromSliceForTrino(@Nonnull Type type, Slice value) {
        return prestoUuidToJavaUuid(value);
    }
    @Nonnull
    @Override
    public UUID fromSlice(@Nonnull Type type, Slice value) {
        return fromSliceForPresto(type, value);
    }

    @Override
    public UUID toObject(Object nativeValue) {
        return fromSlice(VarcharType.VARCHAR,(Slice) nativeValue);
    }



    // This is only useful for Trino. Presto can only handle Strings for UUID
    public static byte[] uuidToBytes(UUID uuid)
    {
        return ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }

    public static Slice javaUuidToPrestoUuid(UUID uuid)
    {
        return wrappedLongArray(
                uuid.getMostSignificantBits(),
                uuid.getLeastSignificantBits());
    }

    public static UUID prestoUuidToJavaUuid(Slice uuid)
    {
        if (uuid.length() != INT128_BYTES) {
            throw new IllegalStateException(format("Expected value to be exactly %d bytes but was %d", INT128_BYTES, uuid.length()));
        }
        return new UUID(
                uuid.getLong(0),
                uuid.getLong(SIZE_OF_LONG));
    }
}
