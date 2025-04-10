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

import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class FloatPrestoValueConverterTest {

    private FloatPrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new FloatPrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.FLOAT);
        assertEquals(converter.getClazz(), Float.class);
    }

    @Test
    public void testToInt() {
        Float testValue = 123.456f;
        Integer result = converter.toInt(testValue);
        assertEquals(result, Integer.valueOf(floatToRawIntBits(123.456f)));
    }

    @Test
    public void testFromInt() {
        Integer testValue = floatToRawIntBits(123.456f);
        Float result = converter.fromInt(testValue);
        assertEquals(result, 123.456f);
    }

    @Test
    public void testWriteValue() {
        Float testValue = 123.456f;
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeLong(blockBuilder, (long) floatToRawIntBits(123.456f));
    }

    @Test
    public void testFromOtherCyodaType() {
        // Test with Integer
        Integer intValue = 123;
        Float intResult = converter.fromOtherCyodaType(intValue, "testColumn");
        assertEquals(intResult, 123.0f);

        // Test with Double
        Double doubleValue = 123.456;
        Float doubleResult = converter.fromOtherCyodaType(doubleValue, "testColumn");
        assertEquals(doubleResult, 123.456f, 0.0001f); // Using delta for float comparison
    }

    @Test
    public void testFromPrestoNative() {
        Float nativeValue = 123.456f;
        Float result = converter.fromPrestoNative(nativeValue);
        assertEquals(result, 123.456f);
    }

    @Test
    public void testStringify() {
        Float testValue = 123.456f;
        String result = converter.stringify(testValue);
        assertEquals(result, "123.456");
    }
}
