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
import com.cyoda.presto.client.reporting.metaproviders.DynamicReportMetadataProvider;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportMetadataProvider;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class CyodaMetadata implements ConnectorMetadata {

    private static final SupplierLogger LOG = SupplierLogger.get(CyodaMetadata.class);

    private final String connectorId;
    private final CyodaConfig config;
    private final AuthService auth;
    private final StaticReportMetadataProvider staticMetadataProvider;
    private final DynamicReportMetadataProvider dynamicReportMetadataProvider;

    @Inject
    public CyodaMetadata(
            CyodaConnectorId connectorId,
            CyodaConfig config,
            AuthService auth,
            StaticReportMetadataProvider staticMetadataProvider,
            DynamicReportMetadataProvider dynamicReportMetadataProvider) {
        this.connectorId = requireNonNull(connectorId, "connectorId is null").toString();
        this.config = requireNonNull(config,"confif is null");
        this.auth = auth;
        this.staticMetadataProvider = staticMetadataProvider;
        this.dynamicReportMetadataProvider = dynamicReportMetadataProvider;
    }


    @Override
    public List<String> listSchemaNames(ConnectorSession session) {
        return ImmutableList.of(config.getSchemaName());
    }

    @Override
    public ConnectorTableHandle getTableHandle(ConnectorSession session, SchemaTableName tableName) {
        if (!listSchemaNames(session).contains(tableName.getSchemaName())) {
            return null;
        }
        String tableKey = tableName.getTableName().toUpperCase();
        if (staticMetadataProvider.contains(tableKey)){
            return staticMetadataProvider.getTableHandle(tableKey);
        } else {
            return dynamicReportMetadataProvider.getTableHandle(auth.fromSession(session), tableKey);
        }
    }

//    @Override
//    public List<ConnectorTableLayoutResult> getTableLayouts(ConnectorSession session, ConnectorTableHandle table, Constraint<ColumnHandle> constraint, Optional<Set<ColumnHandle>> desiredColumns) {
//        CyodaTableHandle tableHandle = (desiredColumns.isPresent()) ?
//                ((CyodaTableHandle) table).withProjectedColumns(convertDesiredColumns(desiredColumns.orElse(Collections.emptySet()))) :
//                ((CyodaTableHandle) table);
//        TupleDomain<CyodaColumnHandle> summary = constraint.getSummary().transform(CyodaColumnHandle.class::cast);
//        ConnectorTableLayout layout = new ConnectorTableLayout(
//                new CyodaTableLayoutHandle(
//                        tableHandle,
//                        summary
//                )
//        );
//        return ImmutableList.of(new ConnectorTableLayoutResult(layout, constraint.getSummary()));
//    }
//
//    private List<CyodaColumnHandle> convertDesiredColumns(Set<ColumnHandle> desiredColumns) {
//        return desiredColumns.stream().map(CyodaColumnHandle.class::cast).collect(Collectors.toList());
//    }
//
//    @Override
//    public ConnectorTableLayout getTableLayout(ConnectorSession session, ConnectorTableLayoutHandle handle) {
//        return new ConnectorTableLayout(handle);
//    }


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
        if ( filterSchema.isPresent() && !filterSchema.get().equals(config.getSchemaName()) ) {
            return Collections.emptyList();
        }
        LOG.info("Getting tables for schema %s",()->filterSchema.orElse("ALL"));
        ImmutableList.Builder<SchemaTableName> builder = ImmutableList.builder();
        for (String tableName : staticMetadataProvider.getTableList()) {
            builder.add(new SchemaTableName(config.getSchemaName(), tableName));
        }
        for (String tableName : dynamicReportMetadataProvider.getTableList(auth.fromSession(session))) {
            builder.add(new SchemaTableName(config.getSchemaName(), tableName));
        }
        return builder.build();
    }

    private List<SchemaTableName> listTables(ConnectorSession session, SchemaTablePrefix prefix) {
        // List all tables if schema or table is null
        if (!prefix.getSchema().isPresent() || !prefix.getTable().isPresent()) {
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
