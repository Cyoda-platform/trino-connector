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
import com.cyoda.presto.client.logic.converters.structure.LongWrittenTypeValueConverter;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Type;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.math.BigDecimal;

public class DoublePrestoValueConverter extends LongWrittenTypeValueConverter<Double> {

    @Inject
    public DoublePrestoValueConverter() {
        super(DataType.DOUBLE);
    }

    public long toLong(@Nonnull Double value) {
        return Double.doubleToLongBits(value);
    }

    @Nonnull
    public Double fromLong(long value) {
        return Double.longBitsToDouble(value);
    }

    @Override
    public boolean areConsecutive(Double a, Double b) {
        return Math.nextAfter(a, Double.POSITIVE_INFINITY) == b;
    }

    @Override
    protected ColumnPredicate<Double> newComparisonPredicate(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, Double value) {
        if (op == ColumnPredicate.ComparisonOp.LESS_EQUAL) {
            if (value == Double.POSITIVE_INFINITY) {
                return ColumnPredicate.isNotNull(column);
            }
            value = Math.nextAfter(value, Double.POSITIVE_INFINITY);
            op = ColumnPredicate.ComparisonOp.LESS;
        } else if (op == ColumnPredicate.ComparisonOp.GREATER) {
            if (value == Double.POSITIVE_INFINITY) {
                return ColumnPredicate.none(column);
            }
            value = Math.nextAfter(value, Double.POSITIVE_INFINITY);
            op = ColumnPredicate.ComparisonOp.GREATER_EQUAL;
        }

        switch (op) {
            case GREATER_EQUAL:
                if (value == Double.NEGATIVE_INFINITY) {
                    return ColumnPredicate.isNotNull(column);
                } else if (value == Double.POSITIVE_INFINITY) {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, value, null);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, value, null);
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, value, null);
            case LESS:
                if (value == Double.NEGATIVE_INFINITY) {
                    return ColumnPredicate.none(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, null, value);
            default:
                throw unsupportedComparison(column, op);
        }
    }

    @Override
    public void writeValue(Type type, BlockBuilder builder, @Nonnull Double value) {
        type.writeDouble(builder, value);
    }

    @Override
    public Double fromOtherCyodaType(Object value, String columnName) {
        return ((BigDecimal)value).doubleValue();
    }
}
