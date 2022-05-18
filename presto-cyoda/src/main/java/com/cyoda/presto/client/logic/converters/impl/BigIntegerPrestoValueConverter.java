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
import com.facebook.presto.common.type.BigintType;
import com.facebook.presto.common.type.Type;
import io.airlift.slice.Slice;

import javax.annotation.Nonnull;
import java.math.BigInteger;

import static com.facebook.presto.common.type.Decimals.decodeUnscaledValue;
import static com.facebook.presto.common.type.Decimals.encodeUnscaledValue;

public class BigIntegerPrestoValueConverter implements ComparablePrestoValueConverter<BigInteger> {

    @Override
    public Slice toSlice(@Nonnull Type type, @Nonnull BigInteger value) {
        // Taken from com.facebook.presto.hive.functions.type.DecimalUtils
        return encodeUnscaledValue(value);
    }

    @Nonnull
    @Override
    public BigInteger fromSlice(@Nonnull Type type, Slice value) {
        return decodeUnscaledValue(value);
    }

    @Override
    public BigInteger toObject(Object nativeValue) {
        return fromSlice(BigintType.BIGINT,(Slice) nativeValue);
    }

    @Override
    public Class<BigInteger> getClazz() {
        return BigInteger.class;
    }
}
