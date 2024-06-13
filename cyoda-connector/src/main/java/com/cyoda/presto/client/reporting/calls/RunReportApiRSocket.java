package com.cyoda.presto.client.reporting.calls;

import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.BaseRSocketReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigKey;
import com.cyoda.presto.client.treenode.CyodaRSocketClient;
import com.cyoda.presto.client.treenode.dto.ConfigRequestDto;
import com.google.inject.Inject;

public class RunReportApiRSocket extends BaseRSocketReportsApiHandler implements RunReportApi {
    @Inject
    public RunReportApiRSocket(CyodaRSocketClient rSocketClient) {
        super(rSocketClient);
    }

    @Override
    public String runReport(String queryId, AuthContext authContext, ReportConfigKey reportConfigKey) {
        ConfigRequestDto configRequestDto = new ConfigRequestDto(reportConfigKey.configId(), authContext.getUserId());
        return rSocketClient.reportsClient.runRequester.retrieveData(queryId, configRequestDto).block();
    }
}
