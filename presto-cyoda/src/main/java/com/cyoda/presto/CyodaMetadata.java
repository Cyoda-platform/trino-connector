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

import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.reporting.calls.DeleteReportsApiHandler;
import com.cyoda.presto.client.reporting.metaproviders.DynamicReportMetadataProvider;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportTable;
import com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadataProvider;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import io.airlift.slice.Slice;
import io.trino.spi.TrinoException;
import io.trino.spi.connector.ConnectorPartitioningHandle;
import io.trino.spi.connector.RetryMode;
import io.trino.spi.connector.TableColumnsMetadata;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.connector.SchemaTablePrefix;
import io.trino.spi.connector.ConnectorMetadata;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import static com.google.common.base.Preconditions.checkArgument;
import static io.trino.spi.StandardErrorCode.NOT_SUPPORTED;
import static java.util.Objects.requireNonNull;

public class CyodaMetadata implements ConnectorMetadata {

    private static final SupplierLogger LOG = SupplierLogger.get(CyodaMetadata.class);

    private final String connectorId;
    private final CyodaConfig config;
    private final AuthService auth;
    private final StaticTableMetadataProvider staticMetadataProvider;
    private final DynamicReportMetadataProvider dynamicReportMetadataProvider;
    private final DeleteReportsApiHandler deleteReportsApiHandler;

    @Inject
    public CyodaMetadata(
            CyodaConnectorId connectorId,
            CyodaConfig config,
            AuthService auth,
            StaticTableMetadataProvider staticMetadataProvider,
            DynamicReportMetadataProvider dynamicReportMetadataProvider,
            DeleteReportsApiHandler deleteReportsApiHandler) {
        this.connectorId = requireNonNull(connectorId, "connectorId is null").toString();
        this.config = requireNonNull(config,"confif is null");
        this.auth = auth;
        this.staticMetadataProvider = staticMetadataProvider;
        this.dynamicReportMetadataProvider = dynamicReportMetadataProvider;
        this.deleteReportsApiHandler = deleteReportsApiHandler;
    }


    @Override
    public List<String> listSchemaNames(ConnectorSession session) {
        return ImmutableList.of(config.getSchemaName());
    }

    @Override
    public ConnectorTableHandle getTableHandle(ConnectorSession session, SchemaTableName tableName) {
        String tableKey = null;
        try {
            if (!listSchemaNames(session).contains(tableName.getSchemaName())) {
                return null;
            }
            tableKey = tableName.getTableName();
            if (staticMetadataProvider.contains(tableKey)) {
                return staticMetadataProvider.getTableHandle(tableKey);
            } else {
                return dynamicReportMetadataProvider.getTableHandle(auth.fromSession(session), tableKey);
            }
        } catch (Exception e){
            LOG.error(e);
            if (StaticReportTable.LOG_TABLE_NAME.equals(tableKey)) {
                return staticMetadataProvider.getTableHandle(StaticReportTable.LOG_TABLE_NAME);
            } else {
                return null;
            }
        }
    }


    @Override
    public void truncateTable(ConnectorSession session, ConnectorTableHandle tableHandle) {
        CyodaTableHandle cTableHandle = (CyodaTableHandle) tableHandle;
        if (cTableHandle.getReportConfigId() != null){
            deleteReportsApiHandler.deleteReports(session, cTableHandle.getReportConfigId());
        } else {
            throw new TrinoException(NOT_SUPPORTED, "Truncate operation is available only for report-related tables. Use DELETE for cache and call stats.");
        }
    }

    @Override
    public OptionalLong executeDelete(ConnectorSession session, ConnectorTableHandle handle) {
        return ConnectorMetadata.super.executeDelete(session, handle);
    }

    @Override
    public Optional<ConnectorTableHandle> applyDelete(ConnectorSession session, ConnectorTableHandle handle) {
        return ConnectorMetadata.super.applyDelete(session, handle);
    }

