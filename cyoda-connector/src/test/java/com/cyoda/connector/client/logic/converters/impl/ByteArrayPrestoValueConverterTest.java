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
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;
import java.util.Base64;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ByteArrayPrestoValueConverterTest {

    private ByteArrayPrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new ByteArrayPrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.BYTE_ARRAY);
        assertEquals(converter.getClazz(), byte[].class);
    }

    @Test
    public void testToSlice() {
        byte[] testValue = "test string".getBytes();
        Slice result = converter.toSlice(testValue);

        byte[] resultBytes = new byte[result.length()];
        result.getBytes(0, resultBytes);

        assertArrayEquals(resultBytes, testValue);
    }

    @Test
    public void testFromSlice() {
        byte[] testValue = "test string".getBytes();
        Slice slice = Slices.wrappedBuffer(testValue);
        byte[] result = converter.fromSlice(slice);

        assertArrayEquals(result, testValue);
    }

    @Test
    public void testWriteValue() {
        byte[] testValue = "test string".getBytes();
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);
        Slice expectedSlice = Slices.wrappedBuffer(testValue);

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeSlice(blockBuilder, expectedSlice);
    }

    @Test
    public void testFromOtherCyodaType() {
        byte[] testValue = "test string".getBytes();
        String base64Value = Base64.getEncoder().encodeToString(testValue);

        byte[] result = converter.fromOtherCyodaType(base64Value, "testColumn");

        assertArrayEquals(result, testValue);
    }

    @Test
    public void testStringify() {
        byte[] testValue = "test string".getBytes();
        String result = converter.stringify(testValue);

        String expectedBase64 = Base64.getEncoder().encodeToString(testValue);
        assertEquals(result, expectedBase64);
    }

    private void assertArrayEquals(byte[] actual, byte[] expected) {
        assertEquals(actual.length, expected.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(actual[i], expected[i]);
        }
    }
}
