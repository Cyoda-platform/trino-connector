package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportTable;
import com.cyoda.presto.client.reporting.stats.ApiRequestStats;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.metadata.InternalNode;
import io.trino.metadata.InternalNodeManager;
import io.trino.metadata.NodeState;
import io.trino.spi.connector.Constraint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class ApiCallStatsDataProvider extends TableDataProvider<ApiRequestStats> {

    private final CyodaApiRequestStatsMonitor statsMonitor;

    private final InternalNodeManager internalNodeManager;
    private final InternalNode thisNode;

    public ApiCallStatsDataProvider(CyodaApiRequestStatsMonitor statsMonitor, InternalNodeManager internalNodeManager) {
        this.statsMonitor = statsMonitor;
        this.internalNodeManager = internalNodeManager;
        thisNode = internalNodeManager.getCurrentNode();
    }

    @Override
    public Iterable<ApiRequestStats> getIterable(AuthContext authContext, CyodaTableHandle tableHandle, CyodaSplit split) {
        return statsMonitor.getIterable();
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull ApiRequestStats entity, CyodaColumnHandle columnHandle) {
        StaticReportTable.ApiCallStatsColumnDef columnDef = StaticReportTable.ApiCallStatsColumnDef.valueOf(
                columnHandle.getColumnName().toUpperCase());
        switch (columnDef) {
            case QUERY_ID -> {
                return entity.queryId();
            }
            case NODE_ID -> {
                return thisNode.getNodeIdentifier();
            }
            case NODE_ADDRESS -> {
                return thisNode.getInternalUri();
            }
            case CALL_TIME -> {
                return entity.callTime();
            }
            case CALL_MILLIS -> {
                return entity.callTime().toInstant().getNano() / 1000000;
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
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        return internalNodeManager.getNodes(NodeState.ACTIVE)
                .stream()
                .map(InternalNode::getInternalUri)
                .map(uri -> CyodaSplit.addressedEmptySplit(tableHandle, queryId, uri))
                .collect(Collectors.toList()
                );
    }
}
