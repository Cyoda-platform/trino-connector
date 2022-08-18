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

import com.cyoda.presto.client.logic.ColumnPredicate;
import com.cyoda.presto.client.types.IDataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.predicate.DiscreteValues;
import com.facebook.presto.common.type.Type;

public interface PrestoValueConverter<T> {

    SupplierLogger LOG = SupplierLogger.get(PrestoValueConverter.class);
    String stringify(T value);


    /****** conversion to/from long *******/
//    default long toLong(@Nonnull T value) {
//        throw new UnsupportedOperationException("Current method is not supported for " + getDataType());
//    }
//
//    default @Nonnull T fromLong(long value) {
//        throw new UnsupportedOperationException("Current method is not supported for " + getDataType());
//    }

    /****** conversion to/from Slice *******/
//    default Slice toSlice(@Nonnull T value) {
//        throw new UnsupportedOperationException("Current method is not supported for " + getDataType());
//    }
//
//    default @Nonnull T fromSlice(Slice value) {
//        throw new UnsupportedOperationException("Current method is not supported for " + getDataType());
//    }
//
//    T toObject(Object nativeValue);

    default ColumnPredicate<?> newComparisonPredicateFromNative(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, Object nativeValue){
        throw new UnsupportedOperationException("Current method is not supported for " + getDataType());
    }
    default <C extends Comparable<C>> ColumnPredicate<C> newComparisonPredicateFromJava(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, C value){
        throw new UnsupportedOperationException("Current method is not supported for " + getDataType());
    }
    default ColumnPredicate<?> newInListPredicate(CyodaColumnHandle columnHandle, DiscreteValues discreteValues) {
        throw new UnsupportedOperationException("Current method is not supported for " + getDataType());
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
