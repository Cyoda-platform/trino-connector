package com.cyoda.connector.client.reporting.meta;

import com.cyoda.core.reports.DistributedReportInfoDto;
import com.cyoda.connector.SizeListener;
import com.cyoda.connector.client.reporting.BaseRSocketReportsApiHandler;
import com.cyoda.connector.client.treenode.CyodaRSocketClient;
import com.google.inject.Inject;
import reactor.core.publisher.Flux;

public class ReportStatisticsApiRSocket extends BaseRSocketReportsApiHandler implements ReportStatisticsApi {
    @Inject
    public ReportStatisticsApiRSocket(CyodaRSocketClient rSocketClient) {
        super(rSocketClient);
    }

    @Override
    public Flux<DistributedReportInfoDto> asFlux(ReportConfigKey requestKey, SizeListener listener) {
        return rSocketClient.reportsClient.statsRequester.retrieveData(requestKey.queryId(), requestKey.configId());
    }
}
