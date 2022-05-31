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

import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.ConnectorPageSource;
import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.ConnectorSplit;
import com.facebook.presto.spi.RecordSet;
import com.facebook.presto.spi.connector.ConnectorRecordSetProvider;
import com.facebook.presto.spi.connector.ConnectorTransactionHandle;
import com.google.common.collect.ImmutableList;

import javax.inject.Inject;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * This is not really needed. Presto will basically use the RecordCursor to
 * create pages. Since we have a {@link ConnectorPageSource}, we don't need this.
 * If you want to activate it, you need to add {@link CyodaRecordSetProvider}
 * to the {@link CyodaModule} and also override {@link CyodaConnector#getRecordSetProvider}
 * and return this.
 *
 * The RecordCursor stuff was implemented to see if there was a better performance by using
 * a cursor rather than pages. But in fact Presto just pages the stuff further up the stack.
 * Perhaps in the future, Presto may offer a JDBC cursor and maybe this will become relevant.
 * Right now it isn't
 */
public class CyodaRecordSetProvider implements ConnectorRecordSetProvider {
    private final String connectorId;
    private final CyodaClient client;

    @Inject
    public CyodaRecordSetProvider(CyodaConnectorId connectorId, CyodaClient client) {
        this.connectorId = requireNonNull(connectorId, "connectorId is null").toString();
        this.client = requireNonNull(client, "client is null");
    }

    @Override
    public RecordSet getRecordSet(ConnectorTransactionHandle transactionHandle, ConnectorSession session, ConnectorSplit split, List<? extends ColumnHandle> columns) {
        requireNonNull(split, "partitionChunk is null");
        CyodaSplit cyodaSplit = (CyodaSplit) split;
        checkArgument(cyodaSplit.getTableHandle().getConnectorId().equals(connectorId), "split is not for this connector");

        ImmutableList.Builder<CyodaColumnHandle> handles = ImmutableList.builder();
        for (ColumnHandle handle : columns) {
            handles.add((CyodaColumnHandle) handle);
        }

        return new CyodaRecordSet<>(client, cyodaSplit, handles.build());
    }
}
