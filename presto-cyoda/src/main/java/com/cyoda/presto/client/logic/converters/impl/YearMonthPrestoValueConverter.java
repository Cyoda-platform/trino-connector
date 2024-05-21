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
import com.cyoda.presto.client.types.DataType;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class YearMonthPrestoValueConverter extends LongComparedTypeValueConverter<YearMonth> {

    public static final long MAX_YEAR_MONTH = YearMonth.from(LocalDate.MAX.atStartOfDay()).atEndOfMonth().toEpochDay();
    public static final long MIN_YEAR_MONTH = YearMonth.from(LocalDate.MIN.atStartOfDay()).atEndOfMonth().toEpochDay();

    @Inject
    public YearMonthPrestoValueConverter() {
        super(DataType.YEAR_MONTH);
    }

    @Override
    public long toLong(@Nonnull YearMonth value) {
        return value.atEndOfMonth().toEpochDay();
    }

    @Nonnull
    @Override
    public YearMonth fromLong(long value) {
        return YearMonth.from(LocalDate.ofEpochDay(value));
    }

    @Override
    public long minValueOfIntType() {
        return MIN_YEAR_MONTH;
    }

    @Override
    public long maxValueOfIntType() {
        return MAX_YEAR_MONTH;
    }

    @Override
    public YearMonth fromOtherCyodaType(Object value, String columnName) {
        return YearMonth.parse((String)value, DateTimeFormatter.ISO_DATE);
    }
}
