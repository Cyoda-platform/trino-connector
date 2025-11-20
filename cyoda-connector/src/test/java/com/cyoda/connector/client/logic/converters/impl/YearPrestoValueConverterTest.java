package com.cyoda.connector.client.logic.converters.impl;

import com.cyoda.connector.client.types.DataType;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Year;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class YearPrestoValueConverterTest {

    private YearPrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new YearPrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.YEAR);
        assertEquals(converter.getClazz(), Year.class);
    }

    @Test
    public void testToLong() {
        Year testValue = Year.of(2023);
        Long result = converter.toLong(testValue);
        
        assertEquals(result, Long.valueOf(2023));
    }

    @Test
    public void testFromLong() {
        Long testValue = 2023L;
        Year result = converter.fromLong(testValue);
        
        assertEquals(result, Year.of(2023));
    }

    @Test
    public void testWriteValue() {
        Year testValue = Year.of(2023);
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);
        
        converter.writeValue(type, blockBuilder, testValue);
        
        verify(type).writeLong(blockBuilder, 2023L);
    }

    @Test
    public void testFromPrestoNative() {
        Long nativeValue = 2023L;
        Year result = converter.fromPrestoNative(nativeValue);
        
        assertEquals(result, Year.of(2023));
    }

    @Test
    public void testFromOtherCyodaType() {
        // Test with year string
        String yearStr = "2023";
        Year result = converter.fromOtherCyodaType(yearStr, "testColumn");
        
        assertEquals(result, Year.of(2023));
    }

    @Test
    public void testStringify() {
        Year testValue = Year.of(2023);
        
        String result = converter.stringify(testValue);
        
        assertEquals(result, "2023");
    }
    
    @Test
    public void testEdgeCases() {
        // Test minimum year
        Year minYear = Year.of(Year.MIN_VALUE);
        Long minLong = converter.toLong(minYear);
        Year minYearResult = converter.fromLong(minLong);
        assertEquals(minYearResult, minYear);
        
        // Test maximum year
        Year maxYear = Year.of(Year.MAX_VALUE);
        Long maxLong = converter.toLong(maxYear);
        Year maxYearResult = converter.fromLong(maxLong);
        assertEquals(maxYearResult, maxYear);
    }
    
    @Test(expectedExceptions = RuntimeException.class)
    public void testFromOtherCyodaTypeWithInvalidFormat() {
        converter.fromOtherCyodaType("not-a-year", "testColumn");
    }
}
