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

public class StringPrestoValueConverterTest {

    private StringPrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new StringPrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.STRING);
        assertEquals(converter.getClazz(), String.class);
    }

    @Test
    public void testFromStr() {
        String testValue = "test string";
        String result = converter.fromStr(testValue);
        assertEquals(result, testValue);
    }

    @Test
    public void testToStr() {
        String testValue = "test string";
        String result = converter.toStr(testValue);
        assertEquals(result, testValue);
    }

    @Test
    public void testToSlice() {
        String testValue = "test string";
        Slice result = converter.toSlice(testValue);
        assertEquals(result.toStringUtf8(), testValue);
    }

    @Test
    public void testFromSlice() {
        String testValue = "test string";
        Slice slice = Slices.utf8Slice(testValue);
        String result = converter.fromSlice(slice);
        assertEquals(result, testValue);
    }

    @Test
    public void testWriteValue() {
        String testValue = "test string";
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);
        Slice expectedSlice = Slices.utf8Slice(testValue);

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeSlice(blockBuilder, expectedSlice);
    }

    @Test
    public void testFromOtherCyodaType() {
        // Test with a non-string object
        Integer testValue = 12345;
        String result = converter.fromOtherCyodaType(testValue, "testColumn");
        assertEquals(result, "12345");

        // Test with a string object
        String stringValue = "test string";
        String stringResult = converter.fromOtherCyodaType(stringValue, "testColumn");
        assertEquals(stringResult, stringValue);
    }

    @Test
    public void testStringify() {
        String testValue = "test string";
        String result = converter.stringify(testValue);
        assertEquals(result, testValue);
    }
}
