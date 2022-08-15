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
import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.type.MapType;
import com.facebook.presto.common.type.Type;

import java.util.Iterator;
import java.util.Map;

public class MapPrestoValueConverter<K, V> extends MultiValueConverter<Map<K,V>, Map.Entry<K,V>, MapType> {

    private final SingleValueConverter<K> keyConverter;
    private final SingleValueConverter<V> valueConverter;
    public MapPrestoValueConverter(SingleValueConverter<K> keyConverter, SingleValueConverter<V> valueConverter){
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
        keyConverter.writeValue(keyType, elementBuilder, value.getKey());
        valueConverter.writeValue(valueType, elementBuilder, value.getValue());
    }


    @Override
    public Map<K, V> toObject(Object nativeValue) {
        throw new UnsupportedOperationException("no can do");
    }
}
