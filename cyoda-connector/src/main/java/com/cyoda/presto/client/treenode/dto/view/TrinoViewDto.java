package com.cyoda.presto.client.treenode.dto.view;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.SchemaTableName;

public class TrinoViewDto {
    private final String userId;
    private final SchemaTableName schemaTableName;
    private final TrinoViewDefinitionDto viewDefinition;

    //REQUEST fields
    private final SchemaTableName newName;
    private final boolean replace;

    @JsonCreator
    public TrinoViewDto(@JsonProperty("userId") String userId,
                        @JsonProperty("schemaTableName") SchemaTableName schemaTableName,
                        @JsonProperty("viewDefinition") TrinoViewDefinitionDto viewDefinition,
                        @JsonProperty("newName")SchemaTableName newName,
                        @JsonProperty("replace") boolean replace) {
        this.userId = userId;
        this.schemaTableName = schemaTableName;
        this.viewDefinition = viewDefinition;
        this.newName = newName;
        this.replace = replace;
    }
    public TrinoViewDto(SchemaTableName schemaTableName, TrinoViewDefinitionDto viewDefinition){
        this(null, schemaTableName, viewDefinition, null, false);
    }

    @JsonProperty
    public String getUserId() {
        return userId;
    }

    @JsonProperty
    public SchemaTableName getSchemaTableName() {
        return schemaTableName;
    }

    @JsonProperty
    public TrinoViewDefinitionDto getViewDefinition() {
        return viewDefinition;
    }

    @JsonProperty
    public SchemaTableName getNewName() {
        return newName;
    }

    @JsonProperty
    public boolean getReplace() {
        return replace;
    }
}
