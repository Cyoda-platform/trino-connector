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


    private static AbstractTrinoConditionDto convert(PrestoValueConverter<?> converter, Operation operation, Object value){
        return converter.toCondition(operation, value);
    }

    public static AbstractTrinoConditionDto createTrinoCondition(PrestoValueConverter<?> converter, Domain domain) {
        ValueSet valueSet = domain.getValues();
        return valueSet.getValuesProcessor().transform(ranges -> {
            List<AbstractTrinoConditionDto> rangeConditions = new ArrayList<>();
            for (Range range : ranges.getOrderedRanges()) {
                if (range.isSingleValue()) {
                    rangeConditions.add(convert(converter, Operation.EQUALS, range.getHighValue().get()));
                } else {
                    List<AbstractTrinoConditionDto> singleRangeCondition = new ArrayList<>();
                    if (!range.isHighUnbounded()) {
                        if (range.isHighInclusive()) {
                            singleRangeCondition.add(convert(converter, Operation.LESS_OR_EQUAL, range.getHighValue().get()));
                        } else {
                            singleRangeCondition.add(convert(converter, Operation.LESS_THAN, range.getHighValue().get()));
                        }
                    }
                    if (!range.isLowUnbounded()) {
                        if (range.isLowInclusive()) {
                            singleRangeCondition.add(convert(converter, Operation.GREATER_OR_EQUAL, range.getLowValue().get()));
                        } else {
                            singleRangeCondition.add(convert(converter, Operation.GREATER_THAN, range.getLowValue().get()));
                        }
                    }
                    rangeConditions.add(new GroupTrinoConditionDto(GroupTrinoConditionDto.Operator.AND, singleRangeCondition).simplify());
                }
            }

            return new GroupTrinoConditionDto(GroupTrinoConditionDto.Operator.OR, rangeConditions).simplify();
        }, discreteValues -> {
            List<AbstractTrinoConditionDto> equalsConditions = new ArrayList<>();
            for (Object value : discreteValues.getValues()){
                equalsConditions.add(convert(converter, Operation.EQUALS, value));
            }
            return new GroupTrinoConditionDto(GroupTrinoConditionDto.Operator.OR, equalsConditions).simplify();
        }, ignored -> null);
    }

}
