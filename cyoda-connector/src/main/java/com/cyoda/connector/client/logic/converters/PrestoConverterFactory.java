package com.cyoda.connector.client.logic.converters;

import com.cyoda.connector.client.logic.converters.impl.BigDecimalPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.BigIntegerPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.BooleanPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.ByteArrayPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.ByteBufferPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.BytePrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.CharacterPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.DatePrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.DoublePrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.FloatPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.IntegerPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.ListPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.LocalDatePrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.LocalDateTimePrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.LocalTimePrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.LongPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.MapPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.ObjectPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.SetPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.ShortPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.StringPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.UUIDPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.YearMonthPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.YearPrestoValueConverter;
import com.cyoda.connector.client.logic.converters.impl.ZonedDateTimePrestoValueConverter;
import com.cyoda.connector.client.logic.converters.structure.ComparableValueConverter;
import com.cyoda.connector.client.logic.converters.structure.SingleValueConverter;
import com.cyoda.connector.client.types.DataType;
import com.cyoda.connector.client.types.IDataType;
import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.Map;

public class PrestoConverterFactory {
    private static final Map<DataType, ComparableValueConverter<?>> comparableValueConverters;
    private static final Map<DataType, SingleValueConverter<?>> singleValueConverters;
    static {
        ImmutableMap.Builder<DataType, ComparableValueConverter<?>> comparableBuilder = ImmutableMap.builder();
        comparableBuilder.put(DataType.BIG_DECIMAL,new BigDecimalPrestoValueConverter());
        comparableBuilder.put(DataType.UNBOUND_DECIMAL, new StringPrestoValueConverter());
        comparableBuilder.put(DataType.BIG_INTEGER,new BigIntegerPrestoValueConverter());
        comparableBuilder.put(DataType.UNBOUND_INTEGER, new StringPrestoValueConverter());
        comparableBuilder.put(DataType.BOOLEAN,new BooleanPrestoValueConverter());
        comparableBuilder.put(DataType.BYTE_BUFFER,new ByteBufferPrestoValueConverter());
        comparableBuilder.put(DataType.BYTE,new BytePrestoValueConverter());
        comparableBuilder.put(DataType.CHARACTER,new CharacterPrestoValueConverter());
        comparableBuilder.put(DataType.DATE,new DatePrestoValueConverter());
        comparableBuilder.put(DataType.DOUBLE,new DoublePrestoValueConverter());
        comparableBuilder.put(DataType.FLOAT,new FloatPrestoValueConverter());
        comparableBuilder.put(DataType.INTEGER,new IntegerPrestoValueConverter());
        comparableBuilder.put(DataType.LOCAL_DATE,new LocalDatePrestoValueConverter());
        comparableBuilder.put(DataType.LOCAL_DATE_TIME,new LocalDateTimePrestoValueConverter());
        comparableBuilder.put(DataType.LOCAL_TIME,new LocalTimePrestoValueConverter());
        comparableBuilder.put(DataType.LONG,new LongPrestoValueConverter());
        comparableBuilder.put(DataType.SHORT,new ShortPrestoValueConverter());
        comparableBuilder.put(DataType.STRING,new StringPrestoValueConverter());
        UUIDPrestoValueConverter uuidConverter = new UUIDPrestoValueConverter();
        comparableBuilder.put(DataType.UUID_TYPE, uuidConverter);
        comparableBuilder.put(DataType.TIME_UUID_TYPE, uuidConverter);
        comparableBuilder.put(DataType.YEAR_MONTH,new YearMonthPrestoValueConverter());
        comparableBuilder.put(DataType.YEAR,new YearPrestoValueConverter());
        comparableBuilder.put(DataType.ZONED_DATE_TIME,new ZonedDateTimePrestoValueConverter());
        comparableValueConverters = comparableBuilder.build();

        ImmutableMap.Builder<DataType, SingleValueConverter<?>> singleBuilder = ImmutableMap.builder();
        singleBuilder.putAll(comparableValueConverters);
        singleBuilder.put(DataType.BYTE_ARRAY,new ByteArrayPrestoValueConverter());
        singleBuilder.put(DataType.OBJECT,new ObjectPrestoValueConverter());
        singleValueConverters = singleBuilder.build();

    }

    public static <T extends Comparable<T>> ComparableValueConverter<T> getComparableConverter(IDataType<T> dataType){
        ComparableValueConverter<T> res = (ComparableValueConverter<T>)comparableValueConverters.get(dataType);
        if (res == null) throw new IllegalArgumentException("There is no comparable converter for " + dataType + " type.");
        return res;
    }

    public static <T> SingleValueConverter<T> getSingleValueConverter(IDataType<T> dataType){
        SingleValueConverter<T> res = (SingleValueConverter<T>) singleValueConverters.get(dataType);
        if (res == null) throw new IllegalArgumentException("There is no single value converter for " + dataType + " type.");
        return res;
    }

    public static PrestoValueConverter<?> getConverter(DataType mainType, DataType[] typeParams, String columnName){
        if (typeParams == null || typeParams.length == 0)
            return getSingleValueConverter(mainType);
        switch (mainType){
            case LIST:
                return new ListPrestoValueConverter<>(columnName, getSingleValueConverter(typeParams[0]));
            case SET:
                return new SetPrestoValueConverter<>(columnName, getSingleValueConverter(typeParams[0]));
            case MAP:
                return new MapPrestoValueConverter<>(columnName, getSingleValueConverter(typeParams[0]),getSingleValueConverter(typeParams[1]));
            default:
                throw new IllegalArgumentException(String.format("Failed to get converter for column \"%s\"(%s[%s])",
                        columnName, mainType, Arrays.toString(typeParams)));
        }
    }
}
