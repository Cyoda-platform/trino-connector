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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class LocalDateTimePrestoValueConverterTest {

    private LocalDateTimePrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new LocalDateTimePrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.LOCAL_DATE_TIME);
        assertEquals(converter.getClazz(), LocalDateTime.class);
    }

    @Test
    public void testToLong() {
        LocalDateTime testValue = LocalDateTime.of(2023, 1, 15, 12, 30, 45, 123_000_000);
        Long result = converter.toLong(testValue);

        // Convert to microseconds since epoch
        long expectedMicros = testValue.toInstant(ZoneOffset.UTC).toEpochMilli() * 1000;
        assertEquals(result.longValue(), expectedMicros);
    }

    @Test
    public void testFromLong() {
        LocalDateTime expectedDateTime = LocalDateTime.of(2023, 1, 15, 12, 30, 45, 123_000_000);
        long micros = expectedDateTime.toInstant(ZoneOffset.UTC).toEpochMilli() * 1000;

        LocalDateTime result = converter.fromLong(micros);

        assertEquals(result, expectedDateTime);
    }

    @Test
    public void testWriteValue() {
        LocalDateTime testValue = LocalDateTime.of(2023, 1, 15, 12, 30, 45, 123_000_000);
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);

        converter.writeValue(type, blockBuilder, testValue);

        long expectedMicros = testValue.toInstant(ZoneOffset.UTC).toEpochMilli() * 1000;
        verify(type).writeLong(blockBuilder, expectedMicros);
    }

    @Test
    public void testFromPrestoNative() {
        LocalDateTime expectedDateTime = LocalDateTime.of(2023, 1, 15, 12, 30, 45, 123_000_000);
        long micros = expectedDateTime.toInstant(ZoneOffset.UTC).toEpochMilli() * 1000;

        LocalDateTime result = converter.fromPrestoNative(micros);

        assertEquals(result, expectedDateTime);
    }

    @Test
    public void testFromOtherCyodaType() {
        // Test with ISO date-time string
        String dateTimeStr = "2023-01-15T12:30:45.123";
        LocalDateTime result = converter.fromOtherCyodaType(dateTimeStr, "testColumn");

        LocalDateTime expectedDateTime = LocalDateTime.parse(dateTimeStr);

        assertEquals(result, expectedDateTime);
    }

    @Test
    public void testStringify() {
        LocalDateTime testValue = LocalDateTime.of(2023, 1, 15, 12, 30, 45, 123_000_000);

        String result = converter.stringify(testValue);

        assertEquals(result, "2023-01-15T12:30:45.123");
    }

    @Test
    public void testNanosecondPrecision() {
        // Test with nanosecond precision
        LocalDateTime testValue = LocalDateTime.of(2023, 1, 15, 12, 30, 45, 123_456_789);
        Long longValue = converter.toLong(testValue);
        LocalDateTime result = converter.fromLong(longValue);

        // The microsecond precision should truncate the nanoseconds
        LocalDateTime expectedDateTime = LocalDateTime.of(2023, 1, 15, 12, 30, 45, 123_000_000);
        assertEquals(result, expectedDateTime);
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Unable to parse not-a-date-time to LocalDateTime at column testColumn")
    public void testFromOtherCyodaTypeWithInvalidFormat() {
        converter.fromOtherCyodaType("not-a-date-time", "testColumn");
    }
}
