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
import javax.inject.Inject;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LongPrestoValueConverter extends LongComparedTypeValueConverter<Long> {

    protected static final BigInteger BIG_INTEGER_MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
    protected static final BigInteger BIG_INTEGER_MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);

    private static final List<Class<? extends Number>> SUPPORTED_CONVERSION_TYPES = Arrays.asList(
            Byte.class, Short.class, Integer.class, AtomicInteger.class, AtomicLong.class
    );

    @Inject
    public LongPrestoValueConverter() {
        super(DataType.LONG);
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
    public String stringify(Long value) {
        return value.toString();
    }


    /**
     * Implement support for integer types, i.e. whole numbers and booleans
     * Numbers that are decimals are not supported and will call the superclass method.
     * BigIntegers that would lead to truncation when converted to Long are not supported.
     * Booleans are converted to 0/1 for false/true respectively
     *
     * @param value to convert
     * @param columnName not used directly here
     * @return the converted value
     */
    @Override
    public Long fromOtherCyodaType(Object value, String columnName) {
        if ( SUPPORTED_CONVERSION_TYPES.contains(value.getClass()) ) {
            return ((Number) value).longValue();
        }
        if (value instanceof BigInteger bigInt) {
            if ( BIG_INTEGER_MAX_LONG.compareTo(bigInt) >= 0 && BIG_INTEGER_MIN_LONG.compareTo(bigInt) <= 0 ) {
                return ((Number) value).longValue();
            } else {
                throw new UnsupportedOperationException(String.format("Error with field \"%s\": " +
                                "Conversion operation from %s to %s is not supported because value is out of range",
                        columnName, value.getClass(), getClazz()));
            }
        }
        if ( value instanceof Boolean) {
            return ((Boolean) value) ? 1L : 0L;
        }
        return super.fromOtherCyodaType(value, columnName);
    }

}
