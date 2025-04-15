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

package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.types.DataType;
import com.cyoda.connector.client.types.IDataType;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.block.VariableWidthBlock;
import io.trino.spi.type.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class SliceUncomparableValueConverterTest {

    private TestSliceUncomparableValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new TestSliceUncomparableValueConverter(DataType.BYTE_ARRAY);
    }

    @Test
    public void testConstructor() {
        assertEquals(converter.getDataType(), DataType.BYTE_ARRAY);
        assertEquals(converter.getClazz(), byte[].class);
    }

    @Test
    public void testWriteValue() {
        byte[] testValue = "test".getBytes();
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);
        Slice expectedSlice = Slices.wrappedBuffer(testValue);

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeSlice(blockBuilder, expectedSlice);
    }

    @Test
    public void testFromPrestoNative() {
        byte[] testValue = "test".getBytes();
        Slice slice = Slices.wrappedBuffer(testValue);
        byte[] result = converter.fromPrestoNative(slice);

        assertNotNull(result);
        assertEquals(result.length, testValue.length);
        for (int i = 0; i < testValue.length; i++) {
            assertEquals(result[i], testValue[i]);
        }
    }

    @Test
    public void testBlockToNativeList() {
        // Create mock VariableWidthBlock
        VariableWidthBlock block = mock(VariableWidthBlock.class);
        Type type = mock(Type.class);

        byte[] value1 = "value1".getBytes();
        byte[] value2 = "value2".getBytes();

        // Setup mock behavior
        when(block.getPositionCount()).thenReturn(2);
        when(block.getSlice(0)).thenReturn(Slices.wrappedBuffer(value1));
        when(block.getSlice(1)).thenReturn(Slices.wrappedBuffer(value2));

        // Test the method
        List<byte[]> result = converter.blockToNativeList(block, type);

        // Verify results
        assertEquals(result.size(), 2);
        assertArrayEquals(result.get(0), value1);
        assertArrayEquals(result.get(1), value2);

        // Don't verify interactions as the method may call getPositionCount multiple times
        // in the actual implementation
    }

    private void assertArrayEquals(byte[] actual, byte[] expected) {
        assertEquals(actual.length, expected.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(actual[i], expected[i]);
        }
    }

    // Test implementation of SliceUncomparableValueConverter for testing
    private static class TestSliceUncomparableValueConverter extends SliceUncomparableValueConverter<byte[]> {

        public TestSliceUncomparableValueConverter(IDataType<byte[]> dataType) {
            super(dataType);
        }

        @Override
        public Slice toSlice(@Nonnull byte[] value) {
            return Slices.wrappedBuffer(value);
        }

        @Nonnull
        @Override
        public byte[] fromSlice(Slice value) {
            byte[] result = new byte[value.length()];
            value.getBytes(0, result);
            return result;
        }
    }
}
