/*
 * Copyright (C) 2022 Cyoda Ltd.
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

package com.cyoda.presto.client.logic.converters;

import com.cyoda.presto.client.logic.converters.impl.BigDecimalPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.BigIntegerPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.BooleanPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.ByteArrayPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.ByteBufferPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.BytePrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.CharacterPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.ClassPrestoValueConverter;
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
import com.cyoda.presto.client.logic.converters.impl.ObjectPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.SetPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.ShortPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.StringPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.UUIDPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.YearMonthPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.YearPrestoValueConverter;
import com.cyoda.presto.client.logic.converters.impl.ZonedDateTimePrestoValueConverter;
import com.cyoda.presto.client.types.DataType;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;

import java.util.function.Supplier;

public class PrestoValueConverterProvider {


    private PrestoValueConverterProvider() {
    }

    private static final Supplier<PrestoValueConverters> CONVERTER_PROVIDER_SUPPLIER = Suppliers.memoize(
            PrestoValueConverterProvider::setupConverters)::get;


    public static <S> PrestoValueConverter<S> getPrestoValueConverter(DataType dataType) {
        return CONVERTER_PROVIDER_SUPPLIER.get().getPrestoValueConverter(dataType);
    }

    public static ComparablePrestoValueConverter<?> getComparablePrestoValueConverterU(
            DataType dataType
    ) {
        return CONVERTER_PROVIDER_SUPPLIER.get().getComparablePrestoValueConverter(dataType);
    }

    private static PrestoValueConverters setupConverters() {
        ImmutableMap.Builder<DataType, PrestoValueConverter<?>> builder = ImmutableMap.builder();
        builder.put(DataType.BIG_DECIMAL,new BigDecimalPrestoValueConverter());
        builder.put(DataType.BIG_INTEGER,new BigIntegerPrestoValueConverter());
        builder.put(DataType.BOOLEAN,new BooleanPrestoValueConverter());
        builder.put(DataType.BYTE_ARRAY,new ByteArrayPrestoValueConverter());
        builder.put(DataType.BYTE_BUFFER,new ByteBufferPrestoValueConverter());
        builder.put(DataType.BYTE,new BytePrestoValueConverter());
        builder.put(DataType.CHARACTER,new CharacterPrestoValueConverter());
        builder.put(DataType.CLASS,new ClassPrestoValueConverter());
        builder.put(DataType.DATE,new DatePrestoValueConverter());
        builder.put(DataType.DOUBLE,new DoublePrestoValueConverter());
        builder.put(DataType.FLOAT,new FloatPrestoValueConverter());
        builder.put(DataType.INTEGER,new IntegerPrestoValueConverter());
        builder.put(DataType.LIST,new ListPrestoValueConverter());
        builder.put(DataType.LOCAL_DATE,new LocalDatePrestoValueConverter());
        builder.put(DataType.LOCAL_DATE_TIME,new LocalDateTimePrestoValueConverter());
        builder.put(DataType.LOCALE,new LocalePrestoValueConverter());
        builder.put(DataType.LOCAL_TIME,new LocalTimePrestoValueConverter());
        builder.put(DataType.LONG,new LongPrestoValueConverter());
        builder.put(DataType.MAP,new MapPrestoValueConverter());
        builder.put(DataType.OBJECT,new ObjectPrestoValueConverter());
        builder.put(DataType.SET,new SetPrestoValueConverter());
        builder.put(DataType.SHORT,new ShortPrestoValueConverter());
        builder.put(DataType.STRING,new StringPrestoValueConverter());
        builder.put(DataType.UUID_TYPE,new UUIDPrestoValueConverter());
        builder.put(DataType.YEAR_MONTH,new YearMonthPrestoValueConverter());
        builder.put(DataType.YEAR,new YearPrestoValueConverter());
        builder.put(DataType.ZONED_DATE_TIME,new ZonedDateTimePrestoValueConverter());
        return new PrestoValueConverters(builder.build());
    }
}
