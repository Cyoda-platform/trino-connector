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

package com.cyoda.connector;

import com.cyoda.connector.auth.AuthContext;
import com.cyoda.connector.auth.AuthService;
import com.cyoda.connector.client.logic.PredicatePushdownController;
import com.cyoda.connector.client.reporting.calls.DeleteReportsApi;
import com.cyoda.connector.client.reporting.metaproviders.*;
import com.cyoda.connector.client.reporting.stats.ContentIdLoadingCache;
import com.cyoda.connector.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.connector.client.treenode.CyodaRSocketClient;
import com.cyoda.connector.client.treenode.dto.view.TrinoViewDefinitionDto;
import com.cyoda.connector.client.treenode.dto.view.TrinoViewDto;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.handles.CyodaTableHandle;
import com.cyoda.connector.handles.CyodaTableMeta;
import com.cyoda.connector.handles.CyodaTableType;
import com.cyoda.connector.logging.SupplierLogger;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.trino.spi.StandardErrorCode;
import io.trino.spi.TrinoException;
import io.trino.spi.connector.*;
import io.trino.spi.expression.ConnectorExpression;
import io.trino.spi.predicate.Domain;
import io.trino.spi.predicate.TupleDomain;

import javax.annotation.Nonnull;

import jakarta.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.trino.spi.StandardErrorCode.MISSING_SCHEMA_NAME;
import static io.trino.spi.StandardErrorCode.NOT_SUPPORTED;
import static java.util.Objects.requireNonNull;

public class CyodaMetadata implements ConnectorMetadata {

    private static final SupplierLogger LOG = SupplierLogger.get(CyodaMetadata.class);

    private final CyodaConfig config;
    private final AuthService auth;
    private final StaticTableMetadataProvider staticMetadataProvider;
    private final DynamicReportMetadataProvider dynamicReportMetadataProvider;
    private final TreeNodeMetadataProvider treeNodeMetadataProvider;
    private final DeleteReportsApi deleteReportsApiHandler;
    private final ContentIdLoadingCache<AuthContext, Map<SchemaTableName, CyodaTableHandle>> tableByUserCache;
    private final Map<SchemaTableName, CyodaTableHandle> defaultTableList;
    private final CyodaRSocketClient rSocketClient;

    @Inject
    public CyodaMetadata(
            CyodaConfig config,
            AuthService auth,
            StaticTableMetadataProvider staticMetadataProvider,
            DynamicReportMetadataProvider dynamicReportMetadataProvider,
            TreeNodeMetadataProvider treeNodeMetadataProvider,
            DeleteReportsApi deleteReportsApiHandler,
            CyodaCacheMonitor cacheMonitor,
            CyodaRSocketClient rSocketClient) {
        this.config = requireNonNull(config,"confif is null");
        this.auth = auth;
        this.staticMetadataProvider = staticMetadataProvider;
        this.dynamicReportMetadataProvider = dynamicReportMetadataProvider;
        this.treeNodeMetadataProvider = treeNodeMetadataProvider;
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
        this.rSocketClient = rSocketClient;
    }

