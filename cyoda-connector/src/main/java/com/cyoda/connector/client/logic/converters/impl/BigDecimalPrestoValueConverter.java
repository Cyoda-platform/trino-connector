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
import io.trino.spi.type.DecimalType;
import io.trino.spi.type.Decimals;
import io.trino.spi.type.Int128;
import io.trino.spi.type.TypeSignatureParameter;

import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.MathContext;

import static java.math.RoundingMode.UNNECESSARY;

public class BigDecimalPrestoValueConverter extends LongDecimalTypeValueConverter<BigDecimal> {

    //TODO this datatype can be improved whenever following ticked would be resolved: https://github.com/trinodb/trino/issues/2274
    public static final int SCALE = 18;
    public static final int PRECISION = Decimals.MAX_PRECISION;
    private static final DecimalType DECIMAL_TYPE = DecimalType.createDecimalType(PRECISION, SCALE);
    public static final TypeSignatureParameter P_SC = TypeSignatureParameter.numericParameter(SCALE);
    public static final TypeSignatureParameter P_PR = TypeSignatureParameter.numericParameter(PRECISION);

    @Inject
    public BigDecimalPrestoValueConverter() {
        super(DataType.BIG_DECIMAL);
    }

    @Override
    protected Int128 toInt128(BigDecimal value) {
        BigDecimal trimmedValue = value.stripTrailingZeros();
        if (trimmedValue.scale() > SCALE){
            throw new IllegalArgumentException(String.format("Value %s of a BigDecimal field has higher scale (%s) than maximum of %s",
                    trimmedValue, trimmedValue.scale(), SCALE));
        }
        BigDecimal rescaled = trimmedValue.setScale(SCALE, UNNECESSARY);
        return Int128.valueOf(rescaled.unscaledValue());
    }

    @Override
    protected BigDecimal fromInt128(Int128 value) {
        return new BigDecimal(value.toBigInteger(), DECIMAL_TYPE.getScale(), new MathContext(DECIMAL_TYPE.getPrecision()));
    }

    @Override
    public String stringify(BigDecimal value) {
        return value.toString();
    }
}
