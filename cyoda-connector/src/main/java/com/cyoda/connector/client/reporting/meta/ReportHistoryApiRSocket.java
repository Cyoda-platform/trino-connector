package com.cyoda.connector.client.reporting.meta;

import com.cyoda.core.model.reports.ReportHistoryFieldsView;
import com.cyoda.connector.CyodaConfig;
import com.cyoda.connector.client.reporting.CachedRSocketReportsApiHandler;
import com.cyoda.connector.client.reporting.stats.ContentIdLoadingCache;
import com.cyoda.connector.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.connector.client.treenode.CyodaRSocketClient;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;

import java.time.Duration;
import java.util.List;

public class ReportHistoryApiRSocket extends CachedRSocketReportsApiHandler<ReportConfigKey, ReportHistoryFieldsView> implements ReportHistoryApi {
    @Inject
    protected ReportHistoryApiRSocket(CyodaRSocketClient rSocketClient, CyodaCacheMonitor cacheMonitor, CyodaConfig config) {
        super(rSocketClient, cacheMonitor, config);
    }

    @Override
    public List<ReportHistoryFieldsView> loadByKey(ReportConfigKey requestKey) {
        return rSocketClient.reportsClient.historiesRequester.retrieveData(requestKey.queryId(), requestKey.configId())
                .map(ReportHistoryFieldsView::new)
                .collectList().block();
    }
    @Override
    protected LoadingCache<ReportConfigKey, List<ReportHistoryFieldsView>> setupCache(CacheLoader<ReportConfigKey, List<ReportHistoryFieldsView>> loader) {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(config.getCacheReportHistorySecAfterWrite()))
                .recordStats()
                .build(loader);
    }

    @Override
    protected void registerCache(CyodaCacheMonitor cacheMonitor, ContentIdLoadingCache<ReportConfigKey, List<ReportHistoryFieldsView>> cache) {
        cacheMonitor.register("HISTORY", cache,
                ReportConfigKey::configId, List::size);
    }
}
