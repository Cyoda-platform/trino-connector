package com.cyoda.connector.client.treenode.dto.conditions;
import com.cyoda.connector.client.treenode.dto.conditions.complex.AbstractConditionDto;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.beans.Transient;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GroupTrinoConditionDto.class, name = "group"),
        @JsonSubTypes.Type(value = SimpleTrinoConditionDto.class, name = "simple")
})
public abstract class AbstractTrinoConditionDto {
    public abstract String getType();
    public abstract AbstractConditionDto toComplexCondition(CyodaColumnHandle.ColumnCategory columnCategory, String columnKey);
    @JsonIgnore
    public AbstractTrinoConditionDto simplify() {
        return this;
    }
}

