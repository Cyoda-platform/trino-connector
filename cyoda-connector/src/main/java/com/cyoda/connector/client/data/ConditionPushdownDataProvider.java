package com.cyoda.connector.client.data;

import com.cyoda.connector.client.reporting.metaproviders.StaticTableMetadata;
import com.cyoda.connector.client.reporting.stats.ConditionPushdownLog;
import com.cyoda.connector.client.reporting.stats.ConditionPushdownLogMonitor;
import com.cyoda.connector.handles.CyodaColumnHandle;
import io.trino.spi.NodeManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConditionPushdownDataProvider extends VirtualTableDataProvider<ConditionPushdownLog> {

    public ConditionPushdownDataProvider(ConditionPushdownLogMonitor statsMonitor, NodeManager nodeManager) {
        super(nodeManager, statsMonitor);
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull ConditionPushdownLog entity, CyodaColumnHandle columnHandle) {
        StaticTableMetadata.ConditionPushdownLogColumnDef columnDef = StaticTableMetadata.ConditionPushdownLogColumnDef.valueOf(
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
            case CODE_POINT -> {
                return entity.codePoint();
            }
            case DOMAIN -> {
                return entity.domainCondition();
            }
            case EXPRESSION -> {
                return entity.expression();
            }
            case ACCEPTED -> {
                return entity.acceptedCondition();
            }
            case REMAINING -> {
                return entity.remainingCondition();
            }
        }
        throw new IllegalArgumentException("Unknown column " + columnHandle.getColumnName());
    }

}
