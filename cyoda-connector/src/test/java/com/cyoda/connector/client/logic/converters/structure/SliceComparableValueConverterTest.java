package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.types.DataType;
import com.cyoda.connector.client.types.IDataType;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.block.VariableWidthBlock;
import io.trino.spi.predicate.NullableValue;
import io.trino.spi.type.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class SliceComparableValueConverterTest {

    private TestSliceComparableValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new TestSliceComparableValueConverter(DataType.STRING);
    }

    @Test
    public void testConstructor() {
        assertEquals(converter.getDataType(), DataType.STRING);
        assertEquals(converter.getClazz(), String.class);
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

    @Test
    public void testBlockToNativeList() {
        // Create mock VariableWidthBlock
        VariableWidthBlock block = mock(VariableWidthBlock.class);
        Type type = mock(Type.class);

        // Setup mock behavior
        when(block.getPositionCount()).thenReturn(2);
        when(block.getSlice(0)).thenReturn(Slices.utf8Slice("value1"));
        when(block.getSlice(1)).thenReturn(Slices.utf8Slice("value2"));

        // Test the method
        List<String> result = converter.blockToNativeList(block, type);

        // Verify results
        assertEquals(result.size(), 2);
        assertEquals(result.get(0), "value1");
        assertEquals(result.get(1), "value2");

        // Don't verify interactions as the method may call getPositionCount multiple times
        // in the actual implementation
    }

    @Test
    public void testToNullableValue() {
        String testValue = "test string";
        Type type = mock(Type.class);
        // Need to mock the type properly for NullableValue constructor
        doReturn(Slice.class).when(type).getJavaType();
        Slice expectedSlice = Slices.utf8Slice(testValue);

        NullableValue result = converter.toNullableValue(type, testValue);

        assertNotNull(result);
        assertEquals(result.getType(), type);
        assertEquals(result.getValue(), expectedSlice);
    }

    // Test implementation of SliceComparableValueConverter for testing
    private static class TestSliceComparableValueConverter extends SliceComparableValueConverter<String> {

        public TestSliceComparableValueConverter(IDataType<String> dataType) {
            super(dataType);
        }

        @Override
        public Slice toSlice(@Nonnull String value) {
            return Slices.utf8Slice(value);
        }

        @Nonnull
        @Override
        public String fromSlice(Slice value) {
            return value.toStringUtf8();
        }
    }
}
