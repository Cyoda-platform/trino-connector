package com.cyoda.connector.client.treenode.dto.conditions.complex;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// Abstract base class with polymorphism config
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GroupConditionDto.class, name = AbstractConditionDto.GROUP_CONDITION_TYPE),
        @JsonSubTypes.Type(value = SimpleConditionDto.class, name = AbstractConditionDto.SIMPLE_CONDITION_TYPE),
        @JsonSubTypes.Type(value = LifecycleConditionDto.class, name = AbstractConditionDto.LIFECYCLE_CONDITION_TYPE),
        @JsonSubTypes.Type(value = ArrayConditionDto.class, name = AbstractConditionDto.ARRAY_CONDITION_TYPE)
})
public abstract class AbstractConditionDto {
    public static final String GROUP_CONDITION_TYPE = "group";
    public static final String SIMPLE_CONDITION_TYPE = "simple";
    public static final String ARRAY_CONDITION_TYPE = "array";
    public static final String LIFECYCLE_CONDITION_TYPE = "lifecycle";


    @JsonProperty
    public abstract String getType();
    @JsonIgnore
    public boolean isAll(){
        return false;
    }
}