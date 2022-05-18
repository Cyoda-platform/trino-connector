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

import com.cyoda.presto.client.logic.converters.ComparablePrestoValueConverter;
import com.facebook.presto.common.type.DateTimeEncoding;
import com.facebook.presto.common.type.TimeZoneKey;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ZonedDateTimePrestoValueConverter implements ComparablePrestoValueConverter<ZonedDateTime> {

    @Override
    public Class<ZonedDateTime> getClazz() {
        return ZonedDateTime.class;
    }

    @Override
    public long toLong(@Nonnull ZonedDateTime value) {
        return DateTimeEncoding.packDateTimeWithZone(
                value.toInstant().toEpochMilli(),
                value.getZone().getId()
        );
    }

    @Nonnull
    @Override
    public ZonedDateTime fromLong(long value) {
        TimeZoneKey timeZoneKey = DateTimeEncoding.unpackZoneKey(value);
        long millisUtc = DateTimeEncoding.unpackMillisUtc(value);
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millisUtc), ZoneId.of(timeZoneKey.getId()));
    }

    @Override
    public long minValueOfIntType() {
        return Long.MIN_VALUE;
    }

    @Override
    public long maxValueOfIntType() {
        return Long.MAX_VALUE;
    }

    @Override
    public ZonedDateTime toObject(Object nativeValue) {
        return fromLong((Long) nativeValue);
    }

}
