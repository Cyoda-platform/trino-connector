package com.cyoda.connector.client.treenode;

import com.cyoda.core.conditions.AbstractCondition;
import com.cyoda.core.conditions.GroupCondition;
import com.cyoda.core.conditions.RangeCondition;
import com.cyoda.core.conditions.queryable.Equals;
import com.cyoda.core.conditions.queryable.GreaterThan;
import com.cyoda.core.conditions.queryable.GreaterThanEquals;
import com.cyoda.core.conditions.queryable.LessThan;
import com.cyoda.core.conditions.queryable.LessThanEquals;
import com.cyoda.connector.client.logic.converters.structure.ComparableValueConverter;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.predicate.Domain;
import io.trino.spi.predicate.Range;
import io.trino.spi.predicate.TupleDomain;
import io.trino.spi.predicate.ValueSet;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;

public class DomainToCondition {

    private DomainToCondition() {
        // Utility class
    }

    public static Object extractSingleEquals(TupleDomain<ColumnHandle> tupleDomain, CyodaColumnHandle columnName){
        if (tupleDomain.getDomains().isEmpty()) return null;
        Domain domain = tupleDomain.getDomains().get().get(columnName);
        if (domain == null || domain.getValues() == null) return null;
        ComparableValueConverter converter = (ComparableValueConverter) columnName.getConverter();
        Object conditionValue = domain.getValues().getValuesProcessor().transform(ranges -> {
            if (ranges.getRangeCount() > 1) throw new RuntimeException("Only single value conditions are allowed for field " + columnName.getColumnName());
            Range range = ranges.getOrderedRanges().getFirst();
                if (range.isSingleValue()) {
                     return range.getHighValue().map(it -> converter.fromPrestoNative(it)).orElse(null);
                } else {
                    throw new RuntimeException("Only single value conditions are allowed for field " + columnName.getColumnName());
                }
        }, discreteValues -> {
            Collection<Object> values = discreteValues.getValues();
            if (values.size() > 1) throw new RuntimeException("Only single value conditions are allowed for field " + columnName.getColumnName());
            return values.stream().findFirst().map(it -> converter.fromPrestoNative(it)).orElse(null);
        }, ignored -> null);
        return conditionValue;
    }

    public static GroupCondition convert(TupleDomain<ColumnHandle> tupleDomain, String...excludeColumns){
        GroupCondition result = new GroupCondition(GroupCondition.Operator.AND);
        Set<String> exclusion = new HashSet<>(List.of(excludeColumns));
        for (Map.Entry<ColumnHandle, Domain> byColumn : tupleDomain.getDomains().get().entrySet()){
            Domain domain = byColumn.getValue();
            CyodaColumnHandle column = (CyodaColumnHandle) byColumn.getKey();
            if (exclusion.contains(column.getColumnName())) continue;
            ComparableValueConverter converter = (ComparableValueConverter) column.getConverter();
            ValueSet valueSet = domain.getValues();
            List<AbstractCondition> conditions = new ArrayList<>();
            GroupCondition fieldCondition = valueSet.getValuesProcessor().transform(ranges -> {
                for (Range range : ranges.getOrderedRanges()) {
                    if (range.isSingleValue()) {
                        Comparable objValue = converter.fromPrestoNative(range.getHighValue().get());
                        conditions.add(new Equals(column.getExternalName(), objValue, determineRangeFieldForEquals(objValue)));
                    } else {
                        if (!range.isHighUnbounded()) {
                            RangeCondition condition;
                            String fieldName = column.getExternalName();
                            if (range.isHighInclusive()) {
                                condition = new LessThanEquals(fieldName, converter.fromPrestoNative(range.getHighValue().get()), true);
                            } else {
                                condition = new LessThan(fieldName, converter.fromPrestoNative(range.getHighValue().get()), true);
                            }
                            conditions.add(condition);
                        }
                        if (!range.isLowUnbounded()) {
                            RangeCondition condition;
                            String fieldName = column.getExternalName();
                            if (range.isLowInclusive()) {
                                condition = new GreaterThanEquals(fieldName, converter.fromPrestoNative(range.getLowValue().get()), true);
                            } else {
                                condition = new GreaterThan(fieldName, converter.fromPrestoNative(range.getLowValue().get()), true);
                            }
                            conditions.add(condition);
                        }
                    }
                }
                return new GroupCondition(GroupCondition.Operator.OR, conditions.toArray(AbstractCondition[]::new));
            }, discreteValues -> {
                for (Object value : discreteValues.getValues()){
                    Comparable<?> objValue = converter.fromPrestoNative(value);
                    conditions.add(new Equals(column.getExternalName(), objValue, determineRangeFieldForEquals(objValue)));
                }
                return new GroupCondition(GroupCondition.Operator.OR, conditions.toArray(AbstractCondition[]::new));
            }, ignored -> null);
            result.addCondition(fieldCondition);
        }
        return result;
    }

    public static final Set<Class<?>> RANGE_TYPES = ImmutableSet.<Class<?>>builder()
            .add(Integer.class)
            .add(Long.class)
            .add(Float.class)
            .add(Double.class)
            .add(Date.class)
            .add(LocalDate.class)
            .add(LocalTime.class)
            .add(LocalDateTime.class)
            .add(ZonedDateTime.class)
            .add(BigDecimal.class)
            .add(BigInteger.class)
            .add(UUID.class)
            .build();

    public static final Map<Class<?>, Class<?>> PRIMITIVE_TYPES = ImmutableMap.<Class<?>, Class<?>>builder()
            .put(boolean.class, Boolean.class)
            .put(byte.class, Byte.class)
            .put(short.class, Short.class)
            .put(char.class, Character.class)
            .put(int.class, Integer.class)
            .put(long.class, Long.class)
            .put(float.class, Float.class)
            .put(double.class, Double.class)
            .build();
    public static boolean isRangeType(Class<?> type) {
        if (type.isPrimitive()) {
            type = PRIMITIVE_TYPES.get(type);
        }
        return RANGE_TYPES.contains(type);
    }
    private static boolean determineRangeFieldForEquals(Object value) {
        if ( value instanceof String ) {
            return true;
        } else {
            return isRangeType(value.getClass());
        }
    }
}
