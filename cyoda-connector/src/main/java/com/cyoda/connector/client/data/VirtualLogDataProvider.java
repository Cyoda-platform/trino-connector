package com.cyoda.connector.client.data;

import com.cyoda.connector.CyodaSplit;
import com.cyoda.connector.client.reporting.metaproviders.StaticTableMetadata;
import com.cyoda.connector.client.reporting.stats.ApiRequestStats;
import com.cyoda.connector.client.reporting.stats.BaseVirtualLogMonitor;
import com.cyoda.connector.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.handles.CyodaTableMeta;
import io.trino.spi.NodeManager;
import io.trino.spi.block.Block;
import io.trino.spi.type.VarcharType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class VirtualLogDataProvider<T> extends VirtualTableDataProvider<T> {

    private final BaseVirtualLogMonitor<T> monitor;

    public VirtualLogDataProvider(BaseVirtualLogMonitor<T> monitor, NodeManager nodeManager) {
        super(nodeManager);
        this.monitor = monitor;
    }

    @Override
    public Iterable<T> getIterable(CyodaTableMeta tableHandle, CyodaSplit split) {
        return monitor.getIterable();
    }


    @Override
    protected void deleteByIds(Block rowIds) {
        for (int position = 0; position < rowIds.getPositionCount(); position++) {
            String requestNodeId = VarcharType.VARCHAR.getSlice(rowIds, position).toStringUtf8();
            if (thisNode.getNodeIdentifier().equals(requestNodeId)){
                monitor.truncate();
            }
        }
    }
}
