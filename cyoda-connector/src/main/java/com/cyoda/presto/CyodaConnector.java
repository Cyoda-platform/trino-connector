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

import com.cyoda.presto.handles.CyodaTransactionHandle;
import com.cyoda.presto.logging.SupplierLogger;
import io.airlift.bootstrap.LifeCycleManager;
import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorMetadata;
import io.trino.spi.connector.ConnectorNodePartitioningProvider;
import io.trino.spi.connector.ConnectorPageSourceProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplitManager;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.procedure.Procedure;
import io.trino.spi.transaction.IsolationLevel;

import jakarta.inject.Inject;

import java.util.Set;

import static java.util.Objects.requireNonNull;

public class CyodaConnector implements Connector {

    private static final SupplierLogger log = SupplierLogger.get(CyodaConnector.class);

    private final LifeCycleManager lifeCycleManager;
    private final CyodaMetadata metadata;
    private final CyodaSplitManager splitManager;
    private final CyodaPageSourceProvider pageSourceProvider;
    private final CyodaProcedureManager procedureManager;
    private final CyodaNodePartitioningProvider nodePartitioningProvider;

    @Inject
    public CyodaConnector(
            LifeCycleManager lifeCycleManager,
            CyodaMetadata metadata,
            CyodaSplitManager splitManager,
            CyodaPageSourceProvider pageSourceProvider,
            CyodaProcedureManager procedureManager,
            CyodaNodePartitioningProvider nodePartitioningProvider
    ) {
        this.lifeCycleManager = requireNonNull(lifeCycleManager, "lifeCycleManager is null");
        this.metadata = requireNonNull(metadata, "metadata is null");
        this.splitManager = requireNonNull(splitManager, "splitManager is null");
        this.pageSourceProvider = requireNonNull(pageSourceProvider, "pageSourceProvider is null");
        this.procedureManager = requireNonNull(procedureManager, "procedureManager is null");
        this.nodePartitioningProvider = nodePartitioningProvider;
    }
    @Override
    public ConnectorTransactionHandle beginTransaction(IsolationLevel isolationLevel, boolean readOnly, boolean autoCommit) {
        return CyodaTransactionHandle.INSTANCE;
    }

    @Override
    public ConnectorMetadata getMetadata(ConnectorSession session, ConnectorTransactionHandle transactionHandle) {
        return metadata;
    }

    @Override
    public ConnectorNodePartitioningProvider getNodePartitioningProvider() {
        return nodePartitioningProvider;
    }

    @Override
    public ConnectorSplitManager getSplitManager() {
        return splitManager;
    }

    @Override
    public ConnectorPageSourceProvider getPageSourceProvider() {
        return pageSourceProvider;
    }

    @Override
    public Set<Procedure> getProcedures() {
        return procedureManager.getProcedures();
    }

    @Override
    public final void shutdown() {
        try {
            lifeCycleManager.stop();
        } catch (Exception e) {
            log.error(e, "Error shutting down connector");
        }
    }
}
