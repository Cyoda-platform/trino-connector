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

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class BigDecimalPrestoValueConverterTest {

    private BigDecimalPrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new BigDecimalPrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.BIG_DECIMAL);
        assertEquals(converter.getClazz(), BigDecimal.class);
    }

    @Test
    public void testToInt128() {
        BigDecimal testValue = new BigDecimal("123456.789");
        Int128 result = converter.toInt128(testValue);

        // The value is scaled to 18 decimal places
        BigDecimal expectedScaled = testValue.setScale(BigDecimalPrestoValueConverter.SCALE);
        BigInteger expectedUnscaled = expectedScaled.unscaledValue();

        assertEquals(result.toBigInteger(), expectedUnscaled);
    }

    @Test
    public void testToInt128WithTrailingZeros() {
        BigDecimal testValue = new BigDecimal("123456.7890000");
        Int128 result = converter.toInt128(testValue);

        // The value should have trailing zeros stripped
        BigDecimal expectedTrimmed = testValue.stripTrailingZeros();
        BigDecimal expectedScaled = expectedTrimmed.setScale(BigDecimalPrestoValueConverter.SCALE);
        BigInteger expectedUnscaled = expectedScaled.unscaledValue();

        assertEquals(result.toBigInteger(), expectedUnscaled);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testToInt128WithTooHighScale() {
        // Create a BigDecimal with scale higher than the maximum allowed (18)
        BigDecimal testValue = new BigDecimal("0.0123456789012345678901");
        converter.toInt128(testValue);
    }

    @Test
    public void testFromInt128() {
        // Create a BigDecimal with a known value
        BigDecimal testValue = new BigDecimal("123456.789");
        BigDecimal scaledValue = testValue.setScale(BigDecimalPrestoValueConverter.SCALE);
        BigInteger unscaledValue = scaledValue.unscaledValue();

        Int128 int128Value = Int128.valueOf(unscaledValue);
        BigDecimal result = converter.fromInt128(int128Value);

        // The result should be equal to the original value (with proper scale)
        assertEquals(result.compareTo(scaledValue), 0);
    }

    @Test
    public void testWriteValue() {
        BigDecimal testValue = new BigDecimal("123456.789");
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);

        converter.writeValue(type, blockBuilder, testValue);

        // Verify that writeObject was called with the correct Int128 value
        verify(type).writeObject(eq(blockBuilder), any(Int128.class));
    }

    @Test
    public void testFromPrestoNative() {
        // Create a BigDecimal with a known value
        BigDecimal testValue = new BigDecimal("123456.789");
        BigDecimal scaledValue = testValue.setScale(BigDecimalPrestoValueConverter.SCALE);
        BigInteger unscaledValue = scaledValue.unscaledValue();

        Int128 nativeValue = Int128.valueOf(unscaledValue);
        BigDecimal result = converter.fromPrestoNative(nativeValue);

        // The result should be equal to the original value (with proper scale)
        assertEquals(result.compareTo(scaledValue), 0);
    }

    @Test
    public void testStringify() {
        BigDecimal testValue = new BigDecimal("123456.789");
        String result = converter.stringify(testValue);
        assertEquals(result, "123456.789");
    }
}
