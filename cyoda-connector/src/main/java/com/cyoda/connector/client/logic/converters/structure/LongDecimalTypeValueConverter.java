package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.logic.ColumnPredicate;
import com.cyoda.connector.client.types.IDataType;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.google.common.base.Preconditions;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Decimals;
import io.trino.spi.type.Int128;
import io.trino.spi.type.Type;

import javax.annotation.Nonnull;
import java.math.BigInteger;

public abstract class LongDecimalTypeValueConverter<T extends Comparable<? super T>> extends ComparableValueConverter<T>{

    public LongDecimalTypeValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    protected abstract Int128 toInt128(T value);
    protected abstract T fromInt128(Int128 value);

    @Override
    public void writeValue(Type type, BlockBuilder builder, @Nonnull T value) {
        type.writeObject(builder, toInt128(value));
    }

    @Override
    public T fromPrestoNative(Object nativeValue) {
        return fromInt128((Int128) nativeValue);
    }

    @Override
    public String toStringFromNative(Object nativeValue) {
        return fromPrestoNative(nativeValue).toString();
    }

    @Override
    protected ColumnPredicate<T> newComparisonPredicate(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, T value) {
        BigInteger bdValue = toInt128(value).toBigInteger();
        BigInteger minValue = Decimals.MIN_UNSCALED_DECIMAL.toBigInteger();
        BigInteger maxValue = Decimals.MAX_UNSCALED_DECIMAL.toBigInteger();
        Preconditions.checkArgument(bdValue.compareTo(maxValue) <= 0 && bdValue.compareTo(minValue) >= 0,
                "Decimal value out of range for %s column: %s",
                column.getDataType(), bdValue);

        if (op == ColumnPredicate.ComparisonOp.LESS_EQUAL) {
            if (bdValue.equals(maxValue)) {
                // If the value can't be incremented because it is at the top end of the
                // range, then substitute the predicate with an IS NOT NULL predicate.
                // This has the same effect as an inclusive upper bound on the maximum
                // value. If the column is not nullable then the IS NOT NULL predicate
                // is ignored.
                return ColumnPredicate.isNotNull(column);
            }
            bdValue = bdValue.add(BigInteger.ONE);
            op = ColumnPredicate.ComparisonOp.LESS;
        } else if (op == ColumnPredicate.ComparisonOp.GREATER) {
            if (bdValue.equals(maxValue)) {
                return ColumnPredicate.none(column);
            }
            bdValue = bdValue.add(BigInteger.ONE);
            op = ColumnPredicate.ComparisonOp.GREATER_EQUAL;
        }

        T wrapped = fromInt128(Int128.valueOf(bdValue));

        switch (op) {
            case GREATER_EQUAL:
                if (bdValue.equals(minValue)) {
                    return ColumnPredicate.isNotNull(column);
                } else if (bdValue.equals(maxValue)) {
                    return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, wrapped, null);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, wrapped, null);
            case EQUAL:
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, column, wrapped, null);
            case LESS:
                if (bdValue.equals(minValue)) {
                    return ColumnPredicate.none(column);
                }
                return new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE, column, null, wrapped);
            default:
                throw unsupportedComparison(column, op);
        }
    }
}
