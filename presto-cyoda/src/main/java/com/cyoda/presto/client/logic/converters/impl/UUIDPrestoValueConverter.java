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

import com.cyoda.presto.client.logic.converters.structure.BigDecimalTypeValueConverter;
import com.cyoda.presto.client.types.DataType;
import io.trino.spi.type.StandardTypes;
import io.trino.spi.type.Type;
import io.trino.spi.type.VarcharType;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;

import static io.trino.spi.block.Int128ArrayBlock.INT128_BYTES;
import static io.airlift.slice.SizeOf.SIZE_OF_LONG;
import static io.airlift.slice.Slices.wrappedLongArray;
import static java.lang.String.format;

public class UUIDPrestoValueConverter extends BigDecimalTypeValueConverter<UUID> {

    public static final String TYPE_STRING = StandardTypes.VARCHAR; // For Presto
    //public static final String TYPE_STRING = StandardTypes.UUID; // For Trino

    public static final Type TYPE = VarcharType.VARCHAR; // For Presto
    //public static final Type TYPE = UuidType.UUID; // For Trino

    private static final BigInteger B = BigInteger.ONE.shiftLeft(64); // 2^64
    private static final BigInteger L = BigInteger.valueOf(Long.MAX_VALUE);

    @Inject
    public UUIDPrestoValueConverter() {
        super(DataType.UUID_TYPE);
    }

    @Override
    protected BigDecimal toBigDecimal(UUID value) {
        return new BigDecimal(convertToBigInteger(value));
    }
    @Override
    protected UUID fromBigDecimal(BigDecimal value) {
        return convertFromBigInteger(value.toBigIntegerExact());
    }

    public Slice toSliceForPresto(@Nonnull UUID value) {
        return Slices.utf8Slice(value.toString());
    }
    public Slice toSliceForTrino(@Nonnull UUID value) {
        return javaUuidToPrestoUuid(value);
    }

    @Override
    public Slice toSlice(@Nonnull UUID value) {
        return toSliceForPresto(value);
    }
    public UUID fromSliceForPresto(Slice value) {
        return UUID.fromString(value.toStringUtf8());
    }
    public UUID fromSliceForTrino(Slice value) {
        return prestoUuidToJavaUuid(value);
    }

    @Nonnull
    @Override
    public UUID fromSlice(Slice value) {
        return fromSliceForPresto(value);
    }

    @Override
    public UUID fromOtherCyodaType(Object value, String columnName) {
        return UUID.fromString((String) value);
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

    public static BigInteger convertToBigInteger(UUID id)
    {
        BigInteger lo = BigInteger.valueOf(id.getLeastSignificantBits());
        BigInteger hi = BigInteger.valueOf(id.getMostSignificantBits());

        // If any of lo/hi parts is negative interpret as unsigned

        if (hi.signum() < 0)
            hi = hi.add(B);

        if (lo.signum() < 0)
            lo = lo.add(B);

        return lo.add(hi.multiply(B));
    }

    public static UUID convertFromBigInteger(BigInteger x)
    {
        BigInteger[] parts = x.divideAndRemainder(B);
        BigInteger hi = parts[0];
        BigInteger lo = parts[1];

        if (L.compareTo(lo) < 0)
            lo = lo.subtract(B);

        if (L.compareTo(hi) < 0)
            hi = hi.subtract(B);

        return new UUID(hi.longValueExact(), lo.longValueExact());
    }
}
