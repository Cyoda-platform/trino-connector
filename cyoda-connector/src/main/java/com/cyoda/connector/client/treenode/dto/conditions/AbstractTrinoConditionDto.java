package com.cyoda.connector.client.treenode.dto.conditions;
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
    @JsonIgnore
    public AbstractTrinoConditionDto simplify() {
        return this;
    }
}

