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

import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;

public class FloatPrestoValueConverter implements ComparablePrestoValueConverter<Float> {

    public static final long MIN_LONG = Float.valueOf(Float.MIN_VALUE).longValue();
    public static final long MAX_LONG = Float.valueOf(Float.MAX_VALUE).longValue();

    @Override
    public Class<Float> getClazz() {
        return Float.class;
    }

    @Override
    public long toLong(@Nonnull Float value) {
        return floatToRawIntBits(value);
    }

    @Nonnull
    @Override
    public Float fromLong(long value) {
        return intBitsToFloat((int)value);
    }

    @Override
    public long minValueOfIntType() {
        return MIN_LONG;
    }

    @Override
    public long maxValueOfIntType() {
        return MAX_LONG;
    }

    @Override
    public Float toObject(Object nativeValue) {
        return fromLong((Long) nativeValue);
    }

}
