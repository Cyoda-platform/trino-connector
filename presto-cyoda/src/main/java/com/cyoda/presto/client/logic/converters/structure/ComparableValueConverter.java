package com.cyoda.presto.client.logic.converters.structure;

import com.cyoda.presto.client.logic.ColumnPredicate;
import com.cyoda.presto.client.logic.ColumnPredicateUtils;
import com.cyoda.presto.client.types.IDataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.common.predicate.DiscreteValues;

public abstract class ComparableValueConverter<T extends Comparable<? super T>> extends SingleValueConverter<T>{

    public ComparableValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    protected abstract ColumnPredicate<T> newComparisonPredicate(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, T value);
    @Override
    public abstract T toObject(Object nativeValue);

    public final ColumnPredicate<T> newComparisonPredicateFromNative(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, Object nativeValue){
        return newComparisonPredicate(column, op, toObject(nativeValue));
    }

    public final ColumnPredicate<T> newComparisonPredicateFromJava(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op, T value){
        return newComparisonPredicate(column, op, value);
    }
    @Override
    public final ColumnPredicate<T> newInListPredicate(CyodaColumnHandle columnHandle, DiscreteValues discreteValues) {
        return ColumnPredicateUtils.newInListPredicate(columnHandle, discreteValues, getClazz());
    }


    //========= FINAL METHODS
    protected final ColumnPredicate<T> nonePredicate(CyodaColumnHandle column) {
        return new ColumnPredicate<>(ColumnPredicate.PredicateType.NONE, column, null, null);
    }
    protected final ColumnPredicate<T> allPredicate(CyodaColumnHandle column) {
        return new ColumnPredicate<>(ColumnPredicate.PredicateType.ALL, column, null, null);
    }
    protected final ColumnPredicate<T> notNullPredicate(CyodaColumnHandle column) {
        if (!column.getIsNullable()){
            return allPredicate(column);
        }
        return new ColumnPredicate<>(ColumnPredicate.PredicateType.IS_NOT_NULL, column, null, null);
    }
    protected final ColumnPredicate<T> isNullPredicate(CyodaColumnHandle column) {
        if (!column.getIsNullable()) {
            return nonePredicate(column);
        }
        return new ColumnPredicate<>(ColumnPredicate.PredicateType.IS_NULL, column, null, null);
    }



    protected final IllegalArgumentException unsupportedComparison(CyodaColumnHandle column, ColumnPredicate.ComparisonOp op){
        return new IllegalArgumentException("Unsupported comparison " + op + " for a field " + column.getColumnName() +
                "(" + getDataType() + ") ");
    }

}
