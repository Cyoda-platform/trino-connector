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
import io.trino.spi.type.Int128;
import io.trino.spi.type.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigInteger;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class  BigIntegerPrestoValueConverterTest {

    private BigIntegerPrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new BigIntegerPrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.BIG_INTEGER);
        assertEquals(converter.getClazz(), BigInteger.class);
    }

    @Test
    public void testToInt128() {
        BigInteger testValue = new BigInteger("123456789012345678901234567890");
        Int128 result = converter.toInt128(testValue);
        assertEquals(result.toBigInteger(), testValue);
    }

    @Test
    public void testFromInt128() {
        BigInteger testValue = new BigInteger("123456789012345678901234567890");
        Int128 int128Value = Int128.valueOf(testValue);
        BigInteger result = converter.fromInt128(int128Value);
        assertEquals(result, testValue);
    }

    @Test
    public void testWriteValue() {
        BigInteger testValue = new BigInteger("123456789012345678901234567890");
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);
        Int128 expectedInt128 = Int128.valueOf(testValue);

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeObject(blockBuilder, expectedInt128);
    }

    @Test
    public void testFromPrestoNative() {
        BigInteger testValue = new BigInteger("123456789012345678901234567890");
        Int128 nativeValue = Int128.valueOf(testValue);
        BigInteger result = converter.fromPrestoNative(nativeValue);
        assertEquals(result, testValue);
    }

    @Test
    public void testStringify() {
        BigInteger testValue = new BigInteger("123456789012345678901234567890");
        String result = converter.stringify(testValue);
        assertEquals(result, "123456789012345678901234567890");
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void testToInt128_ExceedsMaxValue() {
        // 2^127 (exceeds max value)
        BigInteger tooLarge = BigInteger.ONE.shiftLeft(127);
        converter.toInt128(tooLarge);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void testToInt128_ExceedsMinValue() {
        // -2^127 - 1 (exceeds min value)
        BigInteger tooSmall = BigInteger.ONE.shiftLeft(127).negate()
                                          .subtract(BigInteger.ONE);
        converter.toInt128(tooSmall);
    }

    @Test
    public void testToInt128_EdgeValues() {
        // Test maximum allowed value (2^127 - 1)
        BigInteger maxValue = BigInteger.ONE.shiftLeft(127)
                                          .subtract(BigInteger.ONE);
        Int128 result = converter.toInt128(maxValue);
        assertEquals(result.toBigInteger(), maxValue);

        // Test minimum allowed value (-2^127)
        BigInteger minValue = BigInteger.ONE.shiftLeft(127).negate();
        result = converter.toInt128(minValue);
        assertEquals(result.toBigInteger(), minValue);
    }
}
