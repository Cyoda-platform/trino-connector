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
import com.facebook.presto.spi.SchemaTableName;
import com.google.common.collect.ImmutableMap;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class CyodaClient {
    /**
     * TableName -> TableMetadata
     */
    private final Supplier<Map<String, CyodaTable>> tables;


    private final CyodaConfig config;
    private final CyodaApiRequestHandlerProvider requestHandlerProvider;


    @Inject
    public CyodaClient(CyodaConnectorId id, CyodaConfig config, CyodaApiRequestHandlerProvider requestHandlerProvider) {
        this.config = requireNonNull(config, "config is null");
        this.requestHandlerProvider = requireNonNull(requestHandlerProvider, "requestHandlerProvider is null");

        tables = this::lookupSchema;
    }

    private Map<String, CyodaTable> lookupSchema() {

        ImmutableMap.Builder<String, CyodaTable> builder = ImmutableMap.builder();
        requestHandlerProvider.getHandlers().stream()
                .flatMap(r -> r.getTables().stream())
                .forEach(v -> builder.put(v.getName(), v));

        return builder.build();
    }


    public Set<String> getTableNames() {
        return tables.get().keySet();
    }

    public CyodaTable getTable(SchemaTableName tableName) {
        requireNonNull(tableName, "tableName is null");
        Map<String, CyodaTable> tableMap = tables.get();
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
