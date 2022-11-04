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
import io.airlift.slice.Slices;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.math.BigDecimal;

public class BigDecimalPrestoValueConverter extends BigDecimalTypeValueConverter<BigDecimal> {

    @Inject
    public BigDecimalPrestoValueConverter() {
        super(DataType.BIG_DECIMAL);
    }

    @Override
    protected BigDecimal toBigDecimal(BigDecimal value) {
        return value;
    }

    @Override
    protected BigDecimal fromBigDecimal(BigDecimal value) {
        return value;
    }

    @Override
    public Slice toSlice(@Nonnull BigDecimal value) {
        return Slices.utf8Slice(value.toString());
    }

    @Nonnull
    @Override
    public BigDecimal fromSlice(Slice value) {
        return new BigDecimal(value.toStringUtf8());
    }

    @Override
    public BigDecimal fromOtherCyodaType(Object value, String columnName) {
        return BigDecimal.valueOf(((Number) value).doubleValue());
    }
}
