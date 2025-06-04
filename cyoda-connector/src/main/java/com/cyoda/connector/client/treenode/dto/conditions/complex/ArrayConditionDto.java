package com.cyoda.connector.client.treenode.dto.conditions.complex;

import com.cyoda.connector.client.treenode.dto.conditions.Operation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ArrayConditionDto extends AbstractConditionDto {
    private final String jsonPath;
    private final Operation operatorType;
    private final List<String> value;

    @JsonCreator
    public ArrayConditionDto(
            @JsonProperty("jsonPath") String jsonPath,
            @JsonProperty("operatorType") Operation operatorType,
            @JsonProperty("value") List<String> value
    ) {
        this.jsonPath = jsonPath;
        this.operatorType = operatorType;
        this.value = value;
    }

    @JsonProperty
    public String getJsonPath() {
        return jsonPath;
    }

    @JsonProperty
    public Operation getOperatorType() {
        return operatorType;
    }

    @JsonProperty
    public List<String> getValue() {
        return value;
    }

    @Override
    public String getType() {
        return AbstractConditionDto.ARRAY_CONDITION_TYPE;
    }

    @Override
    public String toString() {
        return jsonPath + " " + operatorType + " " + value;
    }
}

