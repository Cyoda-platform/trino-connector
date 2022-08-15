package com.cyoda.presto.client.logic.converters.structure;

import com.cyoda.presto.client.logic.ColumnPredicate;
import com.cyoda.presto.client.types.DataTypeValue;
import com.cyoda.presto.client.types.IDataType;
import com.cyoda.presto.client.util.DecimalUtil;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.google.common.base.Preconditions;

import java.math.BigDecimal;

public abstract class BigDecimalTypeValueConverter<T extends Comparable<? super T>> extends SliceComparableValueConverter<T>{

    public BigDecimalTypeValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    protected abstract BigDecimal toBigDecimal(T value);
    protected abstract T fromBigDecimal(BigDecimal value);

    @Override
    protected ColumnPredicate<T> newComparisonPredicate(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, T value) {
        BigDecimal bdValue = toBigDecimal(value);
        BigDecimal minValue = DecimalUtil.minValue(bdValue.precision(), bdValue.scale());
        BigDecimal maxValue = DecimalUtil.maxValue(bdValue.precision(), bdValue.scale());
        Preconditions.checkArgument(bdValue.compareTo(maxValue) <= 0 && bdValue.compareTo(minValue) >= 0,
                "Decimal value out of range for %s column: %s",
                column.getDataType(), bdValue);
        BigDecimal smallestValue = DecimalUtil.smallestValue(bdValue.scale());

        if (op == ColumnPredicate.ComparisonOp.LESS_EQUAL) {
            if (bdValue.equals(maxValue)) {
                // If the value can't be incremented because it is at the top end of the
                // range, then substitute the predicate with an IS NOT NULL predicate.
                // This has the same effect as an inclusive upper bound on the maximum
                // value. If the column is not nullable then the IS NOT NULL predicate
                // is ignored.
                return notNullPredicate(column);
            }
            bdValue = bdValue.add(smallestValue);
            op = ColumnPredicate.ComparisonOp.LESS;
        } else if (op == ColumnPredicate.ComparisonOp.GREATER) {
            if (bdValue.equals(maxValue)) {
                return nonePredicate(column);
            }
            bdValue = bdValue.add(smallestValue);
            op = ColumnPredicate.ComparisonOp.GREATER_EQUAL;
        }

        DataTypeValue<T> wrapped = DataTypeValue.of(fromBigDecimal(bdValue));

        switch (op) {
            case GREATER_EQUAL:
                if (bdValue.equals(minValue)) {
                    return notNullPredicate(column);
                } else if (bdValue.equals(maxValue)) {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, wrapped, null);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, wrapped, null);
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, wrapped, null);
            case LESS:
                if (bdValue.equals(minValue)) {
                    return nonePredicate(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, null, wrapped);
            default:
                throw unsupportedComparison(column, op);
        }
    }
}
