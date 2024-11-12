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

import com.cyoda.connector.client.logic.ColumnPredicate;
import com.cyoda.connector.client.types.IDataType;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.logging.SupplierLogger;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.predicate.DiscreteValues;
import io.trino.spi.predicate.NullableValue;
import io.trino.spi.type.Type;

public interface PrestoValueConverter<T> {

    SupplierLogger LOG = SupplierLogger.get(PrestoValueConverter.class);
    String stringify(T value);

    default ColumnPredicate<?> newComparisonPredicateFromNative(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, Object nativeValue){
        throw new UnsupportedOperationException("Current method is not supported for " + getDataType());
    }
    default <C extends Comparable<C>> ColumnPredicate<C> newComparisonPredicateFromJava(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, C value){
        throw new UnsupportedOperationException("Current method is not supported for " + getDataType());
    }
    default ColumnPredicate<?> newInListPredicate(CyodaColumnHandle columnHandle, DiscreteValues discreteValues) {
        throw new UnsupportedOperationException("Current method is not supported for " + getDataType());
    }
    default NullableValue toNullableValue(Type type, Object value){
        return new NullableValue(type, value);
    }
    default T fromPrestoNative(Object nativeValue){
        throw new UnsupportedOperationException("Condition pushdown is not supported for " + getDataType());
    }
    default String toStringFromNative(Object nativeValue){
        return nativeValue.toString();
    }

    default boolean areConsecutive(T a, T b){
        return false;
    }

    IDataType<T> getDataType();

    Class<T> getClazz();

    default T fromOtherCyodaType(Object value, String columnName){
        throw new UnsupportedOperationException(String.format("Error with field \"%s\": Conversion operation from %s to %s is not supported",
                columnName, value.getClass(), getClazz()));
    }

    void writeCyodaNative(Type type, BlockBuilder builder, Object cyodaNative, String columnName);
}
