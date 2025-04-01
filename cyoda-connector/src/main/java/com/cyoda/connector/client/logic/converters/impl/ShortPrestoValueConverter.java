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

public class ShortPrestoValueConverter extends LongComparedTypeValueConverter<Short> {

    @Inject
    public ShortPrestoValueConverter() {
        super(DataType.SHORT);
    }

    @Override
    public Long toLong(@Nonnull Short value) {
        return value.longValue();
    }

    @Nonnull
    @Override
    public Short fromLong(Long value) {
        return value.shortValue();
    }

    @Override
    public long minValueOfIntType() {
        return Short.MIN_VALUE;
    }

    @Override
    public long maxValueOfIntType() {
        return Short.MAX_VALUE;
    }


    @Override
    public Short fromOtherCyodaType(Object value, String columnName) {
        return ((Integer)value).shortValue();
    }
}
