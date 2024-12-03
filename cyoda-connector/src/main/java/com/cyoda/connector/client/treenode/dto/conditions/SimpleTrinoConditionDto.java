package com.cyoda.connector.client.treenode.dto.conditions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

// SimpleTrinoConditionDto
public class SimpleTrinoConditionDto extends AbstractTrinoConditionDto {
    private final Operation operation;
    private final String value;

    @JsonCreator
    public SimpleTrinoConditionDto(
            @JsonProperty("operation") Operation operation,
            @JsonProperty("value") String value
    ) {
        this.operation = operation;
        this.value = value;
    }

    @JsonProperty
    public Operation getOperation() {
        return operation;
    }

    @JsonProperty
    public String getValue() {
        return value;
    }

    @Override
    public String getType() {
        return "simple";
    }

    @Override
    public String toString() {
        return "["+ operation.getValue() +" "+ value +"]";
    }
}
