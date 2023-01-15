package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportTable;
import com.cyoda.presto.client.reporting.stats.ApiRequestStats;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ApiCallStatsDataProvider extends UnsplitTableDataProvider<ApiRequestStats>{

    private final CyodaApiRequestStatsMonitor statsMonitor;

    public ApiCallStatsDataProvider(CyodaApiRequestStatsMonitor statsMonitor) {
        this.statsMonitor = statsMonitor;
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
        switch (columnDef){
            case QUERY_ID -> {
                return entity.queryId();
            }
            case CALL_TIME -> {
                return entity.callTime();
            }
            case DURATION -> {
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
}
