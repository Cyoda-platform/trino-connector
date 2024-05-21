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

import com.cyoda.presto.client.logic.converters.structure.LongDecimalTypeValueConverter;
import com.cyoda.presto.client.types.DataType;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Int128;
import io.trino.spi.type.Type;
import io.airlift.slice.Slice;
import io.trino.spi.type.UuidType;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import java.math.BigInteger;
import java.util.UUID;

public class UUIDPrestoValueConverter extends LongDecimalTypeValueConverter<UUID> {

//    public static final String TYPE_STRING = StandardTypes.VARCHAR; // For Presto
    //public static final String TYPE_STRING = StandardTypes.UUID; // For Trino

//    public static final Type TYPE = VarcharType.VARCHAR; // For Presto
    //public static final Type TYPE = UuidType.UUID; // For Trino

    private static final BigInteger B = BigInteger.ONE.shiftLeft(64); // 2^64
    private static final BigInteger L = BigInteger.valueOf(Long.MAX_VALUE);

    @Inject
    public UUIDPrestoValueConverter() {
        super(DataType.UUID_TYPE);
    }

    @Override
    protected Int128 toInt128(UUID value) {
        return Int128.valueOf(convertToBigInteger(value));
    }
    @Override
    protected UUID fromInt128(Int128 value) {
        return convertFromBigInteger(value.toBigInteger());
    }

    @Override
    public void writeValue(Type type, BlockBuilder builder, @Nonnull UUID value) {
        type.writeSlice(builder, UuidType.javaUuidToTrinoUuid(value));
    }

    @Override
    public UUID fromPrestoNative(Object nativeValue) {
        return UuidType.trinoUuidToJavaUuid((Slice) nativeValue);
    }


    @Override
    public UUID fromOtherCyodaType(Object value, String columnName) {
        return UUID.fromString((String) value);
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
