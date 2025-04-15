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
import io.trino.spi.block.Int128ArrayBlock;
import io.trino.spi.type.Int128;
import io.trino.spi.type.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class LongDecimalTypeValueConverterTest {

    private TestLongDecimalTypeValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new TestLongDecimalTypeValueConverter(DataType.BIG_INTEGER);
    }

    @Test
    public void testConstructor() {
        assertEquals(converter.getDataType(), DataType.BIG_INTEGER);
        assertEquals(converter.getClazz(), BigInteger.class);
    }

    @Test
    public void testWriteValue() {
        BigInteger testValue = BigInteger.valueOf(12345);
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);
        Int128 expectedInt128 = Int128.valueOf(testValue);

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeObject(blockBuilder, expectedInt128);
    }

    @Test
    public void testFromPrestoNative() {
        BigInteger testValue = BigInteger.valueOf(12345);
        Int128 nativeValue = Int128.valueOf(testValue);
        BigInteger result = converter.fromPrestoNative(nativeValue);
        assertEquals(result, testValue);
    }

    @Test
    public void testBlockToNativeList() {
        // Create mock Int128ArrayBlock
        Int128ArrayBlock block = mock(Int128ArrayBlock.class);
        Type type = mock(Type.class);

        // Setup mock behavior
        when(block.getPositionCount()).thenReturn(2);
        when(block.getInt128(0)).thenReturn(Int128.valueOf(BigInteger.valueOf(123)));
        when(block.getInt128(1)).thenReturn(Int128.valueOf(BigInteger.valueOf(456)));

        // Test the method
        List<BigInteger> result = converter.blockToNativeList(block, type);

        // Verify results
        assertEquals(result.size(), 2);
        assertEquals(result.get(0), BigInteger.valueOf(123));
        assertEquals(result.get(1), BigInteger.valueOf(456));

        // Don't verify interactions as the method may call getPositionCount multiple times
        // in the actual implementation
    }

    // Test implementation of LongDecimalTypeValueConverter for testing
    private static class TestLongDecimalTypeValueConverter extends LongDecimalTypeValueConverter<BigInteger> {

        public TestLongDecimalTypeValueConverter(IDataType<BigInteger> dataType) {
            super(dataType);
        }

        @Override
        protected Int128 toInt128(BigInteger value) {
            return Int128.valueOf(value);
        }

        @Override
        protected BigInteger fromInt128(Int128 value) {
            return value.toBigInteger();
        }
    }
}
