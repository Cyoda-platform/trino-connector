package com.cyoda.presto.client.reporting.groups;

import com.cyoda.presto.client.reporting.BaseRSocketReportsApiHandler;
import com.cyoda.presto.client.treenode.CyodaRSocketClient;
import com.cyoda.presto.client.treenode.dto.ReportRequestDto;
import com.google.inject.Inject;

import java.util.List;

public class ReportGroupsApiRSocket extends BaseRSocketReportsApiHandler implements ReportGroupsApi {
    @Inject
    public ReportGroupsApiRSocket(CyodaRSocketClient rSocketClient) {
        super(rSocketClient);
    }

    @Override
    public List<GroupingHandle> getByKey(GroupsRequestKey requestKey) {
        ReportRequestDto requestDto = new ReportRequestDto(requestKey.reportId(), requestKey.groupingVersion(), null, 0L, Long.MAX_VALUE);
        return rSocketClient.reportsClient.groupRequester.retrieveData(requestKey.queryId(), requestDto)
                .map(groupHeader ->  new GroupingHandle(requestKey.reportId(), requestKey.groupingVersion(), groupHeader))
                .collectList().block();
    }
}
