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
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.PagingApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.paging.PagedIterable;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.type.TypeManager;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class BasePagingReportsApiHandler<T> extends BaseReportsApiHandler<T> implements PagingApiRequestHandler<T> {

    protected BasePagingReportsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager, String endpoint, RestTemplateCustomizer restTemplateCustomizer, SupplierLogger log) {
        super(connectorId, config, typeManager, endpoint, restTemplateCustomizer, log);
    }

    @Override
    public Iterator<T> getResponseIterator(
            AuthContext authContext,
            int pageSize,
            CyodaTableHandle tableHandle,
            CompoundPredicateNode predicates,
            SizeListener listener
    ) {
        logCreation(pageSize, tableHandle, predicates, log);
        List<CyodaColumnHandle> projectedColumns = tableHandle.getProjectedColumns().orElse(Collections.emptyList());
        return new PagedIterable<>(authContext, this, pageSize, projectedColumns, predicates,listener).iterator();
    }


    public Flux<T> asFlux(AuthContext authContext, int pageSize, CyodaTableHandle tableHandle, CompoundPredicateNode predicates, SizeListener listener) {
        logCreation(pageSize, tableHandle, predicates, log);
        List<CyodaColumnHandle> projectedColumns = tableHandle.getProjectedColumns().orElse(Collections.emptyList());
        return Flux.fromIterable(new PagedIterable<>(authContext,this, pageSize, projectedColumns, predicates,listener));
    }
}
