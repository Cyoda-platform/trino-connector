package com.cyoda.connector.client.treenode.dto.conditions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

// SimpleTrinoConditionDto
public class SimpleTrinoConditionDto extends AbstractTrinoConditionDto {
    private final Operation operation;
    private final Object value;

    @JsonCreator
    public SimpleTrinoConditionDto(
            @JsonProperty("operation") Operation operation,
            @JsonProperty("value") Object value
    ) {
        this.operation = operation;
        this.value = value;
    }

    @JsonProperty
    public Operation getOperation() {
        return operation;
    }

    @JsonProperty
    public Object getValue() {
        return value;
    }

    @Override
    public String getType() {
        return "simple";
    }
}
