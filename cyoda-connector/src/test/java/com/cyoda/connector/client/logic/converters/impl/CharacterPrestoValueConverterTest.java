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

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class CharacterPrestoValueConverterTest {

    private CharacterPrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new CharacterPrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.CHARACTER);
        assertEquals(converter.getClazz(), Character.class);
    }

    @Test
    public void testToSlice() {
        Character testValue = 'A';
        Slice result = converter.toSlice(testValue);
        assertEquals(result.toStringUtf8(), "A");
    }

    @Test
    public void testFromSlice() {
        Slice slice = Slices.utf8Slice("B");
        Character result = converter.fromSlice(slice);
        assertEquals(result, Character.valueOf('B'));
    }

    @Test
    public void testWriteValue() {
        Character testValue = 'C';
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);
        Slice expectedSlice = Slices.utf8Slice("C");

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeSlice(blockBuilder, expectedSlice);
    }

    @Test
    public void testFromPrestoNative() {
        Slice slice = Slices.utf8Slice("D");
        Character result = converter.fromPrestoNative(slice);
        assertEquals(result, Character.valueOf('D'));
    }

    @Test(expectedExceptions = UnsupportedOperationException.class, expectedExceptionsMessageRegExp = ".*Conversion operation from class java.lang.String to class java.lang.Character is not supported.*")
    public void testFromOtherCyodaType() {
        // This should throw UnsupportedOperationException
        converter.fromOtherCyodaType("E", "testColumn");
    }

    // These tests are removed since fromOtherCyodaType throws UnsupportedOperationException
    // before it can validate the input length

    @Test
    public void testStringify() {
        Character testValue = 'F';
        String result = converter.stringify(testValue);
        assertEquals(result, "F");
    }

    @Test
    public void testFromSliceWithMultiCharString() {
        // This tests what happens when a Slice contains more than one character
        // The converter should take the first character
        Slice slice = Slices.utf8Slice("XYZ");
        Character result = converter.fromSlice(slice);
        assertEquals(result, Character.valueOf('X'));
    }
}
