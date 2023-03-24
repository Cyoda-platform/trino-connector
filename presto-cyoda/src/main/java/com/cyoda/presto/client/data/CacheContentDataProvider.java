package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportTable;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportTable.CacheContentColumnDef;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportTable.CacheStatsColumnDef;
import com.cyoda.presto.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.presto.client.reporting.stats.CyodaCacheMonitor.CacheContent;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.trino.spi.NodeManager;
import io.trino.spi.block.Block;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class CacheContentDataProvider extends VirtualTableDataProvider<CacheContent>{

    private final CyodaCacheMonitor cacheMonitor;

    public CacheContentDataProvider(NodeManager nodeManager, CyodaCacheMonitor cacheMonitor) {
        super(nodeManager);
        this.cacheMonitor = cacheMonitor;
    }

    @Override
    public Iterable<CacheContent> getIterable(CyodaTableHandle tableHandle, CyodaSplit split) {
        return cacheMonitor.getContent();
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull CacheContent entity, CyodaColumnHandle columnHandle) {
        CacheContentColumnDef columnDef = CacheContentColumnDef.valueOf(columnHandle.getColumnName().toUpperCase());
        switch (columnDef){
            case NODE_ID -> {
                return thisNode.getNodeIdentifier();
            }
            case NODE_ADDRESS -> {
                return thisNode.getHttpUri();
            }
            case CACHE_NAME -> {
                return entity.cacheName();
            }
            case KEY -> {
                return entity.key();
            }
            case SIZE -> {
                return entity.size();
            }
            default -> throw new RuntimeException("Unknown field " + columnDef);
        }
    }

    @Override
    protected void deleteByIds(Block rowIds) {
        throw new RuntimeException("not yet");
    }
}
