package com.cyoda.connector.client.logic.converters.impl;

import com.cyoda.connector.client.types.DataType;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class UUIDPrestoValueConverterTest {

    private UUIDPrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new UUIDPrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.UUID_TYPE);
        assertEquals(converter.getClazz(), UUID.class);
    }

    @Test
    public void testToSlice() {
        UUID testValue = UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6");
        Slice result = converter.toSlice(testValue);

        // Convert UUID to bytes
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(testValue.getMostSignificantBits());
        buffer.putLong(testValue.getLeastSignificantBits());

        // Compare the bytes
        byte[] expectedBytes = buffer.array();
        byte[] resultBytes = new byte[result.length()];
        result.getBytes(0, resultBytes);

        assertArrayEquals(resultBytes, expectedBytes);
    }

    @Test
    public void testFromSlice() {
        UUID testValue = UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6");

        // Convert UUID to bytes
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(testValue.getMostSignificantBits());
        buffer.putLong(testValue.getLeastSignificantBits());

        Slice slice = Slices.wrappedBuffer(buffer.array());
        UUID result = converter.fromSlice(slice);

        assertEquals(result, testValue);
    }

    @Test
    public void testWriteValue() {
        UUID testValue = UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6");
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);

        // Convert UUID to bytes
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(testValue.getMostSignificantBits());
        buffer.putLong(testValue.getLeastSignificantBits());
        Slice expectedSlice = Slices.wrappedBuffer(buffer.array());

        converter.writeValue(type, blockBuilder, testValue);

        verify(type).writeSlice(blockBuilder, expectedSlice);
    }

    @Test
    public void testFromPrestoNative() {
        UUID testValue = UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6");

        // Convert UUID to bytes
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(testValue.getMostSignificantBits());
        buffer.putLong(testValue.getLeastSignificantBits());

        Slice slice = Slices.wrappedBuffer(buffer.array());
        UUID result = converter.fromPrestoNative(slice);

        assertEquals(result, testValue);
    }

    @Test
    public void testFromOtherCyodaType() {
        // Test with UUID string
        String uuidStr = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";
        UUID result = converter.fromOtherCyodaType(uuidStr, "testColumn");

        assertEquals(result, UUID.fromString(uuidStr));
    }

    @Test
    public void testStringify() {
        UUID testValue = UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6");

        String result = converter.stringify(testValue);

        assertEquals(result, "f81d4fae-7dec-11d0-a765-00a0c91e6bf6");
    }

    @Test
    public void testRandomUUIDs() {
        // Test with random UUIDs
        for (int i = 0; i < 10; i++) {
            UUID randomUUID = UUID.randomUUID();
            Slice slice = converter.toSlice(randomUUID);
            UUID result = converter.fromSlice(slice);
            assertEquals(result, randomUUID);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFromOtherCyodaTypeWithInvalidFormat() {
        converter.fromOtherCyodaType("not-a-uuid", "testColumn");
    }

    private void assertArrayEquals(byte[] actual, byte[] expected) {
        assertEquals(actual.length, expected.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(actual[i], expected[i]);
        }
    }
}
