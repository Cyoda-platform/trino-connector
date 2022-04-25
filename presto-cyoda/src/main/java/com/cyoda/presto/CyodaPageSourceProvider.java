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

import com.cyoda.presto.client.CyodaApiRequestHandler;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.facebook.presto.common.predicate.TupleDomain;
import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.ConnectorPageSource;
import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.ConnectorSplit;
import com.facebook.presto.spi.ConnectorTableLayoutHandle;
import com.facebook.presto.spi.SplitContext;
import com.facebook.presto.spi.connector.ConnectorPageSourceProvider;
import com.facebook.presto.spi.connector.ConnectorTransactionHandle;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class CyodaPageSourceProvider implements ConnectorPageSourceProvider {

    private final String connectorId;
    private final CyodaClient client;

    @Inject
    public CyodaPageSourceProvider(CyodaConnectorId connectorId, CyodaClient client) {
        this.connectorId = requireNonNull(connectorId, "connectorId is null").toString();
        this.client = requireNonNull(client, "client is null");
    }

    @Override
    public ConnectorPageSource createPageSource(
            ConnectorTransactionHandle transactionHandle,
            ConnectorSession session,
            ConnectorSplit split,
            ConnectorTableLayoutHandle layout,
            List<ColumnHandle> columns,
            SplitContext splitContext
    ) {
        requireNonNull(split, "split is null");
        requireNonNull(splitContext, "splitContext is null");

        TupleDomain<?> constraint = ((CyodaSplit) split).getConstraint();
        String requestHandlerKey = ((CyodaSplit) split).getTableHandle().getRequestHandlerKey();
        CyodaApiRequestHandler<?> requestHandler = Optional.ofNullable(client.getRequestHandlerProvider().getHandler(requestHandlerKey))
                .orElseThrow(() -> new IllegalArgumentException("Handler " + requestHandlerKey + " not found"));
        //TODO: requestHandler.createCyodaFilteringPageSource(query,requestHandler,columns,client);
        CyodaTableHandle tableHandle = ((CyodaSplit) split).getTableHandle();
        return new CyodaFilteringPageSource(constraint, requestHandler, tableHandle, columns, client);
    }
}
