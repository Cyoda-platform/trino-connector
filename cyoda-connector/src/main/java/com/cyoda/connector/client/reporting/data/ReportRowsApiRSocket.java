package com.cyoda.connector.client.reporting.data;

import com.cyoda.connector.CyodaConfig;
import com.cyoda.connector.CyodaSplit;
import com.cyoda.connector.client.reporting.BaseRSocketReportsApiHandler;
import com.cyoda.connector.client.treenode.CyodaRSocketClient;
import com.cyoda.connector.client.treenode.dto.ReportRequestDto;
import com.google.inject.Inject;

import java.util.concurrent.atomic.AtomicInteger;

public class ReportRowsApiRSocket extends BaseRSocketReportsApiHandler implements ReportRowsApi {
    private CyodaConfig config;
    @Inject
    public ReportRowsApiRSocket(CyodaRSocketClient rSocketClient, CyodaConfig config) {
        super(rSocketClient);
        this.config = config;
    }

    @Override
    public Iterable<RowHandle> getIterable(CyodaSplit split) {
        int pageSize = config.getRowRequestPageSize();
        int startRow = pageSize * split.getReportHandle().getPage();
        int endRow = startRow + pageSize - 1;
        AtomicInteger pageCounter = new AtomicInteger(startRow -1);
        String queryId = split.getQueryId();
        ReportRequestDto requestDto = new ReportRequestDto(split.getReportHandle(), startRow, endRow);
        return rSocketClient.reportsClient.rowsRequester.retrieveData(queryId, requestDto)
                .map(row -> new RowHandle(split.getReportHandle(), row, pageCounter.incrementAndGet()))
                .toIterable();
    }
}
