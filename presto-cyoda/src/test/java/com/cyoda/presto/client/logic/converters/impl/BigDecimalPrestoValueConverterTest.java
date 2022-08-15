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

import com.cyoda.presto.client.types.DataType;
import com.facebook.presto.common.type.DecimalType;
import com.facebook.presto.common.type.Type;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class BigDecimalPrestoValueConverterTest implements SliceCheck<BigDecimal> {

    DecimalType type = mock(DecimalType.class);

    @Override
    public Type getType() {
        return type;
    }

    @Test
    public void testToFromSlice() {
        assertEquals(getConverter().getClass(),BigDecimalPrestoValueConverter.class);
        for (int i = 0; i < 100; i++) {
            BigDecimal expected = BigDecimal.valueOf(Math.random()*10000.0);
            when(type.getPrecision()).thenReturn(expected.precision());
            when(type.getScale()).thenReturn(expected.scale());
            doSliceTest(expected);
        }
    }

    @Override
    public DataType getDataType() {
        return DataType.BIG_DECIMAL;
    }
}