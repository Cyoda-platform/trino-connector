/*
 * Copyright (C) 2022 Cyoda Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.cyoda.presto.client.reporting;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.paging.PagingFluxProvider;
import com.cyoda.presto.client.paging.PagingHandle;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.presto.logging.SupplierLogger;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.trino.spi.type.TypeManager;
import org.springframework.hateoas.PagedModel;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class CachedPagingReportsApiHandler<K, T> extends BaseReportsApiHandler {

    protected final LoadingCache<K, List<T>> cache;

    public abstract Optional<PagedModel<T>> retrievePage(
            K requestKey,
            int page,
            int pageSize,
            SizeListener listener);

    protected abstract LoadingCache<K, List<T>> setupCache(CacheLoader<K, List<T>> loader);
    protected abstract void registerCache(CyodaCacheMonitor cacheMonitor, LoadingCache<K, List<T>> cache);

    protected CachedPagingReportsApiHandler(CyodaConnectorId connectorId,
                                            CyodaConfig config,
                                            TypeManager typeManager,
                                            RestTemplateCustomizer restTemplateCustomizer,
                                            SupplierLogger log,
                                            AuthService authService,
                                            CyodaApiRequestStatsMonitor requestStatsMonitor,
                                            CyodaCacheMonitor cacheMonitor) {
        super(connectorId, config, typeManager, restTemplateCustomizer, log, authService, requestStatsMonitor);
        cache = setupCache(this::loadByKey);
        registerCache(cacheMonitor, cache);
    }

    protected List<T> loadByKey(K requestKey){

        int pageSize = config.getRequestPageSize();
        logCreation(pageSize, log);
        Function<Integer, PagingHandle<?, T>> pagingHandleGetter = page ->
                new PagingHandle<>(retrievePage(requestKey, page, pageSize, SizeListener.NOT_LISTENING));
        return new PagingFluxProvider<>(pagingHandleGetter).generate(0).subscribeOn(Schedulers.immediate(), false).collectList().block();
    }

    public List<T> getByKey(K requestKey) {
        return cache.get(requestKey);
    }
}