    @Nonnull
    private Map<SchemaTableName, CyodaTableHandle> tableByUserCacheLoad(AuthContext key) {
        try {
            Map<SchemaTableName, List<CyodaTableHandle>> tableNameListMap = Stream.of(
                    staticMetadataProvider,
                    dynamicReportMetadataProvider,
                    treeNodeMetadataProvider)
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
            case TREE_NODE_TABLE -> {
                return treeNodeMetadataProvider;
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
        return schemaMap.get(tableName);
    }


    @Override
    public void truncateTable(ConnectorSession session, ConnectorTableHandle tableHandle) {
        CyodaTableHandle cTableHandle = (CyodaTableHandle) tableHandle;
        if (cTableHandle.getTableMetaId() != null){
            deleteReportsApiHandler.deleteReports(session, cTableHandle.getTableMetaId());
        } else {
            throw new TrinoException(NOT_SUPPORTED, "Truncate operation is available only for report-related tables, cache_content and api_call_stats.");
        }
    }

//    @Override
//    public ColumnHandle getDeleteRowIdColumnHandle(ConnectorSession session, ConnectorTableHandle tableHandle) {
//        CyodaTableType tableType = ((CyodaTableHandle)tableHandle).getTableType();
//        return switch (tableType){
//            case CALL_STATS -> staticMetadataProvider.getApiCallStats().getNodeIdColumn();
//            case CACHE_CONTENT -> staticMetadataProvider.getCacheContent().getCacheKeyColumn();
//            default -> ConnectorMetadata.super.getDeleteRowIdColumnHandle(session, tableHandle);
//        };
//    }


//    @Override
//    public RowChangeParadigm getRowChangeParadigm(ConnectorSession session, ConnectorTableHandle tableHandle) {
//        return RowChangeParadigm.DELETE_ROW_AND_INSERT_ROW;
//    }
//
//    @Override
//    public ColumnHandle getMergeRowIdColumnHandle(ConnectorSession session, ConnectorTableHandle tableHandle) {
//        CyodaTableType tableType = ((CyodaTableHandle)tableHandle).getTableType();
//        return switch (tableType){
//            case CALL_STATS -> staticMetadataProvider.getApiCallStats().getNodeIdColumn();
//            case CACHE_CONTENT -> staticMetadataProvider.getCacheContent().getCacheKeyColumn();
//            default -> ConnectorMetadata.super.getMergeRowIdColumnHandle(session, tableHandle);
//        };
//    }
//
//    @Override
//    public Optional<ConnectorPartitioningHandle> getUpdateLayout(ConnectorSession session, ConnectorTableHandle tableHandle) {
//        return Optional.empty();
//    }
//
//    @Override
//    public ConnectorMergeTableHandle beginMerge(ConnectorSession session, ConnectorTableHandle tableHandle, RetryMode retryMode) {
//        return new CyodaMergeTableHandle(tableHandle);
//    }
//
//    @Override
//    public void finishMerge(ConnectorSession session, ConnectorMergeTableHandle mergeTableHandle, Collection<Slice> fragments, Collection<ComputedStatistics> computedStatistics) {
//        LOG.debug("-=FRAGMENTS" + Strings.collectionToCommaDelimitedString(fragments));
//        LOG.debug("-=STATS" + Strings.collectionToCommaDelimitedString(computedStatistics));
//    }

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
    public Optional<ConstraintApplicationResult<ConnectorTableHandle>> applyFilter(ConnectorSession session, ConnectorTableHandle handle, Constraint constraint) {

        CyodaTableHandle tableHandle = (CyodaTableHandle) handle;
        if(!tableHandle.getTableType().isPushdownSupported()) return Optional.empty();
        TupleDomain<ColumnHandle> oldDomain = tableHandle.getConstraint();
        TupleDomain<ColumnHandle> newDomain = oldDomain.intersect(constraint.getSummary());
        TupleDomain<ColumnHandle> remainingFilter;
        if (newDomain.isNone()) {
            remainingFilter = TupleDomain.all();
        } else {
            Map<ColumnHandle, Domain> domains = newDomain.getDomains().orElseThrow();
            List<CyodaColumnHandle> columnHandles = domains.keySet().stream()
                    .map(CyodaColumnHandle.class::cast)
                    .collect(toImmutableList());


            Map<ColumnHandle, Domain> supported = new HashMap<>();
            Map<ColumnHandle, Domain> unsupported = new HashMap<>();
            for (CyodaColumnHandle column : columnHandles) {
                PredicatePushdownController.DomainPushdownResult pushdownResult =
                        column.getDataType().getMainType().getPushDownController()
                                .apply(config.getPredicatePushdownThreshold(), domains.get(column));
                supported.put(column, pushdownResult.getPushedDown());
                unsupported.put(column, pushdownResult.getRemainingFilter());
            }

            newDomain = TupleDomain.withColumnDomains(supported);
            remainingFilter = TupleDomain.withColumnDomains(unsupported);

        }

        if (oldDomain.equals(newDomain)) {
            return Optional.empty();
        }

        tableHandle.setConstraint(newDomain);

        return Optional.of(new ConstraintApplicationResult<>(tableHandle, remainingFilter, constraint.getExpression(), true));
    }

    @Override
    public Optional<ProjectionApplicationResult<ConnectorTableHandle>> applyProjection(ConnectorSession session, ConnectorTableHandle handle, List<ConnectorExpression> projections, Map<String, ColumnHandle> assignments) {
        CyodaTableHandle cyodaTableHandle = (CyodaTableHandle) handle;
        if (cyodaTableHandle.getSelectedFields() == null){
            cyodaTableHandle.setSelectedFields(assignments.values().stream().map(col -> ((CyodaColumnHandle) col).getColumnKey()).toList());
        }
        return ConnectorMetadata.super.applyProjection(session, handle, projections, assignments);
    }

    @Override
    public Optional<TopNApplicationResult<ConnectorTableHandle>> applyTopN(ConnectorSession session, ConnectorTableHandle handle, long topNCount, List<SortItem> sortItems, Map<String, ColumnHandle> assignments) {
        CyodaTableHandle cyodaTableHandle = (CyodaTableHandle) handle;
        if (cyodaTableHandle.getSortingFields() == null && cyodaTableHandle.getLimit() == null){
            cyodaTableHandle.setLimit(topNCount);
            List<String> sortingFields = new ArrayList<>();
            for (SortItem item : sortItems){
                switch (item.getSortOrder()) {
                    case ASC_NULLS_FIRST -> {
                        CyodaColumnHandle columnHandle = (CyodaColumnHandle) assignments.get(item.getName());
                        requireNonNull(columnHandle);
                        sortingFields.add(columnHandle.getExternalName());
                    }
                    case DESC_NULLS_FIRST -> {
                        CyodaColumnHandle columnHandle = (CyodaColumnHandle) assignments.get(item.getName());
                        requireNonNull(columnHandle);
                        sortingFields.add("-"+columnHandle.getExternalName());
                    }
                    case ASC_NULLS_LAST, DESC_NULLS_LAST -> {
                        //nulls last are not supported
                    }
                }
            }
            cyodaTableHandle.setSortingFields(sortingFields);
        }
        return ConnectorMetadata.super.applyTopN(session, handle, topNCount, sortItems, assignments);
    }

    @Override
    public List<SchemaTableName> listTables(ConnectorSession session, Optional<String> filterSchema) {
        LOG.info("Getting tables for schema %s", () -> filterSchema.orElse("ALL"));
        try {
            Map<SchemaTableName, CyodaTableHandle> tableMap = getTableHandleMap(session);
            return filterSchema.map(s ->
                            Stream.of(tableMap.keySet(), rSocketClient.viewClient.getViews(auth.fromSession(session).getUserId()).keySet()).flatMap(Collection::stream)
                                    .filter(e -> s.equals(e.getSchemaName()))
                                    .collect(Collectors.toList()))
                    .orElseGet(() -> ImmutableList.copyOf(tableMap.keySet()));
        } catch (Exception e){
            LOG.error(e);
            return defaultTableList.keySet().stream().toList();
        }
    }

    @Override
    public synchronized void createView(ConnectorSession session, SchemaTableName viewName, ConnectorViewDefinition definition, boolean replace)
    {

        if (viewName.getSchemaName()==null) {
            throw new TrinoException(MISSING_SCHEMA_NAME,"view is missing schema name: " + viewName.getTableName());
        }
        String queryId = session.getQueryId();
        String userId = auth.fromSession(session).getUserId();
        TrinoViewDto data = new TrinoViewDto(userId, viewName, TrinoViewDefinitionDto.fromModel(definition, viewName), null, replace);
        String response = rSocketClient.viewClient.addRequester.retrieveData(queryId, data).block();
        if (response != null)
            throw new RuntimeException(response);
    }

    @Override
    public synchronized void renameView(ConnectorSession session, SchemaTableName viewName, SchemaTableName newViewName)
    {
        String userId = auth.fromSession(session).getUserId();
        TrinoViewDto data = new TrinoViewDto(userId, viewName, null, newViewName, false);
        String response = rSocketClient.viewClient.renameRequester.retrieveData(session.getQueryId(), data).block();
        if (response != null) throw new TrinoException(StandardErrorCode.REMOTE_TASK_FAILED, response);
    }

    @Override
    public synchronized void dropView(ConnectorSession session, SchemaTableName viewName)
    {
        String userId = auth.fromSession(session).getUserId();
        TrinoViewDto data = new TrinoViewDto(userId, viewName, null, null, true);
        String response = rSocketClient.viewClient.deleteRequester.retrieveData(session.getQueryId(), data).block();
        if (response != null) throw new TrinoException(StandardErrorCode.REMOTE_TASK_FAILED, response);
    }

    @Override
    public synchronized List<SchemaTableName> listViews(ConnectorSession session, Optional<String> schemaName)
    {
        return rSocketClient.viewClient.getViews(auth.fromSession(session).getUserId()).keySet().stream()
                .filter(viewName -> schemaName.map(viewName.getSchemaName()::equals).orElse(true))
                .collect(toImmutableList());
    }
    @Override
    public synchronized Map<SchemaTableName, ConnectorViewDefinition> getViews(ConnectorSession session, Optional<String> schemaName)
    {
        SchemaTablePrefix prefix = schemaName.map(SchemaTablePrefix::new).orElseGet(SchemaTablePrefix::new);
        return ImmutableMap.copyOf(Maps.filterKeys(rSocketClient.viewClient.getViews(auth.fromSession(session).getUserId()), prefix::matches));
    }
    @Override
    public synchronized Optional<ConnectorViewDefinition> getView(ConnectorSession session, SchemaTableName viewName)
    {
        return Optional.ofNullable(rSocketClient.viewClient.getViews(auth.fromSession(session).getUserId()).get(viewName));
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

    public static class CyodaMergeTableHandle implements ConnectorMergeTableHandle {
        private final ConnectorTableHandle tableHandle;

        @JsonCreator
        public CyodaMergeTableHandle(@JsonProperty ConnectorTableHandle tableHandle) {
            this.tableHandle = tableHandle;
        }

        @Override
        @JsonProperty
        public ConnectorTableHandle getTableHandle() {
            return tableHandle;
        }
    }
}
