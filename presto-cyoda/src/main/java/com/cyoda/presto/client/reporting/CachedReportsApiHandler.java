package com.cyoda.presto.client.reporting;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.logging.SupplierLogger;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.trino.spi.type.TypeManager;

import java.time.Duration;
import java.util.List;

public abstract class CachedReportsApiHandler<K, T> extends BaseReportsApiHandler {
    protected final LoadingCache<K, List<T>> cache;

    public CachedReportsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager, RestTemplateCustomizer restTemplateCustomizer, SupplierLogger log, AuthService authService, CyodaApiRequestStatsMonitor requestStatsMonitor) {
        super(connectorId, config, typeManager, restTemplateCustomizer, log, authService, requestStatsMonitor);
        cache = Caffeine.newBuilder()
                .expireAfterAccess(getCacheDuration())
                .build(this::loadByKey);
    }

    protected abstract Duration getCacheDuration();

    public List<T> getByKey(K requestKey) {
        return cache.get(requestKey);
    }

    protected abstract List<T> loadByKey(K requestKey);
}
