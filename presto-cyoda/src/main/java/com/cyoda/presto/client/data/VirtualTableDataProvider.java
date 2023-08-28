package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.CyodaVirtualPageSource;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import io.trino.spi.Node;
import io.trino.spi.NodeManager;
import io.trino.spi.block.Block;
import io.trino.spi.connector.ConnectorPageSource;
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

    protected abstract void deleteByIds(Block rowIds);

    @Override
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        return nodeManager.getAllNodes()
                .stream()
                .map(Node::getHttpUri)
                .map(uri -> CyodaSplit.addressedEmptySplit(tableHandle, queryId, uri))
                .collect(Collectors.toList()
                );
    }

    @Override
    public ConnectorPageSource getPageSource(CyodaTableMeta tableHandle, List<CyodaColumnHandle> cyodaColumns, CyodaSplit split) {
        return new CyodaVirtualPageSource<>(this, tableHandle, cyodaColumns, split, this::deleteByIds);
    }
}
