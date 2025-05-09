package com.cyoda.connector.client.logic.converters.impl;

import com.cyoda.connector.client.types.DataType;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class YearMonthPrestoValueConverterTest {

    private YearMonthPrestoValueConverter converter;

    @BeforeMethod
    public void setUp() {
        converter = new YearMonthPrestoValueConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(converter);
        assertEquals(converter.getDataType(), DataType.YEAR_MONTH);
        assertEquals(converter.getClazz(), YearMonth.class);
    }

    @Test
    public void testToInt() {
        YearMonth testValue = YearMonth.of(2023, 1);
        Integer result = converter.toInt(testValue);
        
        // The epoch day for the end of month (2023-01-31)
        long expectedEpochDay = testValue.atDay(1).toEpochDay();
        assertEquals(result.intValue(), expectedEpochDay);
    }

    @Test
    public void testFromInt() {
        // Create a YearMonth
        YearMonth expectedYearMonth = YearMonth.of(2023, 1);
        // Get the epoch day for the end of month
        int epochDay = (int) expectedYearMonth.atDay(1).toEpochDay();
        
        YearMonth result = converter.fromInt(epochDay);
        
        assertEquals(result, expectedYearMonth);
    }

    @Test
    public void testWriteValue() {
        YearMonth testValue = YearMonth.of(2023, 1);
        Type type = mock(Type.class);
        BlockBuilder blockBuilder = mock(BlockBuilder.class);
        
        converter.writeValue(type, blockBuilder, testValue);
        
        verify(type).writeLong(blockBuilder, testValue.atDay(1).toEpochDay());
    }

    @Test
    public void testFromPrestoNative() {
        YearMonth expectedYearMonth = YearMonth.of(2023, 1);
        int epochDay = (int) expectedYearMonth.atDay(1).toEpochDay();
        
        YearMonth result = converter.fromPrestoNative((long) epochDay);
        
        assertEquals(result, expectedYearMonth);
    }

    @Test
    public void testFromOtherCyodaType() {
        // Test with year-month string
        String yearMonthStr = "2023-01";
        YearMonth result = converter.fromOtherCyodaType(yearMonthStr, "testColumn");
        
        assertEquals(result, YearMonth.of(2023, 1));
    }

    @Test
    public void testStringify() {
        YearMonth testValue = YearMonth.of(2023, 1);
        
        String result = converter.stringify(testValue);
        
        assertEquals(result, "2023-01");
    }
    
    @Test
    public void testConsecutiveMonths() {
        // Test consecutive months to ensure proper conversion
        YearMonth ym1 = YearMonth.of(2023, 1);
        YearMonth ym2 = YearMonth.of(2023, 2);
        
        Integer int1 = converter.toInt(ym1);
        Integer int2 = converter.toInt(ym2);
        
        // The second month should have a higher epoch day
        assertTrue(int2 > int1);
        
        // Converting back should give the original values
        assertEquals(converter.fromInt(int1), ym1);
        assertEquals(converter.fromInt(int2), ym2);
    }
    
    @Test(expectedExceptions = NumberFormatException.class)
    public void testFromOtherCyodaTypeWithInvalidFormat() {
        converter.fromOtherCyodaType("not-a-year-month", "testColumn");
    }
}
