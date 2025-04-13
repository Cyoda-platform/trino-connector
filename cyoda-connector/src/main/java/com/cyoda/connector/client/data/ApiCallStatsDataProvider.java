package com.cyoda.connector.client.data;

import com.cyoda.connector.CyodaSplit;
import com.cyoda.connector.client.reporting.metaproviders.StaticTableMetadata;
import com.cyoda.connector.client.reporting.stats.ApiRequestStats;
import com.cyoda.connector.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.handles.CyodaTableMeta;
import io.trino.spi.NodeManager;
import io.trino.spi.block.Block;
import io.trino.spi.type.VarcharType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ApiCallStatsDataProvider extends VirtualTableDataProvider<ApiRequestStats> {

    public ApiCallStatsDataProvider(CyodaApiRequestStatsMonitor statsMonitor, NodeManager nodeManager) {
        super(nodeManager, statsMonitor);
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
                return thisNode.getHostAndPort().toString();
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
            case REQUEST -> {
                return entity.request();
            }
            case REQUEST_ROUTE -> {
                return entity.requestUrl();
            }
            case RESPONSE -> {
                return entity.response();
            }
        }
        throw new IllegalArgumentException("Unknown column " + columnHandle.getColumnName());
    }

}
