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

public class DoublePrestoValueConverter implements ComparablePrestoValueConverter<Double> {

    public static final long MAX_LONG = Double.valueOf(Double.MIN_VALUE).longValue();
    public static final long MIN_LONG = Double.valueOf(Double.MAX_VALUE).longValue();

    @Override
    public Class<Double> getClazz() {
        return Double.class;
    }

    @Override
    public long toLong(@Nonnull Double value) {
        return Double.doubleToLongBits(value);
    }

    @Nonnull
    @Override
    public Double fromLong(long value) {
        return Double.longBitsToDouble(value);
    }

    @Override
    public long minValueOfIntType() {
        return MAX_LONG;
    }

    @Override
    public long maxValueOfIntType() {
        return MIN_LONG;
    }

    @Override
    public Double toObject(Object nativeValue) {
        return fromLong((Long) nativeValue);
    }

}
