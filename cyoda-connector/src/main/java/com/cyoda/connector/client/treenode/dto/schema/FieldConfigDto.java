package com.cyoda.connector.client.treenode.dto.schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FieldConfigDto {
    private final String fieldName; // display; default = fieldKey;
    private final String fieldKey; // key form <String, String> map
    private final String valuePath; // "fieldName" to use in conditions
    private final String dataType;
    private final Boolean isArray;
    private final Boolean flatten;
    private final List<FieldConfigDto> arrayFields;

    @JsonCreator
    public FieldConfigDto(@JsonProperty("fieldName") String fieldName,
                          @JsonProperty("fieldKey") String fieldKey,
                          @JsonProperty("valuePath") String valuePath,
                          @JsonProperty("dataType") String dataType,
                          @JsonProperty("isArray") Boolean isArray,
                          @JsonProperty("flatten") Boolean flatten,
                          @JsonProperty("arrayFields") List<FieldConfigDto> arrayFields) {
        this.fieldName = fieldName;
        this.fieldKey = fieldKey;
        this.valuePath = valuePath;
        this.dataType = dataType;
        this.isArray = isArray;
        this.flatten = flatten;
        this.arrayFields = arrayFields;
    }

    @JsonProperty
    public String getFieldName() {
        return fieldName;
    }
    @JsonProperty
    public String getFieldKey() {
        return fieldKey;
    }
    @JsonProperty
    public String getDataType() {
        return dataType;
    }
    @JsonProperty
    public String getValuePath() {
        return valuePath;
    }
    @JsonProperty
    public Boolean getArray() {
        return isArray;
    }
    @JsonProperty
    public Boolean getFlatten() {
        return flatten;
    }
    @JsonProperty
    public List<FieldConfigDto> getArrayFields() {
        return arrayFields;
    }
}
