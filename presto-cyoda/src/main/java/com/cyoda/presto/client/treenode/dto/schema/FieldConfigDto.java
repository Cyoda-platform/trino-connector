package com.cyoda.presto.client.treenode.dto.schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FieldConfigDto {
    private final String fieldName; // display; default = fieldKey;
    private final String fieldKey; // key form <String, String> map
    private final String valuePath; // "fieldName" to use in conditions
    private final String dataType;

    @JsonCreator
    public FieldConfigDto(@JsonProperty("fieldName") String fieldName,
                          @JsonProperty("fieldKey") String fieldKey,
                          @JsonProperty("valuePath") String valuePath,
                          @JsonProperty("dataType") String dataType) {
        this.fieldName = fieldName;
        this.fieldKey = fieldKey;
        this.valuePath = valuePath;
        this.dataType = dataType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public String getDataType() {
        return dataType;
    }

    public String getValuePath() {
        return valuePath;
    }
}
