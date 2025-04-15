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

import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDatePrestoValueConverter extends IntWrittenTypeValueConverter<LocalDate> {

    @Inject
    public LocalDatePrestoValueConverter() {
        super(DataType.LOCAL_DATE);
    }

    @Override
    public LocalDate fromOtherCyodaType(Object value, String columnName) {
        return LocalDate.parse((String) value, DateTimeFormatter.ISO_DATE);
    }

    @Override
    public Integer toInt(@NotNull LocalDate value) {
        return (int) value.toEpochDay();
    }

    @Override
    public @NotNull LocalDate fromInt(Integer value) {
        return LocalDate.ofEpochDay(value);
    }
}
