package com.cyoda.connector.client.data;

import com.cyoda.connector.client.reporting.metaproviders.StaticTableMetadata.CacheContentColumnDef;
import com.cyoda.connector.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.connector.client.reporting.stats.CyodaCacheMonitor.CacheContent;
import com.cyoda.connector.handles.CyodaColumnHandle;
import io.trino.spi.NodeManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CacheContentDataProvider extends VirtualTableDataProvider<CacheContent>{

    public CacheContentDataProvider(NodeManager nodeManager, CyodaCacheMonitor cacheMonitor) {
        super(nodeManager, cacheMonitor::getContent);
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull CacheContent entity, CyodaColumnHandle columnHandle) {
        CacheContentColumnDef columnDef = CacheContentColumnDef.valueOf(columnHandle.getColumnName().toUpperCase());
        switch (columnDef){
            case CONTENT_ID -> {
                return entity.contentId();
            }
            case NODE_ID -> {
                return thisNode.getNodeIdentifier();
            }
            case NODE_ADDRESS -> {
                return thisNode.getHostAndPort().toString();
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

}
