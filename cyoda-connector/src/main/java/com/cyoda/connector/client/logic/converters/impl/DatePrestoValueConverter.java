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

import com.cyoda.connector.client.logic.converters.structure.TimestampTypeValueConverter;
import com.cyoda.connector.client.types.DataType;

import jakarta.inject.Inject;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DatePrestoValueConverter extends TimestampTypeValueConverter<Date> {

    @Inject
    public DatePrestoValueConverter() {
        super(DataType.DATE);
    }

    @Override
    public Date fromOtherCyodaType(Object value, String columnName) {
        LocalDateTime localDateTime = LocalDateTime.parse((String) value, DateTimeFormatter.ISO_DATE_TIME);
        return Timestamp.valueOf(localDateTime);
    }

    @Override
    public String toStringFromNative(Object nativeValue) {
        return fromPrestoNative(nativeValue).toInstant().toString();
    }

    @Override
    protected Instant toInstant(Date value) {
        return value.toInstant();
    }

    @Override
    protected Date fromInstant(Instant value) {
        return Date.from(value);
    }
}
