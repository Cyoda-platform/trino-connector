package com.cyoda.presto.client.reporting.meta;

import com.cyoda.core.model.reports.ReportHistoryFieldsView;
import com.cyoda.presto.client.reporting.BaseRSocketReportsApiHandler;
import com.cyoda.presto.client.treenode.CyodaRSocketClient;
import com.google.inject.Inject;

import java.util.List;

public class ReportHistoryApiRSocket extends BaseRSocketReportsApiHandler implements ReportHistoryApi {
    @Inject
    public ReportHistoryApiRSocket(CyodaRSocketClient rSocketClient) {
        super(rSocketClient);
    }

    @Override
    public List<ReportHistoryFieldsView> getByKey(ReportConfigKey requestKey) {
        return rSocketClient.reportsClient.historiesRequester.retrieveData(requestKey.queryId(), requestKey.configId())
                .map(ReportHistoryFieldsView::new)
                .collectList().block();
    }
}
