package com.cyoda.presto.client.reporting.groups;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.client.reporting.BaseRSocketReportsApiHandler;
import com.cyoda.presto.client.reporting.CachedRSocketReportsApiHandler;
import com.cyoda.presto.client.reporting.stats.ContentIdLoadingCache;
import com.cyoda.presto.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.presto.client.treenode.CyodaRSocketClient;
import com.cyoda.presto.client.treenode.dto.ReportRequestDto;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;

import java.time.Duration;
import java.util.List;

public class ReportGroupsApiRSocket extends CachedRSocketReportsApiHandler<GroupsRequestKey, GroupingHandle> implements ReportGroupsApi {


    @Inject
    protected ReportGroupsApiRSocket(CyodaRSocketClient rSocketClient, CyodaCacheMonitor cacheMonitor, CyodaConfig config) {
        super(rSocketClient, cacheMonitor, config);
    }

    @Override
    public List<GroupingHandle> loadByKey(GroupsRequestKey requestKey) {
        ReportRequestDto requestDto = new ReportRequestDto(requestKey.reportId(), requestKey.groupingVersion(), null, 0L, Long.MAX_VALUE);
        return rSocketClient.reportsClient.groupRequester.retrieveData(requestKey.queryId(), requestDto)
                .map(groupHeader ->  new GroupingHandle(requestKey.reportId(), requestKey.groupingVersion(), groupHeader))
                .collectList().block();
    }
    @Override
    protected LoadingCache<GroupsRequestKey, List<GroupingHandle>> setupCache(CacheLoader<GroupsRequestKey, List<GroupingHandle>> loader) {
        return Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(config.getCacheReportGroupsHoursAfterAccess()))
                .recordStats()
                .build(loader);
    }
    @Override
    protected void registerCache(CyodaCacheMonitor cacheMonitor, ContentIdLoadingCache<GroupsRequestKey, List<GroupingHandle>> cache) {
        cacheMonitor.register("GROUPS", cache, GroupsRequestKey::reportId, List::size);
    }
}
