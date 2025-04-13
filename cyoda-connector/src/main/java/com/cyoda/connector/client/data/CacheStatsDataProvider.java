package com.cyoda.connector.client.data;

import com.cyoda.connector.client.reporting.metaproviders.StaticTableMetadata.CacheStatsColumnDef;
import com.cyoda.connector.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.trino.spi.NodeManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class CacheStatsDataProvider extends VirtualTableDataProvider<Map.Entry<String, CacheStats>>{


    public CacheStatsDataProvider(NodeManager nodeManager, CyodaCacheMonitor cacheMonitor) {
        super(nodeManager, () -> cacheMonitor.getStats().entrySet());
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull Map.Entry<String, CacheStats> entity, CyodaColumnHandle columnHandle) {
        CacheStatsColumnDef columnDef = CacheStatsColumnDef.valueOf(columnHandle.getColumnName().toUpperCase());
        switch (columnDef){
            case NODE_ID -> {
                return thisNode.getNodeIdentifier();
            }
            case NODE_ADDRESS -> {
                return thisNode.getHostAndPort().toString();
            }
            case CACHE_NAME -> {
                return entity.getKey();
            }
            case HIT_COUNT -> {
                return entity.getValue().hitCount();
            }
            case MISS_COUNT -> {
                return entity.getValue().missCount();
            }
            case LOAD_SUCCESS_COUNT -> {
                return entity.getValue().loadSuccessCount();
            }
            case LOAD_FAILURE_COUNT -> {
                return entity.getValue().loadFailureCount();
            }
            case TOTAL_LOAD_TIME -> {
                return entity.getValue().totalLoadTime();
            }
            case EVICTION_COUNT -> {
                return entity.getValue().evictionCount();
            }
            case EVICTION_WEIGHT -> {
                return entity.getValue().evictionWeight();
            }
            default -> throw new RuntimeException("Unknown field " + columnDef);
        }
    }

}
