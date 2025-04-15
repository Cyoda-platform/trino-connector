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

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class BooleanPrestoValueConverterTest {

    private BooleanPrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new BooleanPrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.BOOLEAN);
        assertEquals(converter.getClazz(), Boolean.class);
    }

    @Test
    public void testWriteValue() {
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);

        // Test with true
        converter.writeValue(type, blockBuilder, true);
        verify(type).writeBoolean(blockBuilder, true);

        // Test with false
        reset(type, blockBuilder);
        converter.writeValue(type, blockBuilder, false);
        verify(type).writeBoolean(blockBuilder, false);
    }

    @Test
    public void testFromPrestoNative() {
        // Test with true
        Boolean result = converter.fromPrestoNative(true);
        assertEquals(result, Boolean.TRUE);

        // Test with false
        result = converter.fromPrestoNative(false);
        assertEquals(result, Boolean.FALSE);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class, expectedExceptionsMessageRegExp = ".*Conversion operation from class java.lang.Integer to class java.lang.Boolean is not supported.*")
    public void testFromOtherCyodaTypeWithInteger() {
        // This should throw UnsupportedOperationException
        converter.fromOtherCyodaType(1, "testColumn");
    }

    @Test(expectedExceptions = UnsupportedOperationException.class, expectedExceptionsMessageRegExp = ".*Conversion operation from class java.lang.String to class java.lang.Boolean is not supported.*")
    public void testFromOtherCyodaTypeWithString() {
        // This should throw UnsupportedOperationException
        converter.fromOtherCyodaType("true", "testColumn");
    }

    // These tests are removed since fromOtherCyodaType throws UnsupportedOperationException
    // before it can validate the input

    @Test
    public void testStringify() {
        assertEquals(converter.stringify(true), "true");
        assertEquals(converter.stringify(false), "false");
    }
}
