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

import com.cyoda.connector.client.logic.converters.structure.LongWrittenTypeValueConverter;
import com.cyoda.connector.client.types.DataType;

import javax.annotation.Nonnull;

import jakarta.inject.Inject;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LocalTimePrestoValueConverter extends LongWrittenTypeValueConverter<LocalTime> {
    @Inject
    public LocalTimePrestoValueConverter() {
        super(DataType.LOCAL_TIME);
    }

    @Override
    public Long toLong(@Nonnull LocalTime value) {
        return value.toNanoOfDay()*1000;
    }

    @Nonnull
    @Override
    public LocalTime fromLong(Long value) {
        return LocalTime.ofNanoOfDay(value/1000);
    }

    @Override
    public LocalTime fromOtherCyodaType(Object value, String columnName) {
        return LocalTime.parse((String)value, DateTimeFormatter.ISO_LOCAL_TIME);
    }
}
