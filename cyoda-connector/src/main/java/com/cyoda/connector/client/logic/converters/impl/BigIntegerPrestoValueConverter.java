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

import com.cyoda.connector.client.logic.converters.structure.LongDecimalTypeValueConverter;
import com.cyoda.connector.client.types.DataType;
import io.trino.spi.type.Int128;

import jakarta.inject.Inject;
import java.math.BigInteger;

public class BigIntegerPrestoValueConverter extends LongDecimalTypeValueConverter<BigInteger> {

    @Inject
    public BigIntegerPrestoValueConverter() {
        super(DataType.BIG_INTEGER);
    }

    @Override
    protected Int128 toInt128(BigInteger value) {
        return Int128.valueOf(value);
    }

    @Override
    protected BigInteger fromInt128(Int128 value) {
        return value.toBigInteger();
    }

}
