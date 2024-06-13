package com.cyoda.presto.client.treenode.dto.schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class SchemaConfigDto {
    private final UUID id;
    private final String schemaName;

    private final List<TableConfigDto> tables;

    @JsonCreator
    public SchemaConfigDto(@JsonProperty("id")UUID id,
                           @JsonProperty("schemaName") String schemaName,
                           @JsonProperty("tables") List<TableConfigDto> tables) {
        this.id = id;
        this.schemaName = schemaName;
        this.tables = tables;
    }

    @JsonProperty
    public String getSchemaName() {
        return schemaName;
    }

    @JsonProperty
    public List<TableConfigDto> getTables() {
        return tables;
    }

    @JsonProperty
    public UUID getId() {
        return id;
    }
}
