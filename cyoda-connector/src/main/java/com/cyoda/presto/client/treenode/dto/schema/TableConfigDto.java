package com.cyoda.presto.client.treenode.dto.schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class TableConfigDto {
    private final String tableName; // display; default = entityPartName
    private final UUID metadataClassId; // link to entityName.partName on cyoda side
    private final String uniformedPath;
    private final List<FieldConfigDto> fields;

    @JsonCreator
    public TableConfigDto(@JsonProperty("tableName") String tableName,
                          @JsonProperty("metadataClassId") UUID metadataClassId,
                          @JsonProperty("uniformedPath") String uniformedPath,
                          @JsonProperty("fields") List<FieldConfigDto> fields) {
        this.tableName = tableName;
        this.metadataClassId = metadataClassId;
        this.uniformedPath = uniformedPath;
        this.fields = fields;
    }

    @JsonProperty
    public String getTableName() {
        return tableName;
    }

    @JsonProperty
    public UUID getMetadataClassId() {
        return metadataClassId;
    }

    @JsonProperty
    public String getUniformedPath() {
        return uniformedPath;
    }

    @JsonProperty
    public List<FieldConfigDto> getFields() {
        return fields;
    }
}