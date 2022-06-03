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
import com.cyoda.presto.client.CyodaApiRequestHandlerProvider;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.spi.SchemaTableName;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class CyodaClient {

    private static final SupplierLogger LOG = SupplierLogger.get(CyodaClient.class);

    /**
     * TableName -> TableMetadata
     */
    private final Function<AuthContext,Map<String, CyodaTable>> tableFunction;
    private final LoadingCache<AuthContext,Map<String, CyodaTable>> tableCache;


    private final CyodaConnectorId connectorId;
    private final CyodaConfig config;
    private final CyodaApiRequestHandlerProvider requestHandlerProvider;


    @Inject
    public CyodaClient(CyodaConnectorId id, CyodaConfig config, CyodaApiRequestHandlerProvider requestHandlerProvider) {
        this.connectorId = id;
        this.config = requireNonNull(config, "config is null");
        this.requestHandlerProvider = requireNonNull(requestHandlerProvider, "requestHandlerProvider is null");

        tableFunction = tableFunction();
        tableCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(2))
                .build(key -> {
                    LOG.debug("Reloading Tables Cache");
                    return tableFunction.apply(key);
                });
    }

    private Function<AuthContext, Map<String, CyodaTable>> tableFunction() {
        return this::lookupSchema;
    }

    private Map<String, CyodaTable> lookupSchema(AuthContext authContext) {

        ImmutableMap.Builder<String, CyodaTable> builder = ImmutableMap.builder();
        requestHandlerProvider.getHandlers().stream()
                .flatMap(r -> r.getTables(authContext).stream())
                .forEach(v -> builder.put(v.getName(), v));

        return builder.build();
    }


    public Set<String> getTableNames(AuthContext authContext) {
        return Optional.ofNullable(tableCache.get(authContext)).map(Map::keySet)
                .orElseThrow(()->new IllegalArgumentException("Cannot load tables")
        );
    }

    public CyodaTable getTable(AuthContext authContext, SchemaTableName tableName) {
        requireNonNull(tableName, "tableName is null");
        Map<String, CyodaTable> tableMap = tableCache.get(authContext);
        if (tableMap == null) {
            return null;
        }
        return tableMap.get(tableName.getTableName().toUpperCase());

    }

    public String getSchemaName() {
        return config.getSchemaName();
    }

    public CyodaTable getTable(CyodaTableHandle tableHandle) {
        return getTable(tableHandle.getAuthPayload(),new SchemaTableName(tableHandle.getSchemaName(), tableHandle.getTableName()));
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
