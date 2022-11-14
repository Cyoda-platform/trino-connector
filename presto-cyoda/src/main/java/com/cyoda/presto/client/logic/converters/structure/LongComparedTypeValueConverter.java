package com.cyoda.presto.client.logic.converters.structure;

import com.cyoda.presto.client.logic.ColumnPredicate;
import com.cyoda.presto.client.types.IDataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.google.common.base.Preconditions;

import java.time.Instant;

public abstract class LongComparedTypeValueConverter<T extends Comparable<? super T>> extends LongWrittenTypeValueConverter<T>{
    public LongComparedTypeValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    public abstract long minValueOfIntType();
    public abstract long maxValueOfIntType();

    @Override
    public boolean areConsecutive(T a, T b) {
        return toLong(b) - toLong(a) == 1;
    }

    @Override
    protected ColumnPredicate<T> newComparisonPredicate(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, T value) {
        long minValue = minValueOfIntType();
        long maxValue = maxValueOfIntType();
        long longValue = toLong(value);

        Preconditions.checkArgument(longValue <= maxValue && longValue >= minValue,
                "integer value out of range for %s column: %s",
                getDataType(), longValue);

        if (op == ColumnPredicate.ComparisonOp.LESS_EQUAL) {
            if (longValue == maxValue) {
                // If the value can't be incremented because it is at the top end of the
                // range, then substitute the predicate with an IS NOT NULL predicate.
                // This has the same effect as an inclusive upper bound on the maximum
                // value. If the column is not nullable then the IS NOT NULL predicate
                // is ignored.
                return ColumnPredicate.isNotNull(column);
            }
            longValue += 1;
            op = ColumnPredicate.ComparisonOp.LESS;
        } else if (op == ColumnPredicate.ComparisonOp.GREATER) {
            if (longValue == maxValue) {
                return ColumnPredicate.none(column);
            }
            longValue += 1;
            op = ColumnPredicate.ComparisonOp.GREATER_EQUAL;
        }

        T newValue = fromLong(longValue);
        switch (op) {
            case GREATER_EQUAL:
                if (longValue == minValue) {
                    return ColumnPredicate.isNotNull(column);
                } else if (longValue == maxValue) {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, newValue, null);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, newValue, null);
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, newValue, null);
            case LESS:
                if (longValue == minValue) {
                    return ColumnPredicate.none(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, null, newValue);
            default:
                throw unsupportedComparison(column, op);
        }
    }
}
