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

import java.math.BigInteger;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class UnboundIntegerValueConverterTest {

    private UnboundIntegerValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new UnboundIntegerValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.UNBOUND_INTEGER);
        assertEquals(converter.getClazz(), BigInteger.class);
    }

    @Test
    public void testFromStr() {
        String testValue = "123456789012345678901234567890";
        BigInteger result = converter.fromStr(testValue);
        assertEquals(result, new BigInteger("123456789012345678901234567890"));
    }

    @Test
    public void testToStr() {
        BigInteger testValue = new BigInteger("123456789012345678901234567890");
        String result = converter.toStr(testValue);
        assertEquals(result, "123456789012345678901234567890");
    }

    @Test
    public void testToSlice() {
        BigInteger testValue = new BigInteger("123456789012345678901234567890");
        Slice result = converter.toSlice(testValue);
        assertEquals(result.toStringUtf8(), "123456789012345678901234567890");
    }

    @Test
    public void testFromSlice() {
        String testValue = "123456789012345678901234567890";
        Slice slice = Slices.utf8Slice(testValue);
        BigInteger result = converter.fromSlice(slice);
        assertEquals(result, new BigInteger("123456789012345678901234567890"));
    }

    @Test
    public void testWriteValue() {
        BigInteger testValue = new BigInteger("123456789012345678901234567890");
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);
        Slice expectedSlice = Slices.utf8Slice("123456789012345678901234567890");

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeSlice(blockBuilder, expectedSlice);
    }

    @Test
    public void testStringify() {
        BigInteger testValue = new BigInteger("123456789012345678901234567890");
        String result = converter.stringify(testValue);
        assertEquals(result, "123456789012345678901234567890");
    }
}
