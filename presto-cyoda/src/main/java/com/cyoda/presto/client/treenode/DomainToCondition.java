package com.cyoda.presto.client.treenode;

import com.cyoda.core.conditions.AbstractCondition;
import com.cyoda.core.conditions.GroupCondition;
import com.cyoda.core.conditions.RangeCondition;
import com.cyoda.core.conditions.queryable.Equals;
import com.cyoda.core.conditions.queryable.GreaterThan;
import com.cyoda.core.conditions.queryable.GreaterThanEquals;
import com.cyoda.core.conditions.queryable.LessThan;
import com.cyoda.core.conditions.queryable.LessThanEquals;
import com.cyoda.presto.client.logic.converters.structure.ComparableValueConverter;
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
        GroupCondition result = new GroupCondition(GroupCondition.Operator.AND);
        for (Map.Entry<ColumnHandle, Domain> byColumn : tupleDomain.getDomains().get().entrySet()){
            Domain domain = byColumn.getValue();
            CyodaColumnHandle column = (CyodaColumnHandle) byColumn.getKey();
            ComparableValueConverter converter = (ComparableValueConverter) column.getConverter();
            ValueSet valueSet = domain.getValues();
            List<AbstractCondition> conditions = new ArrayList<>();
            GroupCondition fieldCondition = valueSet.getValuesProcessor().transform(ranges -> {
                for (Range range : ranges.getOrderedRanges()) {
                    if (range.isSingleValue()) {
                        conditions.add(new Equals(column.getExternalName(), converter.fromPrestoNative(range.getHighValue().get()), false));
                    } else {
                        if (!range.isHighUnbounded()) {
                            RangeCondition condition;
                            String fieldName = column.getExternalName();
                            if (range.isHighInclusive()) {
                                condition = new LessThanEquals(fieldName, converter.fromPrestoNative(range.getHighValue().get()), false);
                            } else {
                                condition = new LessThan(fieldName, converter.fromPrestoNative(range.getHighValue().get()), false);
                            }
                            conditions.add(condition);
                        }
                        if (!range.isLowUnbounded()) {
                            RangeCondition condition;
                            String fieldName = column.getExternalName();
                            if (range.isLowInclusive()) {
                                condition = new GreaterThanEquals(fieldName, converter.fromPrestoNative(range.getLowValue().get()), false);
                            } else {
                                condition = new GreaterThan(fieldName, converter.fromPrestoNative(range.getLowValue().get()), false);
                            }
                            conditions.add(condition);
                        }
                    }
                }
                return new GroupCondition(GroupCondition.Operator.OR, conditions.toArray(AbstractCondition[]::new));
            }, discreteValues -> {
                for (Object value : discreteValues.getValues()){
                    conditions.add(new Equals(column.getExternalName(), converter.fromPrestoNative(value), false));
                }
                return new GroupCondition(GroupCondition.Operator.OR, conditions.toArray(AbstractCondition[]::new));
            }, ignored -> null);
            result.addCondition(fieldCondition);
        }
        return result;
    }

}
