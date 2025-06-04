package com.cyoda.connector.client.treenode.dto.conditions;

import com.cyoda.connector.client.treenode.dto.conditions.complex.AbstractConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.complex.LifecycleConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.complex.SimpleConditionDto;
import com.cyoda.connector.handles.CyodaColumnHandle;
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
    public AbstractConditionDto toComplexCondition(CyodaColumnHandle.ColumnCategory columnCategory, String columnKey) {
        if (columnCategory == CyodaColumnHandle.ColumnCategory.DATA) {
            return new SimpleConditionDto(columnKey, operation, value);
        } else if (columnCategory == CyodaColumnHandle.ColumnCategory.ROOT) {
            return new LifecycleConditionDto(columnKey, operation, value);
        } else if (columnCategory == CyodaColumnHandle.ColumnCategory.SPECIAL && columnKey.equals(CyodaColumnHandle.SpecialColumn.ENTITY_ID.name())) {
            return new LifecycleConditionDto("id", operation, value);
        } else if (columnCategory == CyodaColumnHandle.ColumnCategory.INDEX) {
            // not an error, but pushdown is not supported for index column
            return null;
        } else throw new UnsupportedOperationException("Unsupported column category and key: " + columnKey + "::" + columnCategory);
    }

    @Override
    public String toString() {
        return "["+ operation.getValue() +" "+ value +"]";
    }
}
