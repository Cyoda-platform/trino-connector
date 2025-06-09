package com.cyoda.connector.client.treenode.dto.conditions.complex;

import com.cyoda.connector.client.treenode.dto.conditions.Operation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SimpleConditionDto extends AbstractConditionDto {
    private final String jsonPath;
    private final Operation operatorType;
    private final String value;

    @JsonCreator
    public SimpleConditionDto(
            @JsonProperty("jsonPath") String jsonPath,
            @JsonProperty("operatorType") Operation operatorType,
            @JsonProperty("value") String value
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
    public String getValue() {
        return value;
    }

    @Override
    public String getType() {
        return AbstractConditionDto.SIMPLE_CONDITION_TYPE;
    }

    @Override
    public String toString() {
        return jsonPath + " " + operatorType + " " + value;
    }
}
