package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.types.DataType;
import com.cyoda.connector.client.types.IDataType;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class StringTypeValueConverterTest {

    private TestStringTypeValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new TestStringTypeValueConverter(DataType.STRING);
    }

    @Test
    public void testConstructor() {
        assertEquals(converter.getDataType(), DataType.STRING);
        assertEquals(converter.getClazz(), String.class);
    }

    @Test
    public void testToSlice() {
        String testValue = "test string";
        Slice slice = converter.toSlice(testValue);
        assertEquals(slice.toStringUtf8(), testValue);
    }

    @Test
    public void testFromSlice() {
        String testValue = "test string";
        Slice slice = Slices.utf8Slice(testValue);
        String result = converter.fromSlice(slice);
        assertEquals(result, testValue);
    }

    @Test
    public void testStringify() {
        String testValue = "test string";
        String result = converter.stringify(testValue);
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
    public void testFromPrestoNative() {
        String testValue = "test string";
        Slice slice = Slices.utf8Slice(testValue);
        String result = converter.fromPrestoNative(slice);
        assertEquals(result, testValue);
    }

    // Test implementation of StringTypeValueConverter for testing
    private static class TestStringTypeValueConverter extends StringTypeValueConverter<String> {

        public TestStringTypeValueConverter(IDataType<String> dataType) {
            super(dataType);
        }

        @Override
        protected String fromStr(String value) {
            return value;
        }

        @Override
        protected String toStr(String value) {
            return value;
        }
    }
}
