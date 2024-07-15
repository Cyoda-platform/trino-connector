package com.cyoda.connector.client.data;

import com.cyoda.connector.CyodaSplit;
import com.cyoda.connector.CyodaVirtualPageSource;
import com.cyoda.connector.auth.AuthContext;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.handles.CyodaTableHandle;
import com.cyoda.connector.handles.CyodaTableMeta;
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
                .map(Node::getHostAndPort)
                .map(uri -> CyodaSplit.addressedEmptySplit(queryId, authContext.getUserId(), uri, tableHandle))
                .collect(Collectors.toList()
                );
    }

    @Override
    public ConnectorPageSource getPageSource(CyodaTableMeta tableHandle, List<CyodaColumnHandle> cyodaColumns, CyodaSplit split) {
        return new CyodaVirtualPageSource<>(this, tableHandle, cyodaColumns, split, this::deleteByIds);
    }
}
