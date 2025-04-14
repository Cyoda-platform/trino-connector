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
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.block.IntArrayBlock;
import io.trino.spi.type.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class IntWrittenTypeValueConverterTest {

    private TestIntWrittenTypeValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new TestIntWrittenTypeValueConverter(DataType.INTEGER);
    }

    @Test
    public void testConstructor() {
        assertEquals(converter.getDataType(), DataType.INTEGER);
        assertEquals(converter.getClazz(), Integer.class);
    }

    @Test
    public void testWriteValue() {
        Integer testValue = 12345;
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeLong(blockBuilder, testValue.longValue());
    }

    @Test
    public void testFromPrestoNative() {
        Long nativeValue = 12345L;
        Integer expected = 12345;
        Integer result = converter.fromPrestoNative(nativeValue);
        assertEquals(result, expected);
    }

    @Test
    public void testBlockToNativeList() {
        // Create mock IntArrayBlock
        IntArrayBlock block = mock(IntArrayBlock.class);
        Type type = mock(Type.class);

        // Setup mock behavior
        when(block.getPositionCount()).thenReturn(2);
        when(block.getInt(0)).thenReturn(123);
        when(block.getInt(1)).thenReturn(456);

        // Test the method
        List<Integer> result = converter.blockToNativeList(block, type);

        // Verify results
        assertEquals(result.size(), 2);
        assertEquals(result.get(0), Integer.valueOf(123));
        assertEquals(result.get(1), Integer.valueOf(456));

        // Don't verify interactions as the method may call getPositionCount multiple times
        // in the actual implementation
    }

    // Test implementation of IntWrittenTypeValueConverter for testing
    private static class TestIntWrittenTypeValueConverter extends IntWrittenTypeValueConverter<Integer> {

        public TestIntWrittenTypeValueConverter(IDataType<Integer> dataType) {
            super(dataType);
        }

        @Override
        public Integer toInt(@Nonnull Integer value) {
            return value;
        }

        @Nonnull
        @Override
        public Integer fromInt(Integer value) {
            return value;
        }
    }
}
