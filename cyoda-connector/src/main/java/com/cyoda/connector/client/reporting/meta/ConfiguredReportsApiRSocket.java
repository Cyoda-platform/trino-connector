package com.cyoda.connector.client.reporting.meta;

import com.cyoda.api.view.GridConfigFieldsView;
import com.cyoda.connector.SizeListener;
import com.cyoda.connector.client.reporting.BaseRSocketReportsApiHandler;
import com.cyoda.connector.client.treenode.CyodaRSocketClient;
import com.google.inject.Inject;
import reactor.core.publisher.Flux;


public class ConfiguredReportsApiRSocket extends BaseRSocketReportsApiHandler implements ConfiguredReportsApi {

    @Inject
    public ConfiguredReportsApiRSocket(CyodaRSocketClient rSocketClient) {
        super(rSocketClient);
    }

    @Override
    public Flux<GridConfigFieldsView> asFlux(ReportListKey requestKey, SizeListener listener) {
        String userId = requestKey.authContext().getUserId();
        return rSocketClient.reportsClient.configsRequester.retrieveData(requestKey.queryId(), userId)
                .map(GridConfigFieldsView::new)
                .doOnNext(ConfiguredReportsApi::addSchemaTableName);
    }
}
