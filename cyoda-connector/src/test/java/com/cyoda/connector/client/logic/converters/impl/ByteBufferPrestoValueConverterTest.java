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

public class ByteBufferPrestoValueConverterTest {

    private ByteBufferPrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new ByteBufferPrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.BYTE_BUFFER);
        assertEquals(converter.getClazz(), ByteBuffer.class);
    }

    @Test
    public void testToSlice() {
        byte[] testBytes = "test string".getBytes();
        ByteBuffer testValue = ByteBuffer.wrap(testBytes);
        Slice result = converter.toSlice(testValue);

        byte[] resultBytes = new byte[result.length()];
        result.getBytes(0, resultBytes);

        assertArrayEquals(resultBytes, testBytes);
    }

    @Test
    public void testFromSlice() {
        byte[] testBytes = "test string".getBytes();
        Slice slice = Slices.wrappedBuffer(testBytes);
        ByteBuffer result = converter.fromSlice(slice);

        byte[] resultBytes = new byte[result.remaining()];
        result.get(resultBytes);

        assertArrayEquals(resultBytes, testBytes);
    }

    @Test
    public void testWriteValue() {
        byte[] testBytes = "test string".getBytes();
        ByteBuffer testValue = ByteBuffer.wrap(testBytes);
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);
        Slice expectedSlice = Slices.wrappedBuffer(testBytes);

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeSlice(blockBuilder, expectedSlice);
    }

    @Test
    public void testFromPrestoNative() {
        byte[] testBytes = "test string".getBytes();
        Slice slice = Slices.wrappedBuffer(testBytes);
        ByteBuffer result = converter.fromPrestoNative(slice);

        byte[] resultBytes = new byte[result.remaining()];
        result.get(resultBytes);

        assertArrayEquals(resultBytes, testBytes);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class, expectedExceptionsMessageRegExp = ".*Conversion operation from class java.lang.String to class java.nio.ByteBuffer is not supported.*")
    public void testFromOtherCyodaType() {
        // This should throw UnsupportedOperationException
        converter.fromOtherCyodaType("dGVzdCBzdHJpbmc=", "testColumn");
    }

    @Test
    public void testStringify() {
        byte[] testBytes = "test string".getBytes();
        ByteBuffer testValue = ByteBuffer.wrap(testBytes);

        String result = converter.stringify(testValue);

        // The actual implementation uses hex encoding with 0x prefix, not base64
        String expected = "0x7465737420737472696E67";
        assertEquals(result, expected);
    }

    @Test
    public void testWithPositionAndLimit() {
        // Test with a ByteBuffer that has position and limit set
        byte[] testBytes = "test string".getBytes();
        ByteBuffer testValue = ByteBuffer.wrap(testBytes);
        testValue.position(2);  // Skip first 2 bytes
        testValue.limit(8);     // Only include up to byte 8

        Slice slice = converter.toSlice(testValue);
        ByteBuffer result = converter.fromSlice(slice);

        assertEquals(result.remaining(), 6);  // 8 - 2 = 6 bytes

        byte[] resultBytes = new byte[result.remaining()];
        result.get(resultBytes);

        byte[] expectedBytes = new byte[6];
        System.arraycopy(testBytes, 2, expectedBytes, 0, 6);

        assertArrayEquals(resultBytes, expectedBytes);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*cannot wrap java.nio.DirectByteBuffer.*")
    public void testDirectBuffer() {
        // Test with a direct ByteBuffer - this should throw an exception
        byte[] testBytes = "test string".getBytes();
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(testBytes.length);
        directBuffer.put(testBytes);
        directBuffer.flip();

        // This should throw IllegalArgumentException
        converter.toSlice(directBuffer);
    }

    private void assertArrayEquals(byte[] actual, byte[] expected) {
        assertEquals(actual.length, expected.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(actual[i], expected[i]);
        }
    }
}
