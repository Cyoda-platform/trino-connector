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
import com.cyoda.presto.client.ApiRequestHandler;
import com.cyoda.presto.client.CyodaApiRequestHandlerProvider;
import com.cyoda.presto.client.logic.ColumnPredicateBuilder;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.connector.ConnectorTableHandle;
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
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class CyodaPageSourceProvider implements ConnectorPageSourceProvider {

    private final String connectorId;
    private final CyodaApiRequestHandlerProvider handlerProvider;
    private final AuthService auth;

    @Inject
    public CyodaPageSourceProvider(CyodaConnectorId connectorId,
                                   CyodaApiRequestHandlerProvider handlerProvider,
                                   AuthService auth) {
        this.connectorId = requireNonNull(connectorId, "connectorId is null").toString();
        this.handlerProvider = requireNonNull(handlerProvider, "handlerProvider is null");
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
        TupleDomain<ColumnHandle> constraint = ((CyodaSplit) split).getConstraint();
        CompoundPredicateNode predicates = ColumnPredicateBuilder.setupConstraintPredicates(constraint);
        CyodaTableHandle cyodaTableHandle = (CyodaTableHandle) tableHandle;
        String requestHandlerKey = cyodaTableHandle.getRequestHandlerKey();
        ApiRequestHandler<?> requestHandler = Optional.ofNullable(handlerProvider.getHandler(requestHandlerKey))
                .orElseThrow(() -> new IllegalArgumentException("Handler " + requestHandlerKey + " not found"));
        Preconditions.checkArgument(connectorId.equals(cyodaTableHandle.getConnectorId()),"tableHandle not for this connectorId");
        List<CyodaColumnHandle> cyodaColumns = columns.stream().map(CyodaColumnHandle.class::cast).collect(Collectors.toList());
        return new CyodaFilteringPageSource<>(auth.fromSession(session),
                requestHandler, cyodaTableHandle, cyodaColumns, predicates);
    }
}
