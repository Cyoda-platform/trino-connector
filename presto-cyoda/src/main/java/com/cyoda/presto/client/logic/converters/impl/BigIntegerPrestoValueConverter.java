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

import com.cyoda.presto.client.logic.converters.structure.BigDecimalTypeValueConverter;
import com.cyoda.presto.client.types.DataType;
import io.airlift.slice.Slice;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.BigInteger;

public class BigIntegerPrestoValueConverter extends BigDecimalTypeValueConverter<BigInteger> {

    @Inject
    public BigIntegerPrestoValueConverter() {
        super(DataType.BIG_INTEGER);
    }

    @Override
    protected BigDecimal toBigDecimal(BigInteger value) {
        return new BigDecimal(value);
    }

    @Override
    protected BigInteger fromBigDecimal(BigDecimal value) {
        return value.toBigIntegerExact();
    }

    @Override
    public Slice toSlice(@Nonnull BigInteger value) {
        return (Slice) encodeDecimal(value);
    }

    @Nonnull
    @Override
    public BigInteger fromSlice(Slice value) {
        return decodeDecimal(value);
    }

    @Override
    public boolean areConsecutive(BigInteger a, BigInteger b) {
        return a.add(BigInteger.ONE).equals(b);
    }
}
