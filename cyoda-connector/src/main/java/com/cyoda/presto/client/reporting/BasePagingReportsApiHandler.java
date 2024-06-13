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
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.paging.PagingFluxProvider;
import com.cyoda.presto.client.paging.PagingHandle;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.logging.SupplierLogger;
import org.springframework.hateoas.PagedModel;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.function.Function;

public abstract class BasePagingReportsApiHandler<K, T> extends BaseReportsApiHandler implements FluxApiHandler<K, T> {

    protected BasePagingReportsApiHandler(CyodaConfig config,
                                          RestTemplateCustomizer restTemplateCustomizer,
                                          SupplierLogger log,
                                          AuthService authService,
                                          CyodaApiRequestStatsMonitor requestStatsMonitor) {
        super(config, restTemplateCustomizer, log, authService, requestStatsMonitor);
    }

    protected abstract Optional<PagedModel<T>> retrievePage(
            K requestKey,
            int page,
            int pageSize,
            SizeListener listener);

    @Override
    public Flux<T> asFlux(K requestKey, SizeListener listener) {

        int pageSize = config.getRequestPageSize();
        logCreation(pageSize, log);
        Function<Integer, PagingHandle<?, T>> pagingHandleGetter = page ->
                new PagingHandle<>(retrievePage(requestKey, page, pageSize, listener));
        return new PagingFluxProvider<>(pagingHandleGetter).generate(0);
    }

    ;
}
