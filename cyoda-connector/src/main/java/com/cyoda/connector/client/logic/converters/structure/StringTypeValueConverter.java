package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.logic.ColumnPredicate;
import com.cyoda.connector.client.types.IDataType;
import com.cyoda.connector.handles.CyodaColumnHandle;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class StringTypeValueConverter<T extends Comparable<? super T>> extends SliceComparableValueConverter<T>{

    public StringTypeValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    protected abstract T fromStr(String value);
    protected abstract String toStr(T value);

    @Override
    public String stringify(T value) {
        return "\"" + toStr(value) + "\"";
    }

    @Override
    protected ColumnPredicate<T> newComparisonPredicate(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, T value) {
        String strValue = toStr(value);
        byte[] bytes = strValue.getBytes(UTF_8);

        if (op == ColumnPredicate.ComparisonOp.LESS_EQUAL) {
            bytes = Arrays.copyOf(bytes, bytes.length + 1);
            op = ColumnPredicate.ComparisonOp.LESS;
        } else if (op == ColumnPredicate.ComparisonOp.GREATER) {
            bytes = Arrays.copyOf(bytes, bytes.length + 1);
            op = ColumnPredicate.ComparisonOp.GREATER_EQUAL;
        }

        T newValue = fromStr(new String(bytes, UTF_8));

        switch (op) {
            case GREATER_EQUAL:
                if (bytes.length == 0) {
                    return ColumnPredicate.isNotNull(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, newValue, null);
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, newValue, null);
            case LESS:
                if (bytes.length == 0) {
                    return ColumnPredicate.none(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, null, newValue);
            default:
                throw unsupportedComparison(column, op);
        }
    }
}
