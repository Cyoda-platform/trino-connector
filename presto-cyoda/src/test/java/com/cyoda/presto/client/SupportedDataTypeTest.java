/*
 * Copyright (C) 2022 Cyoda Ltd.
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

package com.cyoda.presto.client;

import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.client.types.SupportedDataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.common.type.TinyintType;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SupportedDataTypeTest {

    @Test
    public void testStringifyYear() {
        final int isoYear = 2020;
        Year year = Year.of(isoYear);
        SupportedDataType<Year> sdt = SupportedDataType.of(year,Year.class);
        Optional<String> stringify = sdt.stringify();
        assertTrue(stringify.isPresent());
        String str = stringify.get();
        assertEquals(str, "2020");
    }

    @Test
    public void testStringifyYearMonth() {
        YearMonth yearMonth = YearMonth.of(2020,11);
        SupportedDataType<YearMonth> sdt = SupportedDataType.of(yearMonth,YearMonth.class);
        Optional<String> stringify = sdt.stringify();
        assertTrue(stringify.isPresent());
        String str = stringify.get();
        assertEquals(str, "2020-11");
    }


    @Test
    public void testStringifyLocalDate() {
        LocalDate ld = LocalDate.of(2020,11,1);
        SupportedDataType<LocalDate> sdt = SupportedDataType.of(ld,LocalDate.class);
        Optional<String> stringify = sdt.stringify();
        assertTrue(stringify.isPresent());
        String str = stringify.get();
        assertEquals(str, "2020-11-01");
    }

    @Test
    public void testStringifyLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2020,11,1,8,12,34,22023);
        SupportedDataType<LocalDateTime> sdt = SupportedDataType.of(ldt,LocalDateTime.class);
        Optional<String> stringify = sdt.stringify();
        assertTrue(stringify.isPresent());
        String str = stringify.get();
        assertEquals(str, "2020-11-01T08:12:34.000022023");
    }

    @Test
    public void testStringifyLocalTime() {
        LocalTime lt = LocalTime.of(14,15,16);
        SupportedDataType<LocalTime> sdt = SupportedDataType.of(lt,LocalTime.class);
        Optional<String> stringify = sdt.stringify();
        assertTrue(stringify.isPresent());
        String str = stringify.get();
        assertEquals(str, "14:15:16");
    }

    @Test
    public void testStringifyObject() {
        CyodaColumnHandle cch = new CyodaColumnHandle("connector 1", "happy column", TinyintType.TINYINT, DataType.SHORT, 2, "myKey");
        SupportedDataType<?> sdt = SupportedDataType.ofObject(cch);
        Optional<String> stringify = sdt.stringify();
        assertTrue(stringify.isPresent());
        String str = stringify.get();
        String expected ="{\n" +
                "  \"connectorId\" : \"connector 1\",\n" +
                "  \"columnName\" : \"happy column\",\n" +
                "  \"columnType\" : \"tinyint\",\n" +
                "  \"ordinalPosition\" : 2,\n" +
                "  \"requestHandlerKey\" : \"myKey\"\n" +
                "}";
        assertEquals(str, expected);
    }

}