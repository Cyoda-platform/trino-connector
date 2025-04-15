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

public class BytePrestoValueConverterTest {

    private BytePrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new BytePrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.BYTE);
        assertEquals(converter.getClazz(), Byte.class);
    }

    @Test
    public void testToLong() {
        Byte testValue = (byte) 123;
        Long result = converter.toLong(testValue);
        assertEquals(result, Long.valueOf(123L));
    }

    @Test
    public void testFromLong() {
        Long testValue = 123L;
        Byte result = converter.fromLong(testValue);
        assertEquals(result, Byte.valueOf((byte) 123));
    }

    @Test
    public void testWriteValue() {
        Byte testValue = (byte) 123;
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeLong(blockBuilder, 123L);
    }

    @Test
    public void testFromOtherCyodaType() {
        Integer testValue = 123;
        Byte result = converter.fromOtherCyodaType(testValue, "testColumn");
        assertEquals(result, Byte.valueOf((byte) 123));
    }

    @Test
    public void testFromOtherCyodaTypeWithOverflow() {
        // Test with a value that will overflow when converted to byte
        Integer testValue = 1000; // This will overflow when converted to byte
        Byte result = converter.fromOtherCyodaType(testValue, "testColumn");
        // 1000 % 256 = 232, and as a signed byte this is -24
        assertEquals(result, Byte.valueOf((byte) -24));
    }

    @Test
    public void testFromPrestoNative() {
        Long nativeValue = 123L;
        Byte result = converter.fromPrestoNative(nativeValue);
        assertEquals(result, Byte.valueOf((byte) 123));
    }

    @Test
    public void testStringify() {
        Byte testValue = (byte) 123;
        String result = converter.stringify(testValue);
        assertEquals(result, "123");
    }
}
