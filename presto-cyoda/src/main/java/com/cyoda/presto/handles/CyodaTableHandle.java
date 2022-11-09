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

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.auth.AuthContext;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.SchemaTableName;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class CyodaTableHandle implements ConnectorTableHandle {
    private final String connectorId;
    private final String schemaName;
    private final String tableName;
    private final Optional<List<CyodaColumnHandle>> projectedColumns;
    private final String requestHandlerKey;
    private final AuthContext authContext;

    @JsonCreator
    public CyodaTableHandle(
            @JsonProperty("authPayload") AuthContext authContext,
            @JsonProperty("connectorId") String connectorId,
            @JsonProperty("schemaName") String schemaName,
            @JsonProperty("tableName") String tableName,
            @JsonProperty("projectedColumns") Optional<List<CyodaColumnHandle>> projectedColumns,
            @JsonProperty("requestHandlerKey") String requestHandlerKey
    ) {
        this.authContext = requireNonNull(authContext,"authPayload is null");
        this.connectorId = requireNonNull(connectorId, "connectorId is null");
        this.schemaName = requireNonNull(schemaName, "schemaName is null");
        this.tableName = requireNonNull(tableName, "tableName is null");
        this.projectedColumns = requireNonNull(projectedColumns, "projectedColumns is null");
        this.requestHandlerKey = requireNonNull(requestHandlerKey, "requestHandlerKey is null");
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
    public Optional<List<CyodaColumnHandle>> getProjectedColumns() {
        return projectedColumns;
    }

    @JsonProperty
    public String getRequestHandlerKey() {
        return requestHandlerKey;
    }

    @JsonProperty
    public AuthContext getAuthPayload() {
        return authContext;
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
                && tableName.equals(that.tableName)
                && requestHandlerKey.equals(that.requestHandlerKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectorId, schemaName, tableName, requestHandlerKey);
    }

    @Override
    public String toString() {
        return Joiner.on(":").join(connectorId, schemaName, requestHandlerKey);
    }

    public CyodaTableHandle withProjectedColumns(List<CyodaColumnHandle> newProjectedColumns) {
        return new CyodaTableHandle(authContext,connectorId, schemaName, tableName, Optional.of(newProjectedColumns), requestHandlerKey);
    }

    public CyodaTableHandle withSessionConfig(ConnectorSession session, CyodaConfig config) {
        return new CyodaTableHandle(
                AuthContext.fromSession(session,config),
                this.connectorId,
                this.getSchemaName(),
                this.getTableName(),
                this.getProjectedColumns(),
                this.requestHandlerKey
        );
    }
}
