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

import com.cyoda.connector.client.logic.converters.structure.IntWrittenTypeValueConverter;
import com.cyoda.connector.client.logic.converters.structure.LongWrittenTypeValueConverter;
import com.cyoda.connector.client.types.DataType;

import javax.annotation.Nonnull;

import io.trino.spi.block.IntArrayBlock;
import io.trino.spi.type.Type;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class IntegerPrestoValueConverter extends IntWrittenTypeValueConverter<Integer> {

    @Inject
    public IntegerPrestoValueConverter() {
        super(DataType.INTEGER);
    }


    @Override
    public Integer toInt(@NotNull Integer value) {
        return value;
    }

    @Override
    public @NotNull Integer fromInt(Integer value) {
        return value;
    }
}
