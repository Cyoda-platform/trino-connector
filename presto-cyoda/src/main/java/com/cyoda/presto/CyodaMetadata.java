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

import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.reporting.calls.DeleteReportsApiHandler;
import com.cyoda.presto.client.reporting.metaproviders.DynamicReportMetadataProvider;
import com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata;
import com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadataProvider;
import com.cyoda.presto.client.reporting.metaproviders.TableMetadataProvider;
import com.cyoda.presto.client.reporting.stats.ContentIdLoadingCache;
import com.cyoda.presto.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import com.cyoda.presto.handles.CyodaTableType;
import com.cyoda.presto.logging.SupplierLogger;
import com.github.benmanes.caffeine.cache.Caffeine;
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

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final ContentIdLoadingCache<AuthContext, Map<SchemaTableName, CyodaTableHandle>> tableByUserCache;
    private final Map<SchemaTableName, CyodaTableHandle> defaultTableList;

    @Inject
    public CyodaMetadata(
            CyodaConnectorId connectorId,
            CyodaConfig config,
            AuthService auth,
            StaticTableMetadataProvider staticMetadataProvider,
            DynamicReportMetadataProvider dynamicReportMetadataProvider,
            DeleteReportsApiHandler deleteReportsApiHandler,
            CyodaCacheMonitor cacheMonitor) {
        this.connectorId = requireNonNull(connectorId, "connectorId is null").toString();
        this.config = requireNonNull(config,"confif is null");
        this.auth = auth;
        this.staticMetadataProvider = staticMetadataProvider;
        this.dynamicReportMetadataProvider = dynamicReportMetadataProvider;
        this.deleteReportsApiHandler = deleteReportsApiHandler;
        tableByUserCache = new ContentIdLoadingCache<>(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(config.getCacheUserAuthSecAfterWrite()))
                .recordStats()
                .build(key -> {
                    LOG.debug("Loading Tables Cache for user " + key.getUserId());
                    return tableByUserCacheLoad(key);
                }));
        cacheMonitor.register("AUTH", tableByUserCache, AuthContext::getUserId, Map::size);
        defaultTableList = new HashMap<>();
        defaultTableList.put(new SchemaTableName(config.getSchemaName(), StaticTableMetadata.LOG_TABLE_NAME),
                new CyodaTableHandle(config.getSchemaName(), StaticTableMetadata.LOG_TABLE_NAME, CyodaTableType.LOG_TABLE));
    }

    @Nonnull
    private Map<SchemaTableName, CyodaTableHandle> tableByUserCacheLoad(AuthContext key) {
        try {
            Map<SchemaTableName, List<CyodaTableHandle>> tableNameListMap = Stream.of(staticMetadataProvider, dynamicReportMetadataProvider)
                    .flatMap(x -> x.listTables(key).stream())
                    .collect(Collectors.groupingBy(tableHandle -> new SchemaTableName(tableHandle.getSchemaName(), tableHandle.getTableName())));
            Map<SchemaTableName, CyodaTableHandle> result = tableNameListMap.entrySet().stream()
                    .filter(e -> (e.getValue().size() == 1)).collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
            if (result.size() < tableNameListMap.size()) {
                tableNameListMap.entrySet().stream().filter(e -> !result.containsKey(e.getKey())).forEach(entry -> {
                    LOG.error("Unable to load table " + entry.getKey() + " amount of linked handles <> 1 \n\n" + entry.getValue());
                });
            }
            return result;
        } catch (Exception e) {
            LOG.error(e,"Failed to load tables for user " + key.getUserId());
            return defaultTableList;
        }
    }

    private TableMetadataProvider getMetaProvider(CyodaTableHandle tableHandle){
        switch (tableHandle.getTableType()) {
            case DATA -> {
                return dynamicReportMetadataProvider;
            }
//            case HISTORY, GROUP -> It is important to note that while handles for those types of tables are provided
                                //   by dynamicReportMetadataProvider, their META is static and provided by staticMetadataProvider
            default -> {
                return staticMetadataProvider;
            }
        }
    }

    public CyodaTableMeta getTableMeta(CyodaTableHandle tableHandle){
        return getMetaProvider(tableHandle).getTableMeta(tableHandle);
    }

    @Override
    public List<String> listSchemaNames(ConnectorSession session) {
        Map<SchemaTableName, CyodaTableHandle> schemaMap = getTableHandleMap(session);
        return schemaMap.keySet().stream().map(SchemaTableName::getSchemaName).distinct().collect(Collectors.toList());
    }

    @Nonnull
    private Map<SchemaTableName, CyodaTableHandle> getTableHandleMap(ConnectorSession session) {
        Map<SchemaTableName, CyodaTableHandle> result = tableByUserCache.get(auth.fromSession(session));
        requireNonNull(result);
        return result;
    }

    @Override
    public ConnectorTableHandle getTableHandle(ConnectorSession session, SchemaTableName tableName) {
        Map<SchemaTableName, CyodaTableHandle> schemaMap = getTableHandleMap(session);
        CyodaTableHandle handle = schemaMap.get(tableName);
        requireNonNull(handle, "Unknown table " + tableName);
        return handle;
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
    public ColumnHandle getDeleteRowIdColumnHandle(ConnectorSession session, ConnectorTableHandle tableHandle) {
        CyodaTableType tableType = ((CyodaTableHandle)tableHandle).getTableType();
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
        CyodaTableMeta handle = getTableMeta((CyodaTableHandle) table);
        return handle.getMetadata();
    }

    @Override
    public Map<String, ColumnHandle> getColumnHandles(ConnectorSession session, ConnectorTableHandle tableHandle) {
        CyodaTableMeta handle = getTableMeta((CyodaTableHandle) tableHandle);
        return ImmutableMap.copyOf(handle.getColumnHandleMap());
    }

    @Override
    public ColumnMetadata getColumnMetadata(ConnectorSession session, ConnectorTableHandle tableHandle, ColumnHandle columnHandle) {
        return ((CyodaColumnHandle) columnHandle).getColumnMetadata();
    }


    @Override
    public List<SchemaTableName> listTables(ConnectorSession session, Optional<String> filterSchema) {
        LOG.info("Getting tables for schema %s", () -> filterSchema.orElse("ALL"));
        try {
            Map<SchemaTableName, CyodaTableHandle> tableMap = getTableHandleMap(session);
            return filterSchema.map(s ->
                    tableMap.keySet().stream()
                            .filter(e -> s.equals(e.getSchemaName()))
                            .collect(Collectors.toList()))
                    .orElseGet(() -> ImmutableList.copyOf(tableMap.keySet()));
        } catch (Exception e){
            LOG.error(e);
            return defaultTableList.keySet().stream().toList();
        }
    }

    @Override
    public Iterator<TableColumnsMetadata> streamTableColumns(ConnectorSession session, SchemaTablePrefix prefix) {
        requireNonNull(prefix, "prefix is null");
        ImmutableList.Builder<TableColumnsMetadata> columns = ImmutableList.builder();
        Map<SchemaTableName, CyodaTableHandle> tableMap = getTableHandleMap(session);
        Map<SchemaTableName, CyodaTableHandle> selectedHandles;
        if (prefix.getTable().isPresent()){
            SchemaTableName schemaTableName = prefix.toSchemaTableName();
            CyodaTableHandle tableHandle = tableMap.get(schemaTableName);
            if (tableHandle != null){
                selectedHandles = Collections.singletonMap(schemaTableName, tableHandle);
            } else {
                selectedHandles = Collections.emptyMap();
            }
        } else if (prefix.getSchema().isPresent()) {
            String schema = prefix.getSchema().get();
            selectedHandles = tableMap.entrySet().stream()
                    .filter(e -> schema.equals(e.getKey().getSchemaName()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            selectedHandles = tableMap;
        }


        for (Map.Entry<SchemaTableName, CyodaTableHandle> en : selectedHandles.entrySet()) {
            CyodaTableMeta tableMeta = getTableMeta(en.getValue());
            // table can disappear during listing operation
            if (tableMeta != null) {
                columns.add(new TableColumnsMetadata(en.getKey(), Optional.of(tableMeta.getColumnMetadata())));
            }
        }
        return columns.build().iterator();
    }
}
