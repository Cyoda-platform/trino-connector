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

package com.cyoda.presto;

import com.cyoda.presto.client.ApiRequestHandler;
import com.cyoda.presto.client.logic.ColumnPredicateBuilder;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.facebook.airlift.log.Logger;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.RecordCursor;
import com.facebook.presto.spi.RecordSet;
import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Not needed see comments in {@link CyodaRecordSetProvider}
 * We have {@link CyodaFilteringPageSource }
 * @param <T>
 */
public class CyodaRecordSet<T> implements RecordSet {

    private static final Logger LOG = Logger.get(CyodaRecordSet.class);

    private final CyodaTableHandle tableHandle;
    private final List<CyodaColumnHandle> columnHandles;
    private final List<Type> columnTypes;

    private final Function<SizeListener,Iterator<T>> response;

    private final CyodaClient client;
    private final String requestHandlerKey;

    public CyodaRecordSet(CyodaClient client, CyodaSplit split, List<CyodaColumnHandle> columnHandles) {

        this.client = requireNonNull(client, "client is null");
        this.requestHandlerKey = split.getTableHandle().getRequestHandlerKey();
        this.tableHandle = split.getTableHandle();
        CompoundPredicateNode predicates = ColumnPredicateBuilder.setupConstraintPredicates(split.getConstraint());

        this.columnHandles = requireNonNull(columnHandles, "column handles is null");
        ImmutableList.Builder<Type> types = ImmutableList.builder();
        for (CyodaColumnHandle column : columnHandles) {
            types.add(column.getColumnType());
        }
        this.columnTypes = types.build();
        response = listener -> requestCollection(split, predicates,listener);
    }

    private Iterator<T> requestCollection(CyodaSplit split, CompoundPredicateNode predicates,SizeListener listener) {
        @SuppressWarnings("unchecked") ApiRequestHandler<T> handler = (ApiRequestHandler<T>) Optional.ofNullable(client.getRequestHandlerProvider().getHandler(requestHandlerKey))
                .orElseThrow(() -> new IllegalArgumentException("Handler " + requestHandlerKey + " not found"));
        LOG.debug("Handler %s is loaded", handler.getHandlerKey());
        return handler.getResponseIterator(tableHandle.getAuthPayload(), 0, split.getTableHandle(), predicates,listener);
    }

    @Override
    public List<Type> getColumnTypes() {
        return columnTypes;
    }

    @Override
    public RecordCursor cursor() {
        @SuppressWarnings("unchecked")
        ApiRequestHandler<T> requestHandler = (ApiRequestHandler<T>) client.getRequestHandlerProvider().getHandler(requestHandlerKey);
        return new CyodaRecordCursor<>(requestHandler, tableHandle, columnHandles, response);
    }
}
