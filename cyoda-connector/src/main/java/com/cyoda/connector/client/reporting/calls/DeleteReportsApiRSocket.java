package com.cyoda.connector.client.reporting.calls;

import com.cyoda.connector.client.reporting.BaseRSocketReportsApiHandler;
import com.cyoda.connector.client.treenode.CyodaRSocketClient;
import com.cyoda.connector.client.treenode.dto.ConfigRequestDto;
import com.google.inject.Inject;
import io.trino.spi.connector.ConnectorSession;

public class DeleteReportsApiRSocket extends BaseRSocketReportsApiHandler implements DeleteReportsApi {
    @Inject
    public DeleteReportsApiRSocket(CyodaRSocketClient rSocketClient) {
        super(rSocketClient);
    }

    @Override
    public String deleteReports(ConnectorSession session, String configId) {
        String queryId = session.getQueryId();
        ConfigRequestDto configRequestDto = new ConfigRequestDto(configId, session.getUser());
        return rSocketClient.reportsClient.deleteRequester.retrieveData(queryId, configRequestDto).block();
    }
}
