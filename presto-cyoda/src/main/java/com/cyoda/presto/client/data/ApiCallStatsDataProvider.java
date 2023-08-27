package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata;
import com.cyoda.presto.client.reporting.stats.ApiRequestStats;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import io.trino.spi.NodeManager;
import io.trino.spi.block.Block;
import io.trino.spi.type.VarcharType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ApiCallStatsDataProvider extends VirtualTableDataProvider<ApiRequestStats> {

    private final CyodaApiRequestStatsMonitor statsMonitor;

    public ApiCallStatsDataProvider(CyodaApiRequestStatsMonitor statsMonitor, NodeManager nodeManager) {
        super(nodeManager);
        this.statsMonitor = statsMonitor;
    }

    @Override
    public Iterable<ApiRequestStats> getIterable(CyodaTableMeta tableHandle, CyodaSplit split) {
        return statsMonitor.getIterable();
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull ApiRequestStats entity, CyodaColumnHandle columnHandle) {
        StaticTableMetadata.ApiCallStatsColumnDef columnDef = StaticTableMetadata.ApiCallStatsColumnDef.valueOf(
                columnHandle.getColumnName().toUpperCase());
        switch (columnDef) {
            case QUERY_ID -> {
                return entity.queryId();
            }
            case NODE_ID -> {
                return thisNode.getNodeIdentifier();
            }
            case NODE_ADDRESS -> {
                return thisNode.getHttpUri();
            }
            case CALL_TIME -> {
                return entity.callTime();
            }
            case DURATION_MILLIS -> {
                return entity.duration();
            }
            case API_HANDLER -> {
                return entity.handlerName();
            }
            case REQUEST_PARAMS -> {
                return entity.params();
            }
            case REQUEST_URL -> {
                return entity.requestUrl();
            }
            case RESPONSE -> {
                return entity.response();
            }
        }
        throw new IllegalArgumentException("Unknown column " + columnHandle.getColumnName());
    }

    @Override
    protected void deleteByIds(Block rowIds) {
        for (int position = 0; position < rowIds.getPositionCount(); position++) {
            String requestNodeId = VarcharType.VARCHAR.getSlice(rowIds, position).toStringUtf8();
            if (thisNode.getNodeIdentifier().equals(requestNodeId)){
                statsMonitor.truncate();
            }
        }
    }
}
