package com.cyoda.connector.client.treenode.dto.conditions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// ArrayTrinoConditionDto
public class ArrayTrinoConditionDto extends AbstractTrinoConditionDto {
    private final Operation operation;
    private final List<String> values;

    @JsonCreator
    public ArrayTrinoConditionDto(
            @JsonProperty("operation") Operation operation,
            @JsonProperty("values") List<String> values
    ) {
        this.operation = operation;
        this.values = values;
    }

    @JsonProperty
    public Operation getOperation() {
        return operation;
    }

    @JsonProperty
    public List<String> getValues() {
        return values;
    }

    @Override
    public String getType() {
        return "array";
    }

    @Override
    public String toString() {
        return "["+ operation.getValue() +" "+ values +"]";
    }
}
