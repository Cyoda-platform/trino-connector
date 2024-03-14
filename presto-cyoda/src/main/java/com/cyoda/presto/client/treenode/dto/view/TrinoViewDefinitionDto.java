/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cyoda.presto.client.treenode.dto.view;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ConnectorViewDefinition;
import io.trino.spi.type.TypeId;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;


public class TrinoViewDefinitionDto
{
    private final String originalSql;
    private final String catalog;
    private final String schema;
    private final List<TrinoViewColumn> columns;
    private final String comment;
    private final String owner;
    private final boolean runAsInvoker;

    @JsonCreator
    public TrinoViewDefinitionDto(
            @JsonProperty("originalSql") String originalSql,
            @JsonProperty("catalog") String catalog,
            @JsonProperty("schema") String schema,
            @JsonProperty("columns") List<TrinoViewColumn> columns,
            @JsonProperty("comment") String comment,
            @JsonProperty("owner") String owner,
            @JsonProperty("runAsInvoker") boolean runAsInvoker)
    {
        this.originalSql = originalSql;
        this.catalog = catalog;
        this.schema = schema;
        this.columns = columns;
        this.comment = comment;
        this.owner = owner;
        this.runAsInvoker = runAsInvoker;
    }

    @JsonProperty
    public String getOriginalSql()
    {
        return originalSql;
    }

    @JsonProperty
    public String getCatalog()
    {
        return catalog;
    }

    @JsonProperty
    public String getSchema()
    {
        return schema;
    }

    @JsonProperty
    public List<TrinoViewColumn> getColumns()
    {
        return columns;
    }

    @JsonProperty
    public String getComment()
    {
        return comment;
    }

    @JsonProperty
    public String getOwner()
    {
        return owner;
    }

    @JsonProperty
    public boolean isRunAsInvoker()
    {
        return runAsInvoker;
    }

    public static TrinoViewDefinitionDto fromModel(ConnectorViewDefinition connectorView) {
        List<TrinoViewColumn> columns = connectorView.getColumns().stream()
                .map(c -> new TrinoViewColumn(c.getName(), c.getType().getId(), c.getComment().orElse(null)))
                .collect(Collectors.toList());

        return new TrinoViewDefinitionDto(
                connectorView.getOriginalSql(),
                connectorView.getCatalog().orElse(null),
                connectorView.getSchema().orElse(null),
                columns,
                connectorView.getComment().orElse(null),
                connectorView.getOwner().orElse(null),
                connectorView.isRunAsInvoker()
        );
    }

    public static ConnectorViewDefinition toModel(TrinoViewDefinitionDto dto) {
        List<ConnectorViewDefinition.ViewColumn> columns = dto.columns.stream()
                .map(c -> new ConnectorViewDefinition.ViewColumn(c.getName(), TypeId.of(c.getType()), Optional.ofNullable(c.getComment())))
                .collect(Collectors.toList());

        return new ConnectorViewDefinition(
                dto.originalSql,
                Optional.ofNullable(dto.catalog),
                Optional.ofNullable(dto.schema),
                columns,
                Optional.ofNullable(dto.comment),
                Optional.ofNullable(dto.owner),
                dto.runAsInvoker
        );
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", TrinoViewDefinitionDto.class.getSimpleName() + "[", "]");
        joiner.add("originalSql='" + originalSql + "'");
        joiner.add("catalog='" + catalog + "'");
        joiner.add("schema='" + schema + "'");
        joiner.add("columns=" + columns);
        joiner.add("comment='" + comment + "'");
        joiner.add("owner='" + owner + "'");
        joiner.add("runAsInvoker=" + runAsInvoker);
        return joiner.toString();
    }

    public static final class TrinoViewColumn
    {
        private final String name;
        private final String type;
        private final String comment;

        @JsonCreator
        public TrinoViewColumn(
                @JsonProperty("name") String name,
                @JsonProperty("type") String type,
                @JsonProperty("comment") String comment)
        {
            this.name = name;
            this.type = type;
            this.comment = comment;
        }

        @JsonProperty
        public String getName()
        {
            return name;
        }

        @JsonProperty
        public String getType()
        {
            return type;
        }

        @JsonProperty
        public String getComment() {
            return comment;
        }
        @Override
        public String toString()
        {
            return name + " " + type;
        }
    }
}
