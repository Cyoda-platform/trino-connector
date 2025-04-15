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

import com.cyoda.connector.CyodaCachedPageSource;
import com.cyoda.connector.client.logic.converters.structure.LongDecimalTypeValueConverter;
import com.cyoda.connector.client.logic.converters.structure.SliceComparableValueConverter;
import com.cyoda.connector.client.types.DataType;
import com.cyoda.connector.logging.SupplierLogger;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.block.Int128ArrayBlock;
import io.trino.spi.type.Int128;
import io.trino.spi.type.Type;
import io.airlift.slice.Slice;
import io.trino.spi.type.UuidType;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.lang.Long.reverseBytes;

public class UUIDPrestoValueConverter extends SliceComparableValueConverter<UUID> {

    private static final SupplierLogger LOG = SupplierLogger.get(UUIDPrestoValueConverter.class);

    private static final BigInteger B = BigInteger.ONE.shiftLeft(64); // 2^64
    private static final BigInteger HALF_B = BigInteger.ONE.shiftLeft(63); // 2^63

    @Inject
    public UUIDPrestoValueConverter() {
        super(DataType.UUID_TYPE);
    }

    @Override
    public Slice toSlice(@NotNull UUID value) {
        return UuidType.javaUuidToTrinoUuid(value);
    }

    @Override
    public @NotNull UUID fromSlice(Slice value) {
        return UuidType.trinoUuidToJavaUuid(value);
    }

    @Override
    public List<UUID> blockToNativeList(Object nativeBlock, Type trinoType) {
        Int128ArrayBlock block = (Int128ArrayBlock) nativeBlock;
        ArrayList<UUID> res = new ArrayList<>();
        for (int i = 0; i < block.getPositionCount(); i++) {
            Int128 int128 = block.getInt128(i);
            res.add(new UUID(reverseBytes(int128.getHigh()), reverseBytes(int128.getLow())));
        }
        return res;
    }

    @Override
    public UUID fromPrestoNative(Object nativeValue) {
        return UuidType.trinoUuidToJavaUuid((Slice) nativeValue);
    }


    @Override
    public UUID fromOtherCyodaType(Object value, String columnName) {
        return UUID.fromString((String) value);
    }


}
