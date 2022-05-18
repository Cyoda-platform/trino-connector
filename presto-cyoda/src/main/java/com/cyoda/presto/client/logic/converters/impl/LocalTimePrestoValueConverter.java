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

import javax.annotation.Nonnull;
import java.time.LocalTime;

public class LocalTimePrestoValueConverter implements ComparablePrestoValueConverter<LocalTime> {
    @Override
    public Class<LocalTime> getClazz() {
        return LocalTime.class;
    }

    @Override
    public long toLong(@Nonnull LocalTime value) {
        return value.toNanoOfDay();
    }

    @Nonnull
    @Override
    public LocalTime fromLong(long value) {
        return LocalTime.ofNanoOfDay(value);
    }

    @Override
    public long minValueOfIntType() {
        return LocalTime.MIN.toNanoOfDay();
    }

    @Override
    public long maxValueOfIntType() {
        return LocalTime.MAX.toNanoOfDay();
    }

    @Override
    public LocalTime toObject(Object nativeValue) {
        return fromLong((Long) nativeValue);
    }

}
