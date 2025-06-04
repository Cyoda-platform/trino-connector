package com.cyoda.connector.client.treenode.dto.conditions.complex;

import com.cyoda.connector.client.treenode.dto.conditions.GroupTrinoConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.Operation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GroupConditionDto extends AbstractConditionDto {
    private final Operator operator;
    private final List<AbstractConditionDto> conditions;

    public static final GroupConditionDto ALL = new GroupConditionDto(GroupConditionDto.Operator.AND, List.of());

    @JsonCreator
    public GroupConditionDto(
            @JsonProperty("operator") Operator operator,
            @JsonProperty("conditions") List<AbstractConditionDto> conditions
    ) {
        this.operator = operator;
        this.conditions = conditions;
    }

    @JsonProperty
    public Operator getOperator() {
        return operator;
    }

    @JsonProperty
    public List<AbstractConditionDto> getConditions() {
        return conditions;
    }

    @Override
    @JsonIgnore
    public boolean isAll() {
        return conditions.isEmpty() || conditions.stream().allMatch(AbstractConditionDto::isAll);
    }

    @Override
    public String getType() {
        return AbstractConditionDto.GROUP_CONDITION_TYPE;
    }

    @Override
    public String toString() {
        return "Group(" + operator + ", " + conditions + ")";
    }

    public enum Operator {
        AND, OR, NOT
    }


}