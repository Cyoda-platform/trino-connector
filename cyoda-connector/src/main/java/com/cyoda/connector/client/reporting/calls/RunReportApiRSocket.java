package com.cyoda.connector.client.reporting.calls;

import com.cyoda.connector.auth.AuthContext;
import com.cyoda.connector.client.reporting.BaseRSocketReportsApiHandler;
import com.cyoda.connector.client.reporting.meta.ReportConfigKey;
import com.cyoda.connector.client.treenode.CyodaRSocketClient;
import com.cyoda.connector.client.treenode.dto.ConfigRequestDto;
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
