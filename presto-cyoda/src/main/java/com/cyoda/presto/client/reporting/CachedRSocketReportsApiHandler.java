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
import com.cyoda.presto.client.reporting.stats.ContentIdLoadingCache;
import com.cyoda.presto.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.presto.client.treenode.CyodaRSocketClient;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.List;

public abstract class CachedRSocketReportsApiHandler<K, T> extends BaseRSocketReportsApiHandler implements CachedReportsApi<K, T> {

    protected final CyodaConfig config;
    protected final ContentIdLoadingCache<K, List<T>> cache;

    public abstract List<T> loadByKey(K requestKey);

    protected abstract LoadingCache<K, List<T>> setupCache(CacheLoader<K, List<T>> loader);
    protected abstract void registerCache(CyodaCacheMonitor cacheMonitor, ContentIdLoadingCache<K, List<T>> cache);

    protected CachedRSocketReportsApiHandler(CyodaRSocketClient rSocketClient,
                                             CyodaCacheMonitor cacheMonitor, CyodaConfig config) {
        super(rSocketClient);
        this.config = config;
        cache = new ContentIdLoadingCache<>(setupCache(this::loadByKey));
        registerCache(cacheMonitor, cache);
    }

    @Override
    public List<T> getByKey(K requestKey) {
        return cache.get(requestKey);
    }
}
