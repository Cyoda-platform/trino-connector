package com.cyoda.presto;

import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.spi.ColumnHandle;
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

public class CyodaRecordSetProvider implements ConnectorRecordSetProvider {
    private final String connectorId;
    private final CyodaClient client;

    @Inject
    public CyodaRecordSetProvider(CyodaConnectorId connectorId,CyodaClient client)
    {
        this.connectorId = requireNonNull(connectorId, "connectorId is null").toString();
        this.client = requireNonNull(client, "client is null");
    }

    @Override
    public RecordSet getRecordSet(ConnectorTransactionHandle transactionHandle, ConnectorSession session, ConnectorSplit split, List<? extends ColumnHandle> columns)
    {
        requireNonNull(split, "partitionChunk is null");
        CyodaSplit cyodaSplit = (CyodaSplit) split;
        checkArgument(cyodaSplit.getConnectorId().equals(connectorId), "split is not for this connector");

        ImmutableList.Builder<CyodaColumnHandle> handles = ImmutableList.builder();
        for (ColumnHandle handle : columns) {
            handles.add((CyodaColumnHandle) handle);
        }

        return new CyodaRecordSet(client,cyodaSplit, handles.build());
    }
}
