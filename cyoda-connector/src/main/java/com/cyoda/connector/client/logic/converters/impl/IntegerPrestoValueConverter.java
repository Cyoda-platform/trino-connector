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

import com.cyoda.connector.client.logic.converters.structure.LongComparedTypeValueConverter;
import com.cyoda.connector.client.types.DataType;

import javax.annotation.Nonnull;

import io.trino.spi.block.IntArrayBlock;
import io.trino.spi.type.Type;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

public class IntegerPrestoValueConverter extends LongComparedTypeValueConverter<Integer> {

    @Inject
    public IntegerPrestoValueConverter() {
        super(DataType.INTEGER);
    }

    @Override
    public Long toLong(@Nonnull Integer value) {
        return value.longValue();
    }

    @Nonnull
    @Override
    public Integer fromLong(Long value) {
        return value.intValue();
    }

    @Override
    public long minValueOfIntType() {
        return Integer.MIN_VALUE;
    }

    @Override
    public long maxValueOfIntType() {
        return Integer.MAX_VALUE;
    }


    @Override
    public List<Integer> blockToNativeList(Object nativeBlock, Type trinoType) {
        IntArrayBlock intArrayBlock = (IntArrayBlock) nativeBlock;
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < intArrayBlock.getPositionCount(); i++) {
            list.add(intArrayBlock.getInt(i));
        }
        return list;
    }
}
