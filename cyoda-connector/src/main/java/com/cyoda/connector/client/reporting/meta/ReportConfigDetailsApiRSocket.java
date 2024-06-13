package com.cyoda.connector.client.reporting.meta;

import com.cyoda.connector.client.reporting.BaseRSocketReportsApiHandler;
import com.cyoda.connector.client.treenode.CyodaRSocketClient;
import com.google.inject.Inject;
import io.trino.spi.type.TypeManager;


public class ReportConfigDetailsApiRSocket extends BaseRSocketReportsApiHandler implements ReportConfigDetailsApi {
    protected final TypeManager typeManager;
    @Inject
    public ReportConfigDetailsApiRSocket(CyodaRSocketClient rSocketClient, TypeManager typeManager) {
        super(rSocketClient);
        this.typeManager = typeManager;
    }

    @Override
    public ReportDefinitionHandle getReportDefSingleHandle(ReportConfigKey reportConfigKey) {
        String configId = reportConfigKey.configId();
        String jsonResult;
//        try {
//            //for some reason mono.block does not work
//            jsonResult = rSocketClient.reports().getReportDefinition(configId).toFuture().get();
//        } catch (InterruptedException | ExecutionException e) {
//            throw new RuntimeException(e);
//        }
        jsonResult = rSocketClient.reportsClient.definitionRequester.retrieveData(reportConfigKey.queryId(), configId).blockOptional().get();

        return ReportConfigParser.parseReportDefinitionHandle(jsonResult, configId, typeManager);
    }
}
