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

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class UnboundDecimalValueConverterTest {

    private UnboundDecimalValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new UnboundDecimalValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.UNBOUND_DECIMAL);
        assertEquals(converter.getClazz(), BigDecimal.class);
    }

    @Test
    public void testFromStr() {
        String testValue = "123456.789";
        BigDecimal result = converter.fromStr(testValue);
        assertEquals(result, new BigDecimal("123456.789"));
    }

    @Test
    public void testToStr() {
        BigDecimal testValue = new BigDecimal("123456.7890000");
        String result = converter.toStr(testValue);
        assertEquals(result, "123456.789"); // Trailing zeros should be stripped
    }

    @Test
    public void testToSlice() {
        BigDecimal testValue = new BigDecimal("123456.789");
        Slice result = converter.toSlice(testValue);
        assertEquals(result.toStringUtf8(), "123456.789");
    }

    @Test
    public void testFromSlice() {
        String testValue = "123456.789";
        Slice slice = Slices.utf8Slice(testValue);
        BigDecimal result = converter.fromSlice(slice);
        assertEquals(result, new BigDecimal("123456.789"));
    }

    @Test
    public void testWriteValue() {
        BigDecimal testValue = new BigDecimal("123456.789");
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);
        Slice expectedSlice = Slices.utf8Slice("123456.789");

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeSlice(blockBuilder, expectedSlice);
    }

    @Test
    public void testStringify() {
        BigDecimal testValue = new BigDecimal("123456.7890000");
        String result = converter.stringify(testValue);
        assertEquals(result, "123456.789"); // Trailing zeros should be stripped
    }

    @Test
    public void testVeryLargeDecimalValues() {
        // Test a very large value with many digits before decimal
        BigDecimal largeValue = new BigDecimal("12345678901234567890123456789012345678901234567890.0");
        String result = converter.stringify(largeValue);
        assertEquals(result, "1.234567890123456789012345678901234567890123456789E+49");
        assertEquals(converter.fromStr(result), largeValue.stripTrailingZeros());

        // Test a very large value with many digits after decimal
        BigDecimal largePrecisionValue = new BigDecimal("0.12345678901234567890123456789012345678901234567890");
        result = converter.stringify(largePrecisionValue);
        assertEquals(result, "0.1234567890123456789012345678901234567890123456789");
        assertEquals(converter.fromStr(result), largePrecisionValue.stripTrailingZeros());

        // Test a very large value with scientific notation
        BigDecimal scientificValue = new BigDecimal("1.23456789E+100");
        result = converter.stringify(scientificValue);
        // Should maintain precision but convert to plain string format, without trailing zeros
        assertEquals(converter.fromStr(result).stripTrailingZeros(), scientificValue.stripTrailingZeros());
    }

    @Test
    public void testVerySmallDecimalValues() {
        // Test a very small value close to zero
        BigDecimal smallValue = new BigDecimal("0.00000000000000000000000000000000000000000000000001");
        String result = converter.stringify(smallValue);
        assertEquals(result, "0.00000000000000000000000000000000000000000000000001"); // no trailing zeros to strip
        assertEquals(converter.fromStr(result), smallValue);

        // Test a very small value in scientific notation
        BigDecimal scientificSmallValue = new BigDecimal("1.23456789E-100");
        result = converter.stringify(scientificSmallValue);
        // Should maintain precision but convert to plain string format, without trailing zeros
        assertEquals(converter.fromStr(result).stripTrailingZeros(), scientificSmallValue.stripTrailingZeros());
    }
}
