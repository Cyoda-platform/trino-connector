package com.cyoda.connector.client.reporting.data;

import com.cyoda.connector.CyodaSplit;
import com.cyoda.connector.client.reporting.BaseRSocketReportsApiHandler;
import com.cyoda.connector.client.treenode.CyodaRSocketClient;
import com.cyoda.connector.client.treenode.dto.ReportRequestDto;
import com.google.inject.Inject;

import java.util.concurrent.atomic.AtomicInteger;

public class ReportRowsApiRSocket extends BaseRSocketReportsApiHandler implements ReportRowsApi {
    @Inject
    public ReportRowsApiRSocket(CyodaRSocketClient rSocketClient) {
        super(rSocketClient);
    }

    @Override
    public Iterable<RowHandle> getIterable(CyodaSplit split) {
        int startRow = split.getPageSize() * split.getPage();
        int endRow = startRow + split.getPageSize() - 1;
        AtomicInteger pageCounter = new AtomicInteger(startRow -1);
        String queryId = split.getQueryId();
        String reportId = split.getReportId();
        String groupJsonBase64 = split.getGroupJsonBase64();
        ReportRequestDto requestDto = new ReportRequestDto(reportId, null, groupJsonBase64, (long) startRow, (long) endRow);
        return rSocketClient.reportsClient.rowsRequester.retrieveData(queryId, requestDto)
                .map(row -> new RowHandle(split.getReportId(), split.getGroupingVersion(), split.getGroupJsonBase64(), row, pageCounter.incrementAndGet()))
                .toIterable();
    }
}
