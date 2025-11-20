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
import io.trino.spi.type.DateTimeEncoding;
import io.trino.spi.type.TimeZoneKey;
import io.trino.spi.type.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ZonedDateTimePrestoValueConverterTest {

    private ZonedDateTimePrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new ZonedDateTimePrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.ZONED_DATE_TIME);
        assertEquals(converter.getClazz(), ZonedDateTime.class);
    }

    @Test
    public void testToLong() {
        ZonedDateTime testValue = ZonedDateTime.of(2023, 1, 15, 12, 30, 45, 123_000_000, ZoneId.of("UTC"));
        Long result = converter.toLong(testValue);

        // Verify the result is a packed date-time with zone
        long expectedMillis = testValue.toInstant().toEpochMilli();
        String expectedZoneId = testValue.getZone().getId();

        assertEquals(DateTimeEncoding.unpackMillisUtc(result), expectedMillis);
        assertEquals(DateTimeEncoding.unpackZoneKey(result).getId(), expectedZoneId);
    }

    @Test
    public void testFromLong() {
        ZonedDateTime expectedDateTime = ZonedDateTime.of(2023, 1, 15, 12, 30, 45, 123_000_000, ZoneId.of("UTC"));
        long millisUtc = expectedDateTime.toInstant().toEpochMilli();
        TimeZoneKey timeZoneKey = TimeZoneKey.getTimeZoneKey("UTC");
        long packedDateTimeWithZone = DateTimeEncoding.packDateTimeWithZone(millisUtc, timeZoneKey);

        ZonedDateTime result = converter.fromLong(packedDateTimeWithZone);

        assertEquals(result, expectedDateTime);
    }

    @Test
    public void testWriteValue() {
        ZonedDateTime testValue = ZonedDateTime.of(2023, 1, 15, 12, 30, 45, 123_000_000, ZoneId.of("UTC"));
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);
        Long expectedLong = converter.toLong(testValue);

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeLong(blockBuilder, expectedLong);
    }

    @Test
    public void testFromPrestoNative() {
        ZonedDateTime expectedDateTime = ZonedDateTime.of(2023, 1, 15, 12, 30, 45, 123_000_000, ZoneId.of("UTC"));
        long millisUtc = expectedDateTime.toInstant().toEpochMilli();
        TimeZoneKey timeZoneKey = TimeZoneKey.getTimeZoneKey("UTC");
        long packedDateTimeWithZone = DateTimeEncoding.packDateTimeWithZone(millisUtc, timeZoneKey);

        ZonedDateTime result = converter.fromPrestoNative(packedDateTimeWithZone);

        assertEquals(result, expectedDateTime);
    }

    @Test
    public void testFromOtherCyodaType() {
        // Test with ISO zoned date-time string
        String zonedDateTimeStr = "2023-01-15T12:30:45.123Z[UTC]";
        ZonedDateTime result = converter.fromOtherCyodaType(zonedDateTimeStr, "testColumn");

        ZonedDateTime expectedDateTime = ZonedDateTime.parse(zonedDateTimeStr);

        assertEquals(result, expectedDateTime);
    }

    @Test
    public void testStringify() {
        ZonedDateTime testValue = ZonedDateTime.of(2023, 1, 15, 12, 30, 45, 123_000_000, ZoneId.of("UTC"));

        String result = converter.stringify(testValue);

        assertEquals(result, "2023-01-15T12:30:45.123Z[UTC]");
    }

    @Test
    public void testDifferentTimeZones() {
        // Test with different time zones
        ZonedDateTime utcTime = ZonedDateTime.of(2023, 1, 15, 12, 30, 45, 123_000_000, ZoneId.of("UTC"));
        ZonedDateTime estTime = ZonedDateTime.of(2023, 1, 15, 7, 30, 45, 123_000_000, ZoneId.of("America/New_York"));

        // These should represent the same instant
        assertEquals(utcTime.toInstant(), estTime.toInstant());

        // But they should serialize differently
        Long utcLong = converter.toLong(utcTime);
        Long estLong = converter.toLong(estTime);

        assertNotEquals(utcLong, estLong);

        // And deserialize back to their original values
        assertEquals(converter.fromLong(utcLong), utcTime);
        assertEquals(converter.fromLong(estLong), estTime);
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Unable to parse not-a-zoned-date-time to ZonedDateTime at column testColumn")
    public void testFromOtherCyodaTypeWithInvalidFormat() {
        converter.fromOtherCyodaType("not-a-zoned-date-time", "testColumn");
    }
}
