package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.logic.ColumnPredicate;
import com.cyoda.connector.client.types.IDataType;
import com.cyoda.connector.handles.CyodaColumnHandle;
import io.trino.spi.predicate.DiscreteValues;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public abstract class ComparableValueConverter<T extends Comparable<? super T>> extends SingleValueConverter<T>{

    public ComparableValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    protected abstract ColumnPredicate<T> newComparisonPredicate(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, T value);

    @Override
    public abstract T fromPrestoNative(Object nativeValue);

    public final ColumnPredicate<T> newComparisonPredicateFromNative(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, Object nativeValue){
        return newComparisonPredicate(column, op, fromPrestoNative(nativeValue));
    }

    public final <C extends Comparable<C>> ColumnPredicate<C> newComparisonPredicateFromJava(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, C value){
        // assume T == C
        return (ColumnPredicate<C>)newComparisonPredicate(column, op, (T)value);
    }
    @Override
    public final ColumnPredicate<T> newInListPredicate(CyodaColumnHandle columnHandle, DiscreteValues discreteValues) {
        SortedSet<T> javaValues = discreteValues.getValues().stream()
                .map(this::fromPrestoNative)
                .sorted()
                .collect(Collectors.toCollection(TreeSet::new));
        if (javaValues.isEmpty()) {
            return ColumnPredicate.none(columnHandle);
        }

        return buildInListPredicate(columnHandle, javaValues);
    }

    protected ColumnPredicate<T> buildInListPredicate(CyodaColumnHandle column, SortedSet<T> values){
        return ColumnPredicate.buildInList(column, values);
    }

    protected final IllegalArgumentException unsupportedComparison(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op){
        return new IllegalArgumentException("Unsupported comparison " + op + " for a field " + column.getColumnName() +
                "(" + getDataType() + ") ");
    }

}
