package com.cyoda.connector.client.treenode.dto.conditions;

import com.cyoda.connector.client.treenode.dto.conditions.complex.AbstractConditionDto;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;

import java.util.List;
import java.util.StringJoiner;

// GroupTrinoConditionDto
public class GroupTrinoConditionDto extends AbstractTrinoConditionDto {
    private final Operator operator;
    private final List<AbstractTrinoConditionDto> conditions;

    @JsonCreator
    public GroupTrinoConditionDto(
            @JsonProperty("operator") Operator operator,
            @JsonProperty("conditions") List<AbstractTrinoConditionDto> conditions
    ) {
        this.operator = operator;
        this.conditions = conditions;
    }

    @JsonProperty
    public Operator getOperator() {
        return operator;
    }

    @JsonProperty
    public List<AbstractTrinoConditionDto> getConditions() {
        return conditions;
    }

    @Override
    public String getType() {
        return "group";
    }

    @Override
    public AbstractConditionDto toComplexCondition(CyodaColumnHandle.ColumnCategory columnCategory, String columnKey) {
        throw new UnsupportedOperationException("GroupTrinoConditionDto.toComplexCondition() is not supported.");
    }

    @Override
    @JsonIgnore
    public AbstractTrinoConditionDto simplify() {
        if (conditions.size() == 1) {
            return conditions.get(0);
        } else return this;
    }

    public enum Operator {
        AND, OR, NOT
    }

    @Override
    public String toString() {
        return "(" + Joiner.on(operator.toString()).join(conditions) + ")";
    }
}
