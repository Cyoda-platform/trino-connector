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

import com.cyoda.presto.client.logic.converters.PrestoValueConverter;

import javax.annotation.Nonnull;

public class BytePrestoValueConverter implements PrestoValueConverter<Byte> {

    @Override
    public long toLong(@Nonnull Byte value) {
        return value.longValue();
    }

    @Nonnull
    @Override
    public Byte fromLong(long value) {
        return (byte)value;
    }

    @Override
    public long minValueOfIntType() {
        return Byte.MIN_VALUE;
    }

    @Override
    public long maxValueOfIntType() {
        return Byte.MAX_VALUE;
    }
}
