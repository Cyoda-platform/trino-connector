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

package com.cyoda.presto;

import com.cyoda.presto.handles.CyodaColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

public class CyodaTable {
    private final String name;
    private final List<CyodaColumnHandle> columns;
    private final List<ColumnMetadata> columnsMetadata;
    private final String reportConfigurationId;
    private final String description;

    //TODO: This might need to be something pulled from Reporting API.
    private final List<URI> sources;

    private final ImmutableMap<String, CyodaColumnHandle> columnsByName;

    @JsonCreator
    public CyodaTable(
            @JsonProperty("name") String name,
            @JsonProperty("columns") List<CyodaColumnHandle> columns,
            @JsonProperty("reportConfigurationId") String reportConfigurationId,
            @JsonProperty("description") String description,
            @JsonProperty("sources") List<URI> sources
    ) {
        checkArgument(!isNullOrEmpty(name), "name is null or is empty");
        this.name = requireNonNull(name, "name is null");
        this.reportConfigurationId = reportConfigurationId;
        this.description = description;
        this.columns = ImmutableList.copyOf(requireNonNull(columns, "columns is null"));
        this.sources = ImmutableList.copyOf(requireNonNull(sources, "sources is null"));

        ImmutableList.Builder<ColumnMetadata> thisMeta = ImmutableList.builder();
        for (CyodaColumnHandle column : this.columns) {
            thisMeta.add(new ColumnMetadata(column.getColumnName(), column.getColumnType()));
        }
        this.columnsMetadata = thisMeta.build();
        this.columnsByName = new ImmutableMap.Builder<String, CyodaColumnHandle>()
                .putAll(Maps.uniqueIndex(this.columns, CyodaColumnHandle::getColumnName))
                .build();
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public List<CyodaColumnHandle> getColumns() {
        return columns;
    }

    @JsonProperty
    public List<URI> getSources() {
        return sources;
    }

    @JsonProperty
    public List<ColumnMetadata> getColumnsMetadata() {
        return columnsMetadata;
    }

    @JsonProperty
    public String getReportConfigurationId() {
        return reportConfigurationId;
    }

    @JsonProperty
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @JsonIgnore
    public CyodaColumnHandle getColumn(String columnName) {
        CyodaColumnHandle column = this.columnsByName.get(columnName);
        Preconditions.checkArgument(column != null, "Table %s does not have a column %s", this.name, columnName);
        return column;
    }
}
