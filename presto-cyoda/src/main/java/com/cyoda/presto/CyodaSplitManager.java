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

import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.DynamicFilter;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.ConnectorSplitSource;
import io.trino.spi.connector.FixedSplitSource;
import io.trino.spi.connector.ConnectorSplitManager;
import io.trino.spi.connector.ConnectorTransactionHandle;
import com.google.common.base.Preconditions;

import javax.inject.Inject;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class CyodaSplitManager implements ConnectorSplitManager {
    private final String connectorId;
    private final CyodaClient cyodaClient;


    @Inject
    public CyodaSplitManager(CyodaConnectorId connectorId, CyodaClient exampleClient) {
        this.connectorId = requireNonNull(connectorId, "connectorId is null").toString();
        this.cyodaClient = requireNonNull(exampleClient, "client is null");
    }

    @Override
    public ConnectorSplitSource getSplits(
            ConnectorTransactionHandle handle,
            ConnectorSession session,
            ConnectorTableHandle connectorTableHandle,
            DynamicFilter dynamicFilter,
            Constraint constraint) {
        CyodaTableHandle tableHandle = (CyodaTableHandle) connectorTableHandle;
        Preconditions.checkArgument(tableHandle.getConnectorId().equals(connectorId),"This split manager is meant for connector id "+connectorId);
        CyodaTable table = cyodaClient.getTable(tableHandle);
        // this can happen if table is removed during a query
        checkState(table != null, "Table %s.%s no longer exists", tableHandle.getSchemaName(), tableHandle.getTableName());
        List<ConnectorSplit> splits = new ArrayList<>();
        for (URI uri : table.getSources()) {
            splits.add(new CyodaSplit(tableHandle, uri, constraint.getSummary()));
        }
        Collections.shuffle(splits);

        return new FixedSplitSource(splits);
    }
}
