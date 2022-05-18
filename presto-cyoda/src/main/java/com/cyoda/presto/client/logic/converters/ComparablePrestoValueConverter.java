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
import com.cyoda.presto.client.logic.ColumnPredicateUtils;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.common.predicate.DiscreteValues;

public interface ComparablePrestoValueConverter<T extends Comparable<? super T>> extends PrestoValueConverter<T> {

    Class<T> getClazz();

    /****** predicate building *******/
    default ColumnPredicate<T> newInListPredicate(CyodaColumnHandle columnHandle, DiscreteValues discreteValues) {
        return ColumnPredicateUtils.newInListPredicate(columnHandle, discreteValues, getClazz());
    }
}
