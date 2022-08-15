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

import com.cyoda.presto.client.types.DataType;

import java.util.Map;
import java.util.stream.Collectors;

public class PrestoValueConverters {

    private final Map<DataType, PrestoValueConverter<?>> converters;
    private final Map<DataType, ComparablePrestoValueConverter<?>> comparableConverters;

    public PrestoValueConverters(Map<DataType, PrestoValueConverter<?>> converters) {
        this.converters = converters;
        this.comparableConverters = converters.entrySet().stream()
                .filter(it->Comparable.class.isAssignableFrom(it.getKey().getJavaType()))
                .collect(Collectors.toMap(e->e.getKey(), e->(ComparablePrestoValueConverter<?>)e.getValue()
                ));
    }

    public <S> PrestoValueConverter<S> getPrestoValueConverter(DataType dataType) {
        //noinspection unchecked
        return (PrestoValueConverter<S>) converters.get(dataType);
    }

    public ComparablePrestoValueConverter<?> getComparablePrestoValueConverter(DataType dataType) {
        return comparableConverters.get(dataType);
    }
}
