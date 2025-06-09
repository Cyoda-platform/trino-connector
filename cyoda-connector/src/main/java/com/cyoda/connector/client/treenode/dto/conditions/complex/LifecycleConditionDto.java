package com.cyoda.connector.client.treenode.dto.conditions.complex;

import com.cyoda.connector.client.treenode.dto.conditions.Operation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LifecycleConditionDto extends AbstractConditionDto {
    private final String field;
    private final Operation operatorType;
    private final String value;

    @JsonCreator
    public LifecycleConditionDto(
            @JsonProperty("field") String field,
            @JsonProperty("operatorType") Operation operatorType,
            @JsonProperty("value") String value
    ) {
        this.field = field;
        this.operatorType = operatorType;
        this.value = value;
    }

    @JsonProperty
    public String getField() {
        return field;
    }

    @JsonProperty
    public Operation getOperatorType() {
        return operatorType;
    }

    @JsonProperty
    public String getValue() {
        return value;
    }

    @Override
    public String getType() {
        return AbstractConditionDto.LIFECYCLE_CONDITION_TYPE;
    }

    @Override
    public String toString() {
        return field + " " + operatorType + " " + value;
    }
}
