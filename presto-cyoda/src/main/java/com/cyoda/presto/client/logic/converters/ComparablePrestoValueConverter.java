///*
// * Copyright (C) 2022 Cyoda Ltd.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// */
//
//package com.cyoda.presto.client.logic.converters;
//
//import com.cyoda.presto.client.logic.ColumnPredicate;
//import com.cyoda.presto.client.types.DataType;
//import com.cyoda.presto.client.types.DataTypeValue;
//import com.cyoda.presto.handles.CyodaColumnHandle;
//import com.facebook.presto.common.predicate.DiscreteValues;
//
//import java.util.SortedSet;
//import java.util.TreeSet;
//import java.util.stream.Collectors;
//
//public interface ComparablePrestoValueConverter<T extends Comparable<? super T>> extends PrestoValueConverter<T> {
//
//    Class<T> getClazz();
//
//    /****** predicate building *******/
//    default ColumnPredicate<T> newInListPredicate(CyodaColumnHandle columnHandle, DiscreteValues discreteValues) {
//        SortedSet<DataTypeValue<T>> javaValues = discreteValues.getValues().stream()
//                .map(nativeValue -> DataTypeValue.ofPrestoNativeValue(columnHandle.getDataType(), nativeValue, getClazz()))
//                .sorted()
//                .collect(Collectors.toCollection(TreeSet::new));
//        if (javaValues.isEmpty()) {
//            return ColumnPredicate.none(columnHandle);
//        }
//        // IN (true, false) predicates can be simplified to IS NOT NULL.
//        if (columnHandle.getDataType() == DataType.BOOLEAN && javaValues.size() > 1) {
//            return ColumnPredicate.newIsNotNullPredicate(columnHandle);
//        }
//        return ColumnPredicate.buildInList(columnHandle, javaValues);
//    }
//}
