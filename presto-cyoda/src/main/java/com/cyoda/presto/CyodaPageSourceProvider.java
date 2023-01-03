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
import com.cyoda.presto.client.data.TableDataProviderProvider;
import com.cyoda.presto.client.logic.ColumnPredicateBuilder;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.DynamicFilter;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorPageSource;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.ConnectorPageSourceProvider;
import io.trino.spi.connector.ConnectorTransactionHandle;
import com.google.common.base.Preconditions;
import io.trino.spi.predicate.TupleDomain;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class CyodaPageSourceProvider implements ConnectorPageSourceProvider {

    private final String connectorId;
    private final TableDataProviderProvider dataProviderProvider;
    private final AuthService auth;

    @Inject
    public CyodaPageSourceProvider(CyodaConnectorId connectorId,
                                   TableDataProviderProvider dataProviderProvider,
                                   AuthService auth) {
        this.connectorId = requireNonNull(connectorId, "connectorId is null").toString();
        this.dataProviderProvider = requireNonNull(dataProviderProvider, "dataProviderProvider is null");
        this.auth = auth;
    }

    @Override
    public ConnectorPageSource createPageSource(
            ConnectorTransactionHandle transactionHandle,
            ConnectorSession session,
            ConnectorSplit split,
            ConnectorTableHandle tableHandle,
            List<ColumnHandle> columns,
            DynamicFilter dynamicFilter
    ) {
        requireNonNull(split, "split is null");
        CyodaTableHandle cyodaTableHandle = (CyodaTableHandle) tableHandle;
        Preconditions.checkArgument(connectorId.equals(cyodaTableHandle.getConnectorId()),"tableHandle not for this connectorId");
        List<CyodaColumnHandle> cyodaColumns = columns.stream().map(CyodaColumnHandle.class::cast).collect(Collectors.toList());
        AuthContext authContext = auth.fromSession(session);

        return new CyodaFilteringPageSource<>(
                authContext,
                dataProviderProvider.getDataProvider(cyodaTableHandle.getTableType()),
                cyodaTableHandle,
                cyodaColumns, ((CyodaSplit) split).getConstraint());

    }
}
