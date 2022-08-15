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

import com.cyoda.presto.client.logic.converters.structure.LongTypeValueConverter;
import com.cyoda.presto.client.types.DataType;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.time.LocalDate;

public class LocalDatePrestoValueConverter extends LongTypeValueConverter<LocalDate> {

    @Inject
    public LocalDatePrestoValueConverter() {
        super(DataType.LOCAL_DATE);
    }

    @Override
    public long toLong(@Nonnull LocalDate value) {
        return value.toEpochDay();
    }

    @Nonnull
    @Override
    public LocalDate fromLong(long value) {
        return LocalDate.ofEpochDay(value);
    }

    @Override
    public long minValueOfIntType() {
        return LocalDate.MIN.toEpochDay();
    }

    @Override
    public long maxValueOfIntType() {
        return LocalDate.MAX.toEpochDay();
    }

    @Override
    public LocalDate toObject(Object nativeValue) {
        return fromLong((Long) nativeValue);
    }

}
