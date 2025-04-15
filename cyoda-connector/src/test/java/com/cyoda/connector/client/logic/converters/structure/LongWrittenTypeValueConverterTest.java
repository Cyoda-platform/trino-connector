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
import io.trino.spi.block.LongArrayBlock;
import io.trino.spi.type.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class LongWrittenTypeValueConverterTest {

    private TestLongWrittenTypeValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new TestLongWrittenTypeValueConverter(DataType.LONG);
    }

    @Test
    public void testConstructor() {
        assertEquals(converter.getDataType(), DataType.LONG);
        assertEquals(converter.getClazz(), Long.class);
    }

    @Test
    public void testWriteValue() {
        Long testValue = 12345L;
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeLong(blockBuilder, testValue);
    }

    @Test
    public void testFromPrestoNative() {
        Long testValue = 12345L;
        Long result = converter.fromPrestoNative(testValue);
        assertEquals(result, testValue);
    }

    @Test
    public void testBlockToNativeList() {
        // Create mock LongArrayBlock
        LongArrayBlock block = mock(LongArrayBlock.class);
        Type type = mock(Type.class);

        // Setup mock behavior
        when(block.getPositionCount()).thenReturn(2);
        when(block.getLong(0)).thenReturn(123L);
        when(block.getLong(1)).thenReturn(456L);

        // Test the method
        List<Long> result = converter.blockToNativeList(block, type);

        // Verify results
        assertEquals(result.size(), 2);
        assertEquals(result.get(0), Long.valueOf(123L));
        assertEquals(result.get(1), Long.valueOf(456L));

        // Don't verify interactions as the method may call getPositionCount multiple times
        // in the actual implementation
    }

    // Test implementation of LongWrittenTypeValueConverter for testing
    private static class TestLongWrittenTypeValueConverter extends LongWrittenTypeValueConverter<Long> {

        public TestLongWrittenTypeValueConverter(IDataType<Long> dataType) {
            super(dataType);
        }

        @Override
        public Long toLong(@Nonnull Long value) {
            return value;
        }

        @Nonnull
        @Override
        public Long fromLong(Long value) {
            return value;
        }
    }
}
