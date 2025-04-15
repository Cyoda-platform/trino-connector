package com.cyoda.connector.client.data;

import com.cyoda.connector.CyodaSplit;
import com.cyoda.connector.auth.AuthContext;
import com.cyoda.connector.client.reporting.stats.VirtualDataSet;
import com.cyoda.connector.handles.CyodaTableHandle;
import com.cyoda.connector.handles.CyodaTableMeta;
import io.trino.spi.Node;
import io.trino.spi.NodeManager;
import io.trino.spi.connector.Constraint;

import java.util.List;
import java.util.stream.Collectors;

public abstract class VirtualTableDataProvider<T> extends TableDataProvider<T> {
    protected final NodeManager nodeManager;
    protected final Node thisNode;
    protected final VirtualDataSet<T> dataSet;

    public VirtualTableDataProvider(NodeManager nodeManager, VirtualDataSet<T> dataSet) {
        this.nodeManager = nodeManager;
        thisNode = nodeManager.getCurrentNode();
        this.dataSet = dataSet;
    }

    @Override
    public Iterable<T> getIterable(CyodaTableMeta tableHandle, CyodaSplit split) {
        return dataSet.getIterable();
    }

    @Override
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        return nodeManager.getAllNodes()
                .stream()
                .map(Node::getHostAndPort)
                .map(uri -> CyodaSplit.addressedEmptySplit(queryId, authContext.getUserId(), uri, tableHandle))
                .collect(Collectors.toList()
                );
    }

}
