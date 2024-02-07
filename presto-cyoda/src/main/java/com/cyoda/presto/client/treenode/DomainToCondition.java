package com.cyoda.presto.client.treenode;

import com.cyoda.core.conditions.AbstractCondition;
import com.cyoda.core.conditions.GroupCondition;
import com.cyoda.core.conditions.RangeCondition;
import com.cyoda.core.conditions.queryable.Equals;
import com.cyoda.core.conditions.queryable.GreaterThan;
import com.cyoda.core.conditions.queryable.GreaterThanEquals;
import com.cyoda.core.conditions.queryable.LessThan;
import com.cyoda.core.conditions.queryable.LessThanEquals;
import com.cyoda.presto.handles.CyodaColumnHandle;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.predicate.Domain;
import io.trino.spi.predicate.Range;
import io.trino.spi.predicate.TupleDomain;
import io.trino.spi.predicate.ValueSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DomainToCondition {
    public static GroupCondition convert(TupleDomain<ColumnHandle> tupleDomain){
        GroupCondition result = GroupCondition.ALL;
        for (Map.Entry<ColumnHandle, Domain> byColumn : tupleDomain.getDomains().get().entrySet()){
            Domain domain = byColumn.getValue();
            CyodaColumnHandle column = (CyodaColumnHandle) byColumn.getKey();
            ValueSet valueSet = domain.getValues();
            List<AbstractCondition> conditions = new ArrayList<>();
            GroupCondition fieldCondition = valueSet.getValuesProcessor().transform(ranges -> {
                for (Range range : ranges.getOrderedRanges()) {
                    if (range.isSingleValue()) {
                        conditions.add(new Equals(column.getExternalName(), (Comparable) range.getHighValue().get(), false));
                    } else {
                        if (!range.isHighUnbounded())
                            conditions.add(lessCondition(range, column.getExternalName()));
                        if (!range.isLowUnbounded())
                            conditions.add(greaterCondition(range, column.getExternalName()));
                    }
                }
                return new GroupCondition(GroupCondition.Operator.OR, conditions.toArray(AbstractCondition[]::new));
            }, discreteValues -> {
                for (Object value : discreteValues.getValues()){
                    conditions.add(new Equals(column.getExternalName(), (Comparable) value, false));
                }
                return new GroupCondition(GroupCondition.Operator.OR, conditions.toArray(AbstractCondition[]::new));
            }, ignored -> null);
            result.addCondition(fieldCondition);
        }
        return result;
    }

    private static RangeCondition greaterCondition(Range range, String fieldName){
        if (range.isLowInclusive()) {
            return new GreaterThanEquals(fieldName, (Comparable) range.getLowValue().get(), false);
        } else {
            return new GreaterThan(fieldName, (Comparable) range.getLowValue().get(), false);
        }
    }
    private static RangeCondition lessCondition(Range range, String fieldName){
        if (range.isHighInclusive()) {
            return new LessThanEquals(fieldName, (Comparable) range.getHighValue().get(), false);
        } else {
            return new LessThan(fieldName, (Comparable) range.getHighValue().get(), false);
        }
    }
}
