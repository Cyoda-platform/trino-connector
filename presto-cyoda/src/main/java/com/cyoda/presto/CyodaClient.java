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

import com.cyoda.presto.client.CyodaApiRequestHandlerProvider;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.spi.SchemaTableName;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class CyodaClient {

    private static final SupplierLogger LOG = SupplierLogger.get(CyodaClient.class);

    /**
     * TableName -> TableMetadata
     */
    private final Supplier<Map<String, CyodaTable>> tablesSupplier;
    private final LoadingCache<CyodaConnectorId,Map<String, CyodaTable>> tableCache;


    private final CyodaConnectorId connectorId;
    private final CyodaConfig config;
    private final CyodaApiRequestHandlerProvider requestHandlerProvider;


    @Inject
    public CyodaClient(CyodaConnectorId id, CyodaConfig config, CyodaApiRequestHandlerProvider requestHandlerProvider) {
        this.connectorId = id;
        this.config = requireNonNull(config, "config is null");
        this.requestHandlerProvider = requireNonNull(requestHandlerProvider, "requestHandlerProvider is null");

        tablesSupplier = this::lookupSchema;
        tableCache = CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(10))
                .build(new CacheLoader<CyodaConnectorId, Map<String, CyodaTable>>() {
                    @Override
                    public Map<String, CyodaTable> load(@Nonnull CyodaConnectorId key) throws Exception {
                        if (key.equals(id)) {
                            LOG.debug("Reloading Tables Cache");
                            return tablesSupplier.get();
                        } else {
                            throw new IllegalArgumentException("Must ask for the tables for connection id " + id);
                        }
                    }
                });
    }

    private Map<String, CyodaTable> lookupSchema() {

        ImmutableMap.Builder<String, CyodaTable> builder = ImmutableMap.builder();
        requestHandlerProvider.getHandlers().stream()
                .flatMap(r -> r.getTables().stream())
                .forEach(v -> builder.put(v.getName(), v));

        return builder.build();
    }


    // TODO: Determine the correct places where a refresh should be done. Currently, it is being called more than
    // once when querying the schema via sql. We want to refresh once when the schema is queried.
    public void refreshTableCache() {
        tableCache.refresh(connectorId);
    }

    public Set<String> getTableNames() {
        return tableCache.getUnchecked(connectorId).keySet();
    }

    public CyodaTable getTable(SchemaTableName tableName) {
        requireNonNull(tableName, "tableName is null");
        Map<String, CyodaTable> tableMap = tableCache.getUnchecked(connectorId);
        if (tableMap == null) {
            return null;
        }
        return tableMap.get(tableName.getTableName().toUpperCase());

    }

    public String getSchemaName() {
        return config.getSchemaName();
    }

    public CyodaTable getTable(String schemaName, String tableName) {
        return getTable(new SchemaTableName(schemaName, tableName));
    }

    public CyodaApiRequestHandlerProvider getRequestHandlerProvider() {
        return requestHandlerProvider;
    }

    public int getRequestPageSize() {
        return config.getRequestPageSize();
    }

    public enum CyodaAuthenticationType {
        NONE,
        BASIC,
        JWT
    }


}
