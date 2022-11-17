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

import com.cyoda.presto.client.logic.converters.structure.LongComparedTypeValueConverter;
import com.cyoda.presto.client.logic.converters.structure.TimestampTypeValueConverter;
import com.cyoda.presto.client.types.DataType;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class LocalDateTimePrestoValueConverter extends TimestampTypeValueConverter<LocalDateTime> {

    public static final ZoneId UTC = ZoneId.of("UTC");

    @Inject
    public LocalDateTimePrestoValueConverter() {
        super(DataType.LOCAL_DATE_TIME);
    }

    @Override
    protected Instant toInstant(LocalDateTime value) {
        return value.toInstant(ZoneOffset.UTC);
    }

    @Override
    protected LocalDateTime fromInstant(Instant value) {
        return LocalDateTime.ofInstant(value, UTC);
    }


    @Override
    public LocalDateTime fromOtherCyodaType(Object value, String columnName) {
        return LocalDateTime.parse((String) value, DateTimeFormatter.ISO_DATE_TIME);
    }
}
