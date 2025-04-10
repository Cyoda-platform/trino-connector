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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class DatePrestoValueConverterTest {

    private DatePrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new DatePrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.DATE);
        assertEquals(converter.getClazz(), Date.class);
    }

    @Test
    public void testToLong() {
        // Create a date with a known instant
        Instant instant = Instant.parse("2023-01-15T12:30:45.123Z");
        Date testValue = Date.from(instant);

        Long result = converter.toLong(testValue);

        // The timestamp converter multiplies by 1000
        assertEquals(result.longValue(), instant.toEpochMilli() * 1000);
    }

    @Test
    public void testFromLong() {
        // Create a timestamp with a known value
        Instant instant = Instant.parse("2023-01-15T12:30:45.123Z");
        Long testValue = instant.toEpochMilli() * 1000;

        Date result = converter.fromLong(testValue);

        assertEquals(result.toInstant(), instant);
    }

    @Test
    public void testWriteValue() {
        Instant instant = Instant.parse("2023-01-15T12:30:45.123Z");
        Date testValue = Date.from(instant);
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeLong(blockBuilder, instant.toEpochMilli() * 1000);
    }

    @Test
    public void testFromPrestoNative() {
        Instant instant = Instant.parse("2023-01-15T12:30:45.123Z");
        Long nativeValue = instant.toEpochMilli() * 1000;

        Date result = converter.fromPrestoNative(nativeValue);

        assertEquals(result.toInstant(), instant);
    }

    @Test
    public void testFromOtherCyodaType() {
        // Test with ISO date-time string
        String dateTimeStr = "2023-01-15T12:30:45.123";
        Date result = converter.fromOtherCyodaType(dateTimeStr, "testColumn");

        LocalDateTime expectedLocalDateTime = LocalDateTime.parse(dateTimeStr);
        Timestamp expectedTimestamp = Timestamp.valueOf(expectedLocalDateTime);

        assertEquals(result, expectedTimestamp);
    }

    @Test
    public void testStringify() {
        Instant instant = Instant.parse("2023-01-15T12:30:45.123Z");
        Date testValue = Date.from(instant);

        String result = converter.stringify(testValue);

        assertEquals(result, instant.toString());
    }

    @Test
    public void testToInstant() {
        Instant instant = Instant.parse("2023-01-15T12:30:45.123Z");
        Date testValue = Date.from(instant);

        Instant result = converter.toInstant(testValue);

        assertEquals(result, instant);
    }

    @Test
    public void testFromInstant() {
        Instant instant = Instant.parse("2023-01-15T12:30:45.123Z");

        Date result = converter.fromInstant(instant);

        assertEquals(result.toInstant(), instant);
    }
}
