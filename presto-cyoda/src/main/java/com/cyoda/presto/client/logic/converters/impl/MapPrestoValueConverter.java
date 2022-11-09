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

package com.cyoda.presto.client.logic.converters.impl;

import com.cyoda.presto.client.logic.converters.structure.MultiValueConverter;
import com.cyoda.presto.client.logic.converters.structure.SingleValueConverter;
import com.cyoda.presto.client.types.DataType;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.MapType;
import io.trino.spi.type.Type;

import java.util.Iterator;
import java.util.Map;

public class MapPrestoValueConverter<K, V> extends MultiValueConverter<Map<K,V>, Map.Entry<K,V>, MapType> {

    private final SingleValueConverter<K> keyConverter;
    private final SingleValueConverter<V> valueConverter;
    public MapPrestoValueConverter(String columnName, SingleValueConverter<K> keyConverter, SingleValueConverter<V> valueConverter){
        super(DataType.MAP, columnName);
        this.keyConverter = keyConverter;
        this.valueConverter = valueConverter;
    }

    @Override
    protected Iterator<Map.Entry<K, V>> getIterator(Map<K, V> value) {
        return value.entrySet().iterator();
    }

    @Override
    protected String stringifyElement(Map.Entry<K, V> element) {
        return "(" + keyConverter.stringify(element.getKey()) + ", " + valueConverter.stringify(element.getValue()) + ")";
    }

    @Override
    protected void writeElement(MapType type, BlockBuilder elementBuilder, Map.Entry<K, V> value) {
        Type keyType = type.getKeyType();
        Type valueType = type.getValueType();
        keyConverter.writeCyodaNativeFromCollection(keyType, elementBuilder, value.getKey(), getColumnName());
        valueConverter.writeCyodaNativeFromCollection(valueType, elementBuilder, value.getValue(), getColumnName());
    }

}
