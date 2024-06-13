package com.cyoda.presto.client.logic.converters;

import com.cyoda.presto.client.logic.converters.impl.BigDecimalPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.BigIntegerPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.BooleanPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.ByteArrayPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.ByteBufferPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.BytePrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.CharacterPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.DatePrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.DoublePrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.FloatPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.IntegerPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.ListPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.LocalDatePrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.LocalDateTimePrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.LocalTimePrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.LocalePrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.LongPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.MapPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.SetPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.ShortPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.StringPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.UUIDPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.YearMonthPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.YearPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.ZonedDateTimePrestoValueConverter;
import com.cyoda.presto.client.logic.converters.structure.LongWrittenTypeValueConverter;
import com.cyoda.presto.client.logic.converters.structure.SliceComparableValueConverter;
import com.cyoda.presto.client.types.IDataType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.airlift.slice.Slice;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.*;

public class ValueHolder<T> {

    public static final Map<IDataType<?>, ValueHolder<?>> MAP = new HashMap<>();
    public final IDataType<T> dataType;
    public final PrestoValueConverter<T> converter;
    public final T exampleValue;

    static {
        Double random = Math.random()*10000.0;
        UUID uuid = UUID.randomUUID();
        byte[] bytes = uuid.toString().getBytes(StandardCharsets.UTF_8);
        new ValueHolder<>(new BigDecimalPrestoValueConverter(), BigDecimal.valueOf(random));
        new ValueHolder<>(new BigIntegerPrestoValueConverter(), BigInteger.valueOf(random.longValue()));
        new ValueHolder<>(new BooleanPrestoValueConverter(), true);
        new ValueHolder<>(new ByteArrayPrestoValueConverter(), bytes);
        new ValueHolder<>(new ByteBufferPrestoValueConverter(), ByteBuffer.wrap(bytes));
        new ValueHolder<>(new BytePrestoValueConverter(), random.byteValue());
        new ValueHolder<>(new CharacterPrestoValueConverter(), 'F');
        new ValueHolder<>(new DatePrestoValueConverter(), new Date());
        new ValueHolder<>(new DoublePrestoValueConverter(), random);
        new ValueHolder<>(new FloatPrestoValueConverter(), random.floatValue());
        new ValueHolder<>(new IntegerPrestoValueConverter(), random.intValue());
        new ValueHolder<>(new ListPrestoValueConverter<>("strList",new StringPrestoValueConverter()),
                ImmutableList.of("hello","goodbye"));
        new ValueHolder<>(new LocalDatePrestoValueConverter(), LocalDate.now());
        new ValueHolder<>(new LocalDateTimePrestoValueConverter(), LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        new ValueHolder<>(new LocalePrestoValueConverter(), Locale.CANADA);
        new ValueHolder<>(new LocalTimePrestoValueConverter(), LocalTime.now());
        new ValueHolder<>(new LongPrestoValueConverter(), random.longValue());
        new ValueHolder<>(new MapPrestoValueConverter<>("map",
                new IntegerPrestoValueConverter(),
                new StringPrestoValueConverter()),
                new HashMap<Integer, String>(){{
                    put(1,"hello");
                    put(2, "goodbye");
                }}
        );
        //TODO ObjectValueConverter
        new ValueHolder<>(new SetPrestoValueConverter<>("strSet",new StringPrestoValueConverter()),
                ImmutableSet.of("hello","goodbye"));
        new ValueHolder<>(new ShortPrestoValueConverter(), random.shortValue());
        new ValueHolder<>(new StringPrestoValueConverter(), uuid.toString());
        new ValueHolder<>(new UUIDPrestoValueConverter(), uuid);
        new ValueHolder<>(new YearMonthPrestoValueConverter(), YearMonth.now());
        new ValueHolder<>(new YearPrestoValueConverter(), Year.now());
        new ValueHolder<>(new ZonedDateTimePrestoValueConverter(), ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS));
    }

    public ValueHolder(PrestoValueConverter<T> converter, T exampleValue) {
        this.dataType = converter.getDataType();
        this.converter = converter;
        this.exampleValue = exampleValue;
        MAP.put(dataType, this);

    }

    public String testStringify(){
        return converter.stringify(exampleValue);
    }

    public <C extends Comparable<C>> boolean testSliceConversions(){
        C converted;
        if (converter instanceof SliceComparableValueConverter){
            Slice slice = ((SliceComparableValueConverter<C>) converter).toSlice((C)exampleValue);
            converted = ((SliceComparableValueConverter<C>) converter).fromSlice(slice);
            assertEquals(exampleValue, converted, String.valueOf(converter.getDataType()));
            return true;
        }
        return false;
    }

    public <C extends Comparable<C>> boolean testLongConversions(){
        C converted;
        if (converter instanceof LongWrittenTypeValueConverter){
            long l = ((LongWrittenTypeValueConverter<C>) converter).toLong((C) exampleValue);
            converted = ((LongWrittenTypeValueConverter<C>) converter).fromLong(l);
            System.out.println(exampleValue + " -> " + converted);
            assertEquals(exampleValue.toString(), converted.toString(), String.valueOf(converter.getDataType()));
            return true;
        }
        return false;
    }
}
