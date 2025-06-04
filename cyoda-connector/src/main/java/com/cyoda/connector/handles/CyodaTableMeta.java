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

package com.cyoda.connector.handles;

import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.SchemaTableName;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import io.trino.spi.predicate.TupleDomain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class CyodaTableMeta {
    private final String schemaName;
    private final String tableName;
    private final Map<String,CyodaColumnHandle> columnHandleMap;
    private final CyodaTableType tableType;
    private final String reportConfigId;
    private final String description;
    private final transient List<ColumnMetadata> columnMetadata;
    private final transient ConnectorTableMetadata metadata;

    private final boolean hasGroups;
    private final boolean hasHistory;

    @JsonCreator
    public CyodaTableMeta(
            @JsonProperty("schemaName") String schemaName,
            @JsonProperty("tableName") String tableName,
            @JsonProperty("projectedColumns") List<CyodaColumnHandle> projectedColumns,
            @JsonProperty("tableType") CyodaTableType tableType,
            @JsonProperty("reportConfigId") String reportConfigId,
            @JsonProperty("description") String description,
            @JsonProperty("hasGroups") boolean hasGroups,
            @JsonProperty("hasHistory") boolean hasHistory) {
        this.schemaName = requireNonNull(schemaName, "schemaName is null");
        this.tableName = requireNonNull(tableName, "tableName is null");
        this.reportConfigId = reportConfigId;
        this.description = description;
        this.tableType = tableType;
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
    public String getSchemaName() {
        return schemaName;
    }

    @JsonProperty
    public String getTableName() {
        return tableName;
    }

    @JsonProperty
    public CyodaTableType getTableType() {
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

    public SchemaTableName toSchemaTableName() {
        return new SchemaTableName(schemaName, tableName);
    }
    public CyodaTableHandle toMainHandle(long createDate, long lastUpdateDate) {
        return new CyodaTableHandle(schemaName, tableName, tableType, reportConfigId, createDate, lastUpdateDate, TupleDomain.all(), null, null, null, null);
    }
    public CyodaTableHandle toSuppHandle(String tablePostfix, CyodaTableType tableType) {
        return new CyodaTableHandle(schemaName, tableName + tablePostfix, tableType, reportConfigId, 0, 0, TupleDomain.all(), null, null, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CyodaTableMeta that = (CyodaTableMeta) o;
        return schemaName.equals(that.schemaName)
                && tableName.equals(that.tableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaName, tableName);
    }

    @Override
    public String toString() {
        return Joiner.on(":").join(schemaName, tableName);
    }

    public static class Template{
        private final String description;
        private final List<CyodaColumnHandle> columnHandles;
        private final CyodaTableType tableType;

        public Template(String description, List<CyodaColumnHandle> columnHandles, CyodaTableType tableType) {
            this.description = description;
            this.columnHandles = columnHandles;
            this.tableType = tableType;
        }
        public static Template of(CyodaTableMeta tableHandle){
            return new Template(tableHandle.getDescription(),
                    tableHandle.getProjectedColumns(),
                    tableHandle.tableType);
        }

        public CyodaTableMeta createTableMeta(CyodaTableHandle tableHandle){
            return new CyodaTableMeta(tableHandle.getSchemaName(), tableHandle.getTableName(), columnHandles,
                    tableType, tableHandle.getTableMetaId(), description, false, false);
        }
    }

}
