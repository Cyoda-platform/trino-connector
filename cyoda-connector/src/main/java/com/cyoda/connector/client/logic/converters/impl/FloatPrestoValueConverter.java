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

package com.cyoda.connector.client.logic.converters.impl;

import com.cyoda.connector.client.logic.ColumnPredicate;
import com.cyoda.connector.client.logic.converters.structure.LongWrittenTypeValueConverter;
import com.cyoda.connector.client.types.DataType;
import com.cyoda.connector.handles.CyodaColumnHandle;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;

import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;

public class FloatPrestoValueConverter extends LongWrittenTypeValueConverter<Float> {

    @Inject
    public FloatPrestoValueConverter() {
        super(DataType.FLOAT);
    }

    @Override
    public Long toLong(@Nonnull Float value) {
        return (long) floatToRawIntBits(value);
    }

    @Override
    public Float fromPrestoNative(Object nativeValue) {
        return (Float) nativeValue;
    }

    @Nonnull
    @Override
    public Float fromLong(Long value) {
        return intBitsToFloat(value.intValue());
    }

    @Override
    public boolean areConsecutive(Float a, Float b) {
        return Math.nextAfter(a, Float.POSITIVE_INFINITY) == b;
    }

    @Override
    protected ColumnPredicate<Float> newComparisonPredicate(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, Float value) {
        if (op == ColumnPredicate.ComparisonOp.LESS_EQUAL) {
            if (value == Float.POSITIVE_INFINITY) {
                return ColumnPredicate.isNotNull(column);
            }
            value = Math.nextAfter(value, Float.POSITIVE_INFINITY);
            op = ColumnPredicate.ComparisonOp.LESS;
        } else if (op == ColumnPredicate.ComparisonOp.GREATER) {
            if (value == Float.POSITIVE_INFINITY) {
                return ColumnPredicate.none(column);
            }
            value = Math.nextAfter(value, Float.POSITIVE_INFINITY);
            op = ColumnPredicate.ComparisonOp.GREATER_EQUAL;
        }


        switch (op) {
            case GREATER_EQUAL:
                if (value == Float.NEGATIVE_INFINITY) {
                    return ColumnPredicate.isNotNull(column);
                } else if (value == Float.POSITIVE_INFINITY) {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, value, null);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, value, null);
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, value, null);
            case LESS:
                if (value == Float.NEGATIVE_INFINITY) {
                    return ColumnPredicate.none(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, null, value);
            default:
                throw unsupportedComparison(column, op);
        }
    }

    @Override
    public Float fromOtherCyodaType(Object value, String columnName) {
        return ((Number) value).floatValue();
    }
}
