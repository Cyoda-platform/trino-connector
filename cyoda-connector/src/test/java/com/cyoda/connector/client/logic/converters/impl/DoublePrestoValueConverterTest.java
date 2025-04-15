/*
 * Copyright (C) 2025 Cyoda Ltd.
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

import com.cyoda.connector.client.types.DataType;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class DoublePrestoValueConverterTest {

    private DoublePrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new DoublePrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.DOUBLE);
        assertEquals(converter.getClazz(), Double.class);
    }

    @Test
    public void testToLong() {
        Double testValue = 123.456;
        Long result = converter.toLong(testValue);
        assertEquals(result.longValue(), Double.doubleToLongBits(123.456));
    }

    @Test
    public void testFromLong() {
        Long testValue = Double.doubleToLongBits(123.456);
        Double result = converter.fromLong(testValue);
        assertEquals(result, 123.456);
    }

    @Test
    public void testWriteValue() {
        Double testValue = 123.456;
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);

        converter.writeValue(type, blockBuilder, testValue);

        // Note: This converter overrides writeValue to use writeDouble directly
        verify(type).writeDouble(blockBuilder, 123.456);
    }

    @Test
    public void testFromOtherCyodaType() {
        // Test with Integer
        Integer intValue = 123;
        Double intResult = converter.fromOtherCyodaType(intValue, "testColumn");
        assertEquals(intResult, 123.0);

        // Test with Float
        Float floatValue = 123.456f;
        Double floatResult = converter.fromOtherCyodaType(floatValue, "testColumn");
        assertEquals(floatResult, 123.456, 0.0001); // Using delta for float comparison
    }

    @Test
    public void testFromPrestoNative() {
        Double nativeValue = 123.456;
        Double result = converter.fromPrestoNative(nativeValue);
        assertEquals(result, 123.456);
    }

    @Test
    public void testStringify() {
        Double testValue = 123.456;
        String result = converter.stringify(testValue);
        assertEquals(result, "123.456");
    }
}
