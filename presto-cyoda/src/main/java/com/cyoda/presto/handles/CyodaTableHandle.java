/*
 * Copyright (C) 2022 Cyoda Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.cyoda.presto.handles;

import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.SchemaTableName;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

//TODO need some day to separate it in CyodaTable data object stored in some map and an actual handle
public class CyodaTableHandle implements ConnectorTableHandle {
    private final String connectorId;
    private final String schemaName;
    private final String tableName;
    private final Map<String,CyodaColumnHandle> columnHandleMap;
    private final TableType tableType;
    private final String reportConfigId;
    private final String description;
    private final URI uri;

    private final transient List<ColumnMetadata> columnMetadata;
    private final transient ConnectorTableMetadata metadata;

    private final boolean hasGroups;
    private final boolean hasHistory;

    @JsonCreator
    public CyodaTableHandle(
            @JsonProperty("connectorId") String connectorId,
            @JsonProperty("schemaName") String schemaName,
            @JsonProperty("tableName") String tableName,
            @JsonProperty("projectedColumns") List<CyodaColumnHandle> projectedColumns,
            @JsonProperty("tableType") TableType tableType,
            @JsonProperty("reportConfigId") String reportConfigId,
            @JsonProperty("description") String description,
            @JsonProperty("uri") URI uri,
            @JsonProperty("hasGroups") boolean hasGroups,
            @JsonProperty("hasHistory") boolean hasHistory) {
        this.connectorId = requireNonNull(connectorId, "connectorId is null");
        this.schemaName = requireNonNull(schemaName, "schemaName is null");
        this.tableName = requireNonNull(tableName, "tableName is null");
        this.reportConfigId = reportConfigId;
        this.description = description;
        this.tableType = tableType;
        this.uri = uri;
        this.hasGroups = hasGroups;
        this.hasHistory = hasHistory;
        columnHandleMap = new HashMap<>();
        columnMetadata = new ArrayList<>();
        for (CyodaColumnHandle columnHandle : projectedColumns) {
            columnHandleMap.put(columnHandle.getColumnName(), columnHandle);
            columnMetadata.add(columnHandle.getColumnMetadata());
        }
        metadata = new ConnectorTableMetadata(
                new SchemaTableName(schemaName, tableName),
                columnMetadata, Collections.emptyMap(), Optional.ofNullable(description));
    }

    @JsonProperty("hasGroups")
    public boolean hasGroups() {
        return hasGroups;
    }

    @JsonProperty("hasHistory")
    public boolean hasHistory() {
        return hasHistory;
    }

    @JsonProperty
    public String getConnectorId() {
        return connectorId;
    }

    @JsonProperty
    public String getSchemaName() {
        return schemaName;
    }

    @JsonProperty
    public String getTableName() {
        return tableName;
    }

    @JsonProperty
    public TableType getTableType() {
        return tableType;
    }

    @JsonProperty
    public List<CyodaColumnHandle> getProjectedColumns() {
        return columnHandleMap.values().stream().toList();
    }

    public List<ColumnMetadata> getColumnMetadata() {
        return columnMetadata;
    }

    public ConnectorTableMetadata getMetadata() {
        return metadata;
    }

    public Map<String, CyodaColumnHandle> getColumnHandleMap() {
        return columnHandleMap;
    }

    public CyodaColumnHandle getColumn(String name){
        return Optional.ofNullable(columnHandleMap.get(name)).orElseThrow(
                () -> new NoSuchElementException(String.format(
                        "Metadata of table \"%s\" does not contain field with name %s",
                        tableName, name)));
    }

    @JsonProperty
    public String getReportConfigId() {
        return reportConfigId;
    }
    @JsonProperty
    public String getDescription() {
        return description;
    }
    public URI getUri() {
        return uri;
    }

    public SchemaTableName toSchemaTableName() {
        return new SchemaTableName(schemaName, tableName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CyodaTableHandle that = (CyodaTableHandle) o;
        return connectorId.equals(that.connectorId)
                && schemaName.equals(that.schemaName)
                && tableName.equals(that.tableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectorId, schemaName, tableName);
    }

    @Override
    public String toString() {
        return Joiner.on(":").join(connectorId, schemaName);
    }

    public enum TableType {
        REPORTS,
        STATS,
        HISTORY,
        GROUP,
        DATA,
        DUMMY,
        CALL_STATS
    }

    public static class Template{
        private final String connectorId;
        private final String schemaName;
        private final List<CyodaColumnHandle> columnHandles;
        private final TableType tableType;
        private final URI uri;

        public Template(String connectorId, String schemaName, List<CyodaColumnHandle> columnHandles, TableType tableType, URI uri) {
            this.connectorId = connectorId;
            this.schemaName = schemaName;
            this.columnHandles = columnHandles;
            this.tableType = tableType;
            this.uri = uri;
        }
        public static Template of(CyodaTableHandle tableHandle){
            return new Template(tableHandle.connectorId,
                    tableHandle.schemaName,
                    tableHandle.getProjectedColumns(),
                    tableHandle.tableType,
                    tableHandle.uri);
        }

        public CyodaTableHandle createTableHandle(String tableName, String configId, String description){
            return new CyodaTableHandle(connectorId, schemaName, tableName, columnHandles, tableType, configId, description, uri, false, false);
        }
    }

}
