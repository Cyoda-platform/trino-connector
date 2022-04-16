package com.cyoda.presto;

import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableLayoutHandle;
import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.ConnectorSplit;
import com.facebook.presto.spi.ConnectorSplitSource;
import com.facebook.presto.spi.ConnectorTableLayoutHandle;
import com.facebook.presto.spi.FixedSplitSource;
import com.facebook.presto.spi.connector.ConnectorSplitManager;
import com.facebook.presto.spi.connector.ConnectorTransactionHandle;

import javax.inject.Inject;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class CyodaSplitManager implements ConnectorSplitManager {
    private final String connectorId;
    private final CyodaClient cyodaClient;

    @Inject
    public CyodaSplitManager(CyodaConnectorId connectorId, CyodaClient exampleClient)
    {
        this.connectorId = requireNonNull(connectorId, "connectorId is null").toString();
        this.cyodaClient = requireNonNull(exampleClient, "client is null");
    }

    @Override
    public ConnectorSplitSource getSplits(
            ConnectorTransactionHandle handle,
            ConnectorSession session,
            ConnectorTableLayoutHandle layout,
            SplitSchedulingContext splitSchedulingContext)
    {
        CyodaTableLayoutHandle layoutHandle = (CyodaTableLayoutHandle) layout;
        CyodaTableHandle tableHandle = layoutHandle.getTable();
        CyodaTable table = cyodaClient.getTable(tableHandle.getSchemaName(), tableHandle.getTableName());
        // this can happen if table is removed during a query
        checkState(table != null, "Table %s.%s no longer exists", tableHandle.getSchemaName(), tableHandle.getTableName());

        List<ConnectorSplit> splits = new ArrayList<>();
        for (URI uri : table.getSources()) {
            splits.add(new CyodaSplit(connectorId, tableHandle.getSchemaName(), tableHandle.getTableName(), uri));
        }
        Collections.shuffle(splits);

        return new FixedSplitSource(splits);
    }
}