    @Override
    public ColumnHandle getDeleteRowIdColumnHandle(ConnectorSession session, ConnectorTableHandle tableHandle) {
        CyodaTableHandle.TableType tableType = ((CyodaTableHandle)tableHandle).getTableType();
        return switch (tableType){
            case CALL_STATS -> staticMetadataProvider.getApiCallStats().getNodeIdColumn();
            case CACHE_CONTENT -> staticMetadataProvider.getCacheContent().getCacheKeyColumn();
            default -> ConnectorMetadata.super.getDeleteRowIdColumnHandle(session, tableHandle);
        };
    }

    @Override
    public Optional<ConnectorPartitioningHandle> getUpdateLayout(ConnectorSession session, ConnectorTableHandle tableHandle) {
        return Optional.of(new ConnectorPartitioningHandle(){});
    }

    @Override
    public ConnectorTableHandle beginDelete(ConnectorSession session, ConnectorTableHandle tableHandle, RetryMode retryMode) {
        return tableHandle;
    }

    @Override
    public void finishDelete(ConnectorSession session, ConnectorTableHandle tableHandle, Collection<Slice> fragments) {
    }

    @Override
    public ConnectorTableMetadata getTableMetadata(ConnectorSession session, ConnectorTableHandle table) {
        CyodaTableHandle handle = (CyodaTableHandle) table;
        return handle.getMetadata();
    }

    @Override
    public Map<String, ColumnHandle> getColumnHandles(ConnectorSession session, ConnectorTableHandle tableHandle) {
        CyodaTableHandle handle = (CyodaTableHandle) tableHandle;
        checkArgument(handle.getConnectorId().equals(connectorId), "tableHandle is not for this connector");
        return ImmutableMap.copyOf(handle.getColumnHandleMap());
    }

    @Override
    public ColumnMetadata getColumnMetadata(ConnectorSession session, ConnectorTableHandle tableHandle, ColumnHandle columnHandle) {
        return ((CyodaColumnHandle) columnHandle).getColumnMetadata();
    }


    @Override
    public List<SchemaTableName> listTables(ConnectorSession session, Optional<String> filterSchema) {
        try {
            if (filterSchema.isPresent() && !filterSchema.get().equals(config.getSchemaName())) {
                return Collections.emptyList();
            }
            LOG.info("Getting tables for schema %s", () -> filterSchema.orElse("ALL"));
            ImmutableList.Builder<SchemaTableName> builder = ImmutableList.builder();
            for (String tableName : staticMetadataProvider.getTableList()) {
                builder.add(new SchemaTableName(config.getSchemaName(), tableName));
            }
            for (String tableName : dynamicReportMetadataProvider.getTableList(auth.fromSession(session))) {
                builder.add(new SchemaTableName(config.getSchemaName(), tableName));
            }
            return builder.build();
        } catch (Exception e){
            LOG.error(e);
            return Collections.singletonList(new SchemaTableName(config.getSchemaName(),StaticReportTable.LOG_TABLE_NAME));
        }
    }

    private List<SchemaTableName> listTables(ConnectorSession session, SchemaTablePrefix prefix) {
        // List all tables if schema or table is null
        if (prefix.getSchema().isEmpty() || prefix.getTable().isEmpty()) {
            return listTables(session, prefix.getSchema());
        }

        // Make sure requested table exists, returning the single table of it does
        SchemaTableName table = new SchemaTableName(prefix.getSchema().get(), prefix.getTable().get());
        if (getTableHandle(session, table) != null) {
            return ImmutableList.of(table);
        }

        // Else, return empty list
        return ImmutableList.of();
    }

    @Override
    public Iterator<TableColumnsMetadata> streamTableColumns(ConnectorSession session, SchemaTablePrefix prefix) {
        requireNonNull(prefix, "prefix is null");
        ImmutableList.Builder<TableColumnsMetadata> columns = ImmutableList.builder();
        for (SchemaTableName tableName : listTables(session, prefix)) {
            CyodaTableHandle handle = (CyodaTableHandle) getTableHandle(session, tableName);
            // table can disappear during listing operation
            if (handle != null) {
                columns.add(new TableColumnsMetadata(tableName, Optional.of(handle.getColumnMetadata())));
            }
        }
        return columns.build().iterator();
    }
}
