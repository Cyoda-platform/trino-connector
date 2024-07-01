/*
 * Copyright (C) 2023 Cyoda Ltd.
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

import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

public class LongPrestoValueConverterTest {
    LongPrestoValueConverter converter = new LongPrestoValueConverter();

    @Test
    public void testWholeNumbers() {
        assertEquals(converter.fromOtherCyodaType((byte) 1,"hello"),(Long) 1L);
        assertEquals(converter.fromOtherCyodaType((short)10,"hello"),(Long) 10L);
        assertEquals(converter.fromOtherCyodaType((int)100,"hello"),(Long) 100L);
        assertEquals(converter.fromOtherCyodaType(new AtomicInteger(10_000),"hello"),(Long) 10_000L);
        assertEquals(converter.fromOtherCyodaType(new AtomicLong(100_000),"hello"),(Long) 100_000L);
    }

    @Test
    public void testBigInteger() {
        assertEquals(converter.fromOtherCyodaType(BigInteger.valueOf(1_000),"hello"),(Long) 1_000L);
        assertEquals(converter.fromOtherCyodaType(BigInteger.valueOf(Long.MIN_VALUE),"hello"),(Long) Long.MIN_VALUE);
        assertEquals(converter.fromOtherCyodaType(BigInteger.valueOf(Long.MAX_VALUE),"hello"),(Long) Long.MAX_VALUE);
        assertThrows(UnsupportedOperationException.class, () -> converter.fromOtherCyodaType(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE),"hello"));
        assertThrows(UnsupportedOperationException.class, () -> converter.fromOtherCyodaType(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE),"hello"));

    }
    
    @Test
    public void testBoolean() {
        assertEquals(converter.fromOtherCyodaType(true,"hello"),(Long) 1L);
        assertEquals(converter.fromOtherCyodaType(false,"hello"),(Long) 0L);
    }
    
    @Test
    public void testUnsupportedNumbers() {
        // There are many types. We'll just test the normal ones...
        assertThrows(UnsupportedOperationException.class, () -> converter.fromOtherCyodaType(1.1f,"hello"));
        assertThrows(UnsupportedOperationException.class, () -> converter.fromOtherCyodaType(1.1d,"hello"));
        assertThrows(UnsupportedOperationException.class, () -> converter.fromOtherCyodaType(BigDecimal.valueOf(1.3d),"hello"));

    }
}