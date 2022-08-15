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

import com.cyoda.presto.client.logic.ColumnPredicate;
import com.cyoda.presto.client.logic.converters.structure.ComparableValueConverter;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.client.types.DataTypeValue;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.type.Type;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public class BooleanPrestoValueConverter extends ComparableValueConverter<Boolean> {

    @Inject
    public BooleanPrestoValueConverter() {
        super(DataType.BOOLEAN);
    }

    @Override
    protected void writeValueInternal(Type type, BlockBuilder builder, @Nonnull Boolean value) {
        type.writeBoolean(builder, value);
    }

    @Override
    protected ColumnPredicate<Boolean> newComparisonPredicate(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, Boolean value) {
        // Create the comparison predicate. Range predicates on boolean values can
        // always be converted to either an equality, an IS NOT NULL (filtering only
        // null values), or NONE (filtering all values).
        switch (op) {
            case GREATER: {
                // b > true  -> b NONE
                // b > false -> b = true
                if (value) {
                    return nonePredicate(column);
                } else {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, DataTypeValue.of(true), null);
                }
            }
            case GREATER_EQUAL: {
                // b >= true  -> b = true
                // b >= false -> b IS NOT NULL
                if (value) {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, DataTypeValue.of(true), null);
                } else {
                    return notNullPredicate(column);
                }
            }
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, DataTypeValue.of(value), null);
            case LESS: {
                // b < true  -> b NONE
                // b < false -> b = true
                if (value) {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, DataTypeValue.of(false), null);
                } else {
                    return nonePredicate(column);
                }
            }
            case LESS_EQUAL: {
                // b <= true  -> b IS NOT NULL
                // b <= false -> b = false
                if (value) {
                    return notNullPredicate(column);
                } else {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, DataTypeValue.of(false), null);
                }
            }
            default:
                throw unsupportedComparison(column, op);
        }
    }

    @Override
    public Boolean toObject(Object nativeValue) {
        return (Boolean) nativeValue;
    }
}
