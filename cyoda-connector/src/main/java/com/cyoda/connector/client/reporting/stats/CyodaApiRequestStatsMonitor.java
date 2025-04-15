package com.cyoda.connector.client.reporting.stats;

import com.cyoda.connector.CyodaConfig;

import jakarta.inject.Inject;
import java.util.Date;
import java.util.Map;

public class CyodaApiRequestStatsMonitor extends BaseVirtualLogMonitor<ApiRequestStats> {

    @Inject
    public CyodaApiRequestStatsMonitor(CyodaConfig config){
        super(config);
    };

    public void registerApiCall(String queryId, Date callTime, String requestUrl, String apiHandlerName, Map<String, String> request, Object response){
        add(
                new ApiRequestStats(
                        queryId,
                        callTime,
                        requestUrl,
                        apiHandlerName,
                        System.currentTimeMillis() - callTime.getTime(),
                        request,
                        response));
    }

    @Override
    protected long getMaxRecords(CyodaConfig config) {
        return config.getApiCallStatsMaxRecords();
    }

}
