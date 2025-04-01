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

import com.cyoda.connector.client.logic.converters.structure.LongComparedTypeValueConverter;
import com.cyoda.connector.client.types.DataType;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import java.time.Year;

public class YearPrestoValueConverter extends LongComparedTypeValueConverter<Year> {
    @Inject
    public YearPrestoValueConverter() {
        super(DataType.YEAR);
    }

    @Override
    public Long toLong(@Nonnull Year value) {
        return (long) value.getValue();
    }

    @Nonnull
    @Override
    public Year fromLong(Long value) {
        return Year.of(value.intValue());
    }

    @Override
    public long minValueOfIntType() {
        return Year.MIN_VALUE;
    }

    @Override
    public long maxValueOfIntType() {
        return Year.MAX_VALUE;
    }

    @Override
    public Year fromOtherCyodaType(Object value, String columnName) {
        return Year.of(Integer.parseInt((String) value));
    }
}
