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

public class LongPrestoValueConverter implements ComparablePrestoValueConverter<Long> {

    @Override
    public Class<Long> getClazz() {
        return Long.class;
    }

    @Override
    public long toLong(@Nonnull Long value) {
        return value;
    }

    @Nonnull
    @Override
    public Long fromLong(long value) {
        return (long)value;
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
    public Long toObject(Object nativeValue) {
        return fromLong((Long) nativeValue);
    }

}
