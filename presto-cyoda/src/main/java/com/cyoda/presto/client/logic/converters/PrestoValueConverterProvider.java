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
import com.cyoda.presto.client.types.ComparableSupportedDataType;
import com.cyoda.presto.client.types.SupportedDataType;
import com.cyoda.presto.client.types.impl.BigDecimalDataType;
import com.cyoda.presto.client.types.impl.BigIntegerDataType;
import com.cyoda.presto.client.types.impl.BooleanDataType;
import com.cyoda.presto.client.types.impl.ByteArrayDataType;
import com.cyoda.presto.client.types.impl.ByteBufferDataType;
import com.cyoda.presto.client.types.impl.ByteDataType;
import com.cyoda.presto.client.types.impl.CharacterDataType;
import com.cyoda.presto.client.types.impl.ClassDataType;
import com.cyoda.presto.client.types.impl.DateDataType;
import com.cyoda.presto.client.types.impl.DoubleDataType;
import com.cyoda.presto.client.types.impl.FloatDataType;
import com.cyoda.presto.client.types.impl.IntegerDataType;
import com.cyoda.presto.client.types.impl.ListDataType;
import com.cyoda.presto.client.types.impl.LocalDateDataType;
import com.cyoda.presto.client.types.impl.LocalDateTimeDataType;
import com.cyoda.presto.client.types.impl.LocalTimeDataType;
import com.cyoda.presto.client.types.impl.LocaleDataType;
import com.cyoda.presto.client.types.impl.LongDataType;
import com.cyoda.presto.client.types.impl.MapDataType;
import com.cyoda.presto.client.types.impl.ObjectDataType;
import com.cyoda.presto.client.types.impl.SetDataType;
import com.cyoda.presto.client.types.impl.ShortDataType;
import com.cyoda.presto.client.types.impl.StringDataType;
import com.cyoda.presto.client.types.impl.UUIDDataType;
import com.cyoda.presto.client.types.impl.YearDataType;
import com.cyoda.presto.client.types.impl.YearMonthDataType;
import com.cyoda.presto.client.types.impl.ZonedDateTimeDataType;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;

import java.util.function.Supplier;

public class PrestoValueConverterProvider {

    private PrestoValueConverterProvider() {
    }

    private static final Supplier<PrestoValueConverters> CONVERTER_PROVIDER_SUPPLIER = Suppliers.memoize(
            PrestoValueConverterProvider::setupConverters)::get;


    public static <S> PrestoValueConverter<S> getPrestoValueConverter(SupportedDataType<S> supportedDataType) {
        return CONVERTER_PROVIDER_SUPPLIER.get().getPrestoValueConverter(supportedDataType);
    }

    public static <S extends Comparable<? super S>> ComparablePrestoValueConverter<S> getComparablePrestoValueConverter(
            ComparableSupportedDataType<S> supportedDataType
    ) {
        //noinspection unchecked
        return (ComparablePrestoValueConverter<S>) CONVERTER_PROVIDER_SUPPLIER.get().getComparablePrestoValueConverter(supportedDataType);
    }
    public static ComparablePrestoValueConverter<?> getComparablePrestoValueConverterU(
            ComparableSupportedDataType<?> supportedDataType
    ) {
        return CONVERTER_PROVIDER_SUPPLIER.get().getComparablePrestoValueConverter(supportedDataType);
    }

    private static PrestoValueConverters setupConverters() {
        ImmutableMap.Builder<SupportedDataType<?>, PrestoValueConverter<?>> builder = ImmutableMap.builder();
        builder.put(BigDecimalDataType.INSTANCE,new BigDecimalPrestoValueConverter());
        builder.put(BigIntegerDataType.INSTANCE,new BigIntegerPrestoValueConverter());
        builder.put(BooleanDataType.INSTANCE,new BooleanPrestoValueConverter());
        builder.put(ByteArrayDataType.INSTANCE,new ByteArrayPrestoValueConverter());
        builder.put(ByteBufferDataType.INSTANCE,new ByteBufferPrestoValueConverter());
        builder.put(ByteDataType.INSTANCE,new BytePrestoValueConverter());
        builder.put(CharacterDataType.INSTANCE,new CharacterPrestoValueConverter());
        builder.put(ClassDataType.INSTANCE,new ClassPrestoValueConverter());
        builder.put(DateDataType.INSTANCE,new DatePrestoValueConverter());
        builder.put(DoubleDataType.INSTANCE,new DoublePrestoValueConverter());
        builder.put(FloatDataType.INSTANCE,new FloatPrestoValueConverter());
        builder.put(IntegerDataType.INSTANCE,new IntegerPrestoValueConverter());
        builder.put(ListDataType.INSTANCE,new ListPrestoValueConverter());
        builder.put(LocalDateDataType.INSTANCE,new LocalDatePrestoValueConverter());
        builder.put(LocalDateTimeDataType.INSTANCE,new LocalDateTimePrestoValueConverter());
        builder.put(LocaleDataType.INSTANCE,new LocalePrestoValueConverter());
        builder.put(LocalTimeDataType.INSTANCE,new LocalTimePrestoValueConverter());
        builder.put(LongDataType.INSTANCE,new LongPrestoValueConverter());
        builder.put(MapDataType.INSTANCE,new MapPrestoValueConverter());
        builder.put(ObjectDataType.INSTANCE,new ObjectPrestoValueConverter());
        builder.put(SetDataType.INSTANCE,new SetPrestoValueConverter());
        builder.put(ShortDataType.INSTANCE,new ShortPrestoValueConverter());
        builder.put(StringDataType.INSTANCE,new StringPrestoValueConverter());
        builder.put(UUIDDataType.INSTANCE,new UUIDPrestoValueConverter());
        builder.put(YearMonthDataType.INSTANCE,new YearMonthPrestoValueConverter());
        builder.put(YearDataType.INSTANCE,new YearPrestoValueConverter());
        builder.put(ZonedDateTimeDataType.INSTANCE,new ZonedDateTimePrestoValueConverter());
        return new PrestoValueConverters(builder.build());
    }
}
