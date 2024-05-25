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
import com.cyoda.presto.handles.CyodaColumnHandle;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Type;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import java.util.SortedSet;

public class BooleanPrestoValueConverter extends ComparableValueConverter<Boolean> {

    @Inject
    public BooleanPrestoValueConverter() {
        super(DataType.BOOLEAN);
    }

    @Override
    public void writeValue(Type type, BlockBuilder builder, @Nonnull Boolean value) {
        type.writeBoolean(builder, value);
    }

    @Override
    protected ColumnPredicate<Boolean> buildInListPredicate(CyodaColumnHandle column, SortedSet<Boolean> values) {
        // IN (true, false) predicates can be simplified to IS NOT NULL.
        if (values.size() > 1) {
            return ColumnPredicate.isNotNull(column);
        }
        return super.buildInListPredicate(column, values);
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
                    return ColumnPredicate.none(column);
                } else {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, true, null);
                }
            }
            case GREATER_EQUAL: {
                // b >= true  -> b = true
                // b >= false -> b IS NOT NULL
                if (value) {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, true, null);
                } else {
                    return ColumnPredicate.isNotNull(column);
                }
            }
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, value, null);
            case LESS: {
                // b < true  -> b NONE
                // b < false -> b = true
                if (value) {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, false, null);
                } else {
                    return ColumnPredicate.none(column);
                }
            }
            case LESS_EQUAL: {
                // b <= true  -> b IS NOT NULL
                // b <= false -> b = false
                if (value) {
                    return ColumnPredicate.isNotNull(column);
                } else {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, false, null);
                }
            }
            default:
                throw unsupportedComparison(column, op);
        }
    }

    @Override
    public Boolean fromPrestoNative(Object nativeValue) {
        return (Boolean) nativeValue;
    }
}
