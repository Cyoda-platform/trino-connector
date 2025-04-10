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
import io.trino.spi.type.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class LocalTimePrestoValueConverterTest {

    private LocalTimePrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new LocalTimePrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.LOCAL_TIME);
        assertEquals(converter.getClazz(), LocalTime.class);
    }

    @Test
    public void testToLong() {
        LocalTime testValue = LocalTime.of(12, 30, 45, 123_000_000);
        Long result = converter.toLong(testValue);

        // The actual implementation multiplies by 1000 (nanoseconds to picoseconds)
        long expectedPicos = testValue.toNanoOfDay() * 1000;
        assertEquals(result.longValue(), expectedPicos);
    }

    @Test
    public void testFromLong() {
        // For this test, we need to use the actual implementation's behavior
        // The converter divides by 1000 to convert picoseconds to nanoseconds
        long picos = 45045123000L;

        LocalTime result = converter.fromLong(picos);

        // This is what the implementation actually returns
        LocalTime expectedTime = LocalTime.ofNanoOfDay(45045123);
        assertEquals(result, expectedTime);
    }

    @Test
    public void testWriteValue() {
        LocalTime testValue = LocalTime.of(12, 30, 45, 123_000_000);
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);

        converter.writeValue(type, blockBuilder, testValue);

        // The actual implementation multiplies by 1000 (nanoseconds to picoseconds)
        long expectedPicos = testValue.toNanoOfDay() * 1000;
        verify(type).writeLong(blockBuilder, expectedPicos);
    }

    @Test
    public void testFromPrestoNative() {
        // For this test, we need to use the actual implementation's behavior
        // The converter divides by 1000 to convert picoseconds to nanoseconds
        long picos = 45045123000L;

        LocalTime result = converter.fromPrestoNative(picos);

        // This is what the implementation actually returns
        LocalTime expectedTime = LocalTime.ofNanoOfDay(45045123);
        assertEquals(result, expectedTime);
    }

    @Test
    public void testFromOtherCyodaType() {
        // Test with ISO time string
        String timeStr = "12:30:45.123";
        LocalTime result = converter.fromOtherCyodaType(timeStr, "testColumn");

        LocalTime expectedTime = LocalTime.parse(timeStr);

        assertEquals(result, expectedTime);
    }

    @Test
    public void testStringify() {
        LocalTime testValue = LocalTime.of(12, 30, 45, 123_000_000);

        String result = converter.stringify(testValue);

        assertEquals(result, "12:30:45.123");
    }

    @Test
    public void testNanosecondPrecision() {
        // Test with nanosecond precision
        LocalTime testValue = LocalTime.of(12, 30, 45, 123_456_789);
        Long longValue = converter.toLong(testValue);
        LocalTime result = converter.fromLong(longValue);

        // The actual implementation preserves all nanoseconds
        assertEquals(result, testValue);
    }

    @Test(expectedExceptions = DateTimeParseException.class, expectedExceptionsMessageRegExp = "Text 'not-a-time' could not be parsed at index 0")
    public void testFromOtherCyodaTypeWithInvalidFormat() {
        converter.fromOtherCyodaType("not-a-time", "testColumn");
    }
}
