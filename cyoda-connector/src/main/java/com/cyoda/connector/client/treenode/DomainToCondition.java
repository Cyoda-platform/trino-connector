package com.cyoda.connector.client.treenode;

import com.cyoda.connector.client.logic.converters.PrestoValueConverter;
import com.cyoda.connector.client.treenode.dto.conditions.AbstractTrinoConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.GroupTrinoConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.SimpleTrinoConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.Operation;
import io.trino.spi.predicate.Domain;
import io.trino.spi.predicate.Range;
import io.trino.spi.predicate.ValueSet;

import java.util.*;

public class DomainToCondition {

    private DomainToCondition() {
        // Utility class
    }

    private static String convert(PrestoValueConverter<?> converter, Optional<Object> value){
        return converter.toStringFromNative(value.get());
    }
    private static String convert(PrestoValueConverter<?> converter, Object value){
        return converter.toStringFromNative(value);
    }

    public static AbstractTrinoConditionDto createTrinoCondition(PrestoValueConverter<?> converter, Domain domain) {
        ValueSet valueSet = domain.getValues();
        return valueSet.getValuesProcessor().transform(ranges -> {
            List<AbstractTrinoConditionDto> rangeConditions = new ArrayList<>();
            for (Range range : ranges.getOrderedRanges()) {
                if (range.isSingleValue()) {
                    rangeConditions.add(new SimpleTrinoConditionDto(Operation.EQUALS, convert(converter, range.getHighValue())));
                } else {
                    List<AbstractTrinoConditionDto> singleRangeCondition = new ArrayList<>();
                    if (!range.isHighUnbounded()) {
                        if (range.isHighInclusive()) {
                            singleRangeCondition.add(new SimpleTrinoConditionDto(Operation.LESS_OR_EQUAL, convert(converter, range.getHighValue())));
                        } else {
                            singleRangeCondition.add(new SimpleTrinoConditionDto(Operation.LESS_THAN, convert(converter, range.getHighValue())));
                        }
                    }
                    if (!range.isLowUnbounded()) {
                        if (range.isLowInclusive()) {
                            singleRangeCondition.add(new SimpleTrinoConditionDto(Operation.GREATER_OR_EQUAL, convert(converter, range.getLowValue())));
                        } else {
                            singleRangeCondition.add(new SimpleTrinoConditionDto(Operation.GREATER_THAN, convert(converter, range.getLowValue())));
                        }
                    }
                    rangeConditions.add(new GroupTrinoConditionDto(GroupTrinoConditionDto.Operator.AND, singleRangeCondition).simplify());
                }
            }

            return new GroupTrinoConditionDto(GroupTrinoConditionDto.Operator.OR, rangeConditions).simplify();
        }, discreteValues -> {
            List<AbstractTrinoConditionDto> equalsConditions = new ArrayList<>();
            for (Object value : discreteValues.getValues()){
                equalsConditions.add(new SimpleTrinoConditionDto(Operation.EQUALS, convert(converter, value)));
            }
            return new GroupTrinoConditionDto(GroupTrinoConditionDto.Operator.OR, equalsConditions).simplify();
        }, ignored -> null);
    }

}
