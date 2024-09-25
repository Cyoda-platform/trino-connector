package com.cyoda.connector.client.treenode;

import com.cyoda.connector.client.logic.converters.PrestoValueConverter;
import com.cyoda.connector.client.treenode.dto.conditions.AbstractTrinoConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.GroupTrinoConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.SimpleTrinoConditionDto;
import com.cyoda.core.conditions.Operation;
import io.trino.spi.predicate.Domain;
import io.trino.spi.predicate.Range;
import io.trino.spi.predicate.ValueSet;

import java.util.*;

public class DomainToCondition {

    private DomainToCondition() {
        // Utility class
    }

    public static AbstractTrinoConditionDto createTrinoCondition(PrestoValueConverter<?> converter, Domain domain) {
        ValueSet valueSet = domain.getValues();
        return valueSet.getValuesProcessor().transform(ranges -> {
            List<AbstractTrinoConditionDto> rangeConditions = new ArrayList<>();
            for (Range range : ranges.getOrderedRanges()) {
                if (range.isSingleValue()) {
                    Object objValue = converter.fromPrestoNative(range.getHighValue().get());
                    rangeConditions.add(new SimpleTrinoConditionDto(Operation.EQUALS, objValue));
                } else {
                    List<AbstractTrinoConditionDto> singleRangeCondition = new ArrayList<>();
                    if (!range.isHighUnbounded()) {
                        if (range.isHighInclusive()) {
                            singleRangeCondition.add(new SimpleTrinoConditionDto(Operation.LESS_OR_EQUAL, converter.fromPrestoNative(range.getHighValue().get())));
                        } else {
                            singleRangeCondition.add(new SimpleTrinoConditionDto(Operation.LESS_THAN, converter.fromPrestoNative(range.getHighValue().get())));
                        }
                    }
                    if (!range.isLowUnbounded()) {
                        if (range.isLowInclusive()) {
                            singleRangeCondition.add(new SimpleTrinoConditionDto(Operation.GREATER_OR_EQUAL, converter.fromPrestoNative(range.getLowValue().get())));
                        } else {
                            singleRangeCondition.add(new SimpleTrinoConditionDto(Operation.GREATER_THAN, converter.fromPrestoNative(range.getLowValue().get())));
                        }
                    }
                    rangeConditions.add(new GroupTrinoConditionDto(GroupTrinoConditionDto.Operator.AND, singleRangeCondition).simplify());
                }
            }

            return new GroupTrinoConditionDto(GroupTrinoConditionDto.Operator.OR, rangeConditions).simplify();
        }, discreteValues -> {
            List<AbstractTrinoConditionDto> equalsConditions = new ArrayList<>();
            for (Object value : discreteValues.getValues()){
                Object objValue = converter.fromPrestoNative(value);
                equalsConditions.add(new SimpleTrinoConditionDto(Operation.EQUALS, objValue));
            }
            return new GroupTrinoConditionDto(GroupTrinoConditionDto.Operator.OR, equalsConditions).simplify();
        }, ignored -> null);
    }

}
