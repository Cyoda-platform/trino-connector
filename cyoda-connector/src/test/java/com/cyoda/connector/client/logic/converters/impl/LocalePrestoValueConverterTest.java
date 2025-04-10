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

import java.util.Locale;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class LocalePrestoValueConverterTest {

    private LocalePrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new LocalePrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.LOCALE);
        assertEquals(converter.getClazz(), Locale.class);
    }

    @Test
    public void testToSlice() {
        Locale testValue = Locale.US;
        Slice result = converter.toSlice(testValue);

        assertEquals(result.toStringUtf8(), "en_US");
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "Cannot convert from json")
    public void testFromSlice() {
        // This should throw IllegalStateException since the converter expects JSON
        Slice slice = Slices.utf8Slice("en_US");
        converter.fromSlice(slice);
    }

    @Test
    public void testWriteValue() {
        Locale testValue = Locale.US;
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);
        Slice expectedSlice = Slices.utf8Slice("en_US");

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeSlice(blockBuilder, expectedSlice);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class, expectedExceptionsMessageRegExp = "Condition pushdown is not supported for LOCALE")
    public void testFromPrestoNative() {
        // This should throw UnsupportedOperationException
        Slice slice = Slices.utf8Slice("en_US");
        converter.fromPrestoNative(slice);
    }

    @Test
    public void testFromOtherCyodaType() {
        // Test language only
        assertEquals(
                converter.fromOtherCyodaType("en", "testColumn"),
                new Locale.Builder()
                        .setLanguage("en")
                        .build()
        );

        // Test language and country. Use toLanguageTag to avoid upper/lower case diffs
        assertEquals(
                converter.fromOtherCyodaType("en_US", "testColumn").toLanguageTag(),
                Locale.US.toLanguageTag()
        );

        // Test language, country and variant
        assertEquals(
                converter.fromOtherCyodaType("en_US_POSIX", "testColumn"),
                new Locale.Builder()
                        .setLanguage("en")
                        .setRegion("US")
                        .setVariant("POSIX")
                        .build()
        );

        // Test with comma format
        assertEquals(
                converter.fromOtherCyodaType("en, US", "testColumn"),
                new Locale.Builder()
                        .setLanguage("en")
                        .setRegion("US")
                        .build()
        );

        // Test with comma and variant
        assertEquals(
            converter.fromOtherCyodaType("en, US, POSIX", "testColumn"),
                new Locale.Builder()
                        .setLanguage("en")
                        .setRegion("US")
                        .setVariant("POSIX")
                        .build()
        );
    }

    @Test
    public void testStringify() {
        Locale testValue = Locale.US;

        String result = converter.stringify(testValue);

        assertEquals(result, "en_US");
    }

}
