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

package com.cyoda.connector.client.logic.converters;

import com.cyoda.connector.client.treenode.dto.conditions.AbstractTrinoConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.Operation;
import com.cyoda.connector.client.treenode.dto.conditions.SimpleTrinoConditionDto;
import com.cyoda.connector.client.types.IDataType;
import com.cyoda.connector.logging.SupplierLogger;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.block.ValueBlock;
import io.trino.spi.predicate.NullableValue;
import io.trino.spi.type.Type;

import java.util.ArrayList;
import java.util.List;

public interface PrestoValueConverter<T> {

    SupplierLogger LOG = SupplierLogger.get(PrestoValueConverter.class);
    default String stringify(T value) {
        return value.toString();
    }

    default NullableValue toNullableValue(Type type, Object value){
        return new NullableValue(type, value);
    }
    default T fromPrestoNative(Object nativeValue){
        throw new UnsupportedOperationException("Condition pushdown is not supported for " + getDataType());
    }

    default String toStringFromNative(Object nativeValue){
        return fromPrestoNative(nativeValue).toString();
    }
    /**
     * This function is called from *element converter* to convert array block, because types of array blocks are different,
     * depending on type of element
     **/
    default List<T> blockToNativeList(Object nativeBlock, Type trinoType) {
        ValueBlock block = (ValueBlock) nativeBlock;
        List<T> values = new ArrayList<>();
        for (int i = 0; i < block.getPositionCount(); i++) {
            Object element = trinoType.getObject(block, i);
            values.add(fromPrestoNative(element));
        }
        return values;
    }

    default AbstractTrinoConditionDto toCondition(Operation operation, Type trinoType, Object nativeValue) {
        return new SimpleTrinoConditionDto(operation, toStringFromNative(nativeValue));
    }

    IDataType<T> getDataType();

    Class<T> getClazz();

    default T fromOtherCyodaType(Object value, String columnName){
        throw new UnsupportedOperationException(String.format("Error with field \"%s\": Conversion operation from %s to %s is not supported",
                columnName, value.getClass(), getClazz()));
    }

    void writeCyodaNative(Type type, BlockBuilder builder, Object cyodaNative, String columnName);
}
