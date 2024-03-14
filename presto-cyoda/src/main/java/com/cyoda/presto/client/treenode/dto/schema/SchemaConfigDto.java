package com.cyoda.presto.client.treenode.dto.schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
public class SchemaConfigDto {
    private final String schemaName;

    private final List<TableConfigDto> tables;

    @JsonCreator
    public SchemaConfigDto(@JsonProperty("schemaName") String schemaName,
                           @JsonProperty("tables") List<TableConfigDto> tables) {
        this.schemaName = schemaName;
        this.tables = tables;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public List<TableConfigDto> getTables() {
        return tables;
    }
}
