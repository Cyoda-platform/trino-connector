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
import com.cyoda.connector.client.logic.converters.structure.TemporalTransformer;
import com.cyoda.connector.client.types.DataType;
import io.trino.spi.type.DateTimeEncoding;
import io.trino.spi.type.TimeZoneKey;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ZonedDateTimePrestoValueConverter extends LongWrittenTypeValueConverter<ZonedDateTime> {
    private final TemporalTransformer<ZonedDateTime> temporalTransformer = new TemporalTransformer<ZonedDateTime>(
            year -> year.atMonth(12).atEndOfMonth().atStartOfDay().atZone(ZoneOffset.UTC), yearMonth -> yearMonth.atEndOfMonth().atStartOfDay().atZone(ZoneOffset.UTC), localTime -> localTime.atDate(LocalDate.EPOCH).atZone(ZoneOffset.UTC), localDate -> localDate.atStartOfDay().atZone(ZoneOffset.UTC), null, zonedDateTime -> zonedDateTime
    );

    @Inject
    public ZonedDateTimePrestoValueConverter() {
        super(DataType.ZONED_DATE_TIME);
    }

    @Override
    public Long toLong(@Nonnull ZonedDateTime value) {
        return DateTimeEncoding.packDateTimeWithZone(
                value.toInstant().toEpochMilli(),
                value.getZone().getId()
        );
    }

    @Nonnull
    @Override
    public ZonedDateTime fromLong(Long value) {
        TimeZoneKey timeZoneKey = DateTimeEncoding.unpackZoneKey(value);
        long millisUtc = DateTimeEncoding.unpackMillisUtc(value);
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millisUtc), ZoneId.of(timeZoneKey.getId()));
    }

    @Override
    public ZonedDateTime fromOtherCyodaType(Object value, String columnName) {
        return temporalTransformer.parse(value, columnName, getClazz());
    }
}
