package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.stats.ApiRequestStats;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.Node;
import io.trino.spi.NodeManager;
import io.trino.spi.connector.Constraint;

import java.util.List;
import java.util.stream.Collectors;

public abstract class VirtualTableDataProvider<T> extends TableDataProvider<T> {
    protected final NodeManager nodeManager;
    protected final Node thisNode;

    public VirtualTableDataProvider(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        thisNode = nodeManager.getCurrentNode();
    }

    @Override
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        return nodeManager.getWorkerNodes()
                .stream()
                .map(Node::getHttpUri)
                .map(uri -> CyodaSplit.addressedEmptySplit(tableHandle, queryId, uri))
                .collect(Collectors.toList()
                );
    }
}
