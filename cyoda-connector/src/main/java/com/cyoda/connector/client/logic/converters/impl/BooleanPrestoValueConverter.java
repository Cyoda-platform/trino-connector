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

import com.cyoda.connector.client.logic.converters.structure.ComparableValueConverter;
import com.cyoda.connector.client.types.DataType;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.block.ByteArrayBlock;
import io.trino.spi.type.Type;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

public class BooleanPrestoValueConverter extends ComparableValueConverter<Boolean> {

    @Inject
    public BooleanPrestoValueConverter() {
        super(DataType.BOOLEAN);
    }

    @Override
    public void writeValue(Type type, BlockBuilder builder, @Nonnull Boolean value) {
        type.writeBoolean(builder, value);
    }

    @Override
    public Boolean fromPrestoNative(Object nativeValue) {
        return (Boolean) nativeValue;
    }

    @Override
    public List<Boolean> blockToNativeList(Object nativeBlock, Type trinoType) {
        ByteArrayBlock block = (ByteArrayBlock) nativeBlock;
        ArrayList<Boolean> result = new ArrayList<>();
        for (int i = 0; i < block.getPositionCount(); i++) {
            result.add(block.getByte(i) == 1);
        }
        return result;
    }
}
