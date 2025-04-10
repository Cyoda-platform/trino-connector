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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class LocalDatePrestoValueConverterTest {

    private LocalDatePrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new LocalDatePrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.LOCAL_DATE);
        assertEquals(converter.getClazz(), LocalDate.class);
    }

    @Test
    public void testToInt() {
        LocalDate testValue = LocalDate.of(2023, 1, 15);
        Integer result = converter.toInt(testValue);

        // The epoch day for 2023-01-15
        long expectedEpochDay = testValue.toEpochDay();
        assertEquals(result.intValue(), expectedEpochDay);
    }

    @Test
    public void testFromInt() {
        // The epoch day for 2023-01-15
        LocalDate expectedDate = LocalDate.of(2023, 1, 15);
        long epochDay = expectedDate.toEpochDay();

        LocalDate result = converter.fromInt((int) epochDay);

        assertEquals(result, expectedDate);
    }

    @Test
    public void testWriteValue() {
        LocalDate testValue = LocalDate.of(2023, 1, 15);
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeLong(blockBuilder, testValue.toEpochDay());
    }

    @Test
    public void testFromPrestoNative() {
        LocalDate expectedDate = LocalDate.of(2023, 1, 15);
        long epochDay = expectedDate.toEpochDay();

        LocalDate result = converter.fromPrestoNative((long) epochDay);

        assertEquals(result, expectedDate);
    }

    @Test
    public void testFromOtherCyodaType() {
        // Test with ISO date string
        String dateStr = "2023-01-15";
        LocalDate result = converter.fromOtherCyodaType(dateStr, "testColumn");

        LocalDate expectedDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);

        assertEquals(result, expectedDate);
    }

    @Test
    public void testStringify() {
        LocalDate testValue = LocalDate.of(2023, 1, 15);

        String result = converter.stringify(testValue);

        assertEquals(result, "2023-01-15");
    }

    @Test
    public void testEdgeCases() {
        // The implementation has limitations on the range of dates it can handle
        // Test with a very old but supported date
        LocalDate oldDate = LocalDate.of(-466210, 8, 20); // This is what MIN_VALUE gets converted to
        Integer oldInt = converter.toInt(oldDate);
        LocalDate oldDateResult = converter.fromInt(oldInt);
        assertEquals(oldDateResult, oldDate);

        // Test with a recent date
        LocalDate recentDate = LocalDate.of(2023, 1, 15);
        Integer recentInt = converter.toInt(recentDate);
        LocalDate recentDateResult = converter.fromInt(recentInt);
        assertEquals(recentDateResult, recentDate);
    }
}
