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

import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.reports.CyodaApiRequestHandler;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.RecordCursor;
import com.facebook.presto.spi.RecordSet;
import com.google.common.collect.ImmutableList;
import org.springframework.hateoas.CollectionModel;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class CyodaRecordSet<T> implements RecordSet {
    private final List<CyodaColumnHandle> columnHandles;
    private final List<Type> columnTypes;

    private final Supplier<CollectionModel<T>> response;
    private final CyodaClient client;
    private final String requestHandlerKey;

    public CyodaRecordSet(CyodaClient client, CyodaSplit split, List<CyodaColumnHandle> columnHandles) {

        this.client = requireNonNull(client, "client is null");
        requireNonNull(split, "split is null");
        this.requestHandlerKey = split.getRequestHandlerKey();

        this.columnHandles = requireNonNull(columnHandles, "column handles is null");
        ImmutableList.Builder<Type> types = ImmutableList.builder();
        for (CyodaColumnHandle column : columnHandles) {
            types.add(column.getColumnType());
        }
        this.columnTypes = types.build();
        response = () -> requestCollection(split);
    }

    @SuppressWarnings("java:S1854") // We want local variables to make debugging a bit easier.
    private CollectionModel<T> requestCollection(CyodaSplit split) {
        @SuppressWarnings("unchecked")
        CyodaApiRequestHandler<T> handler = client.getRequestHandlerProvider().getHandler(split.getRequestHandlerKey());
        return handler.retrieveCollection();
    }

    @Override
    public List<Type> getColumnTypes() {
        return columnTypes;
    }

    @Override
    public RecordCursor cursor() {
        @SuppressWarnings("unchecked")
        CyodaApiRequestHandler<T> requestHandler = client.getRequestHandlerProvider().getHandler(requestHandlerKey);
        return new CyodaRecordCursor<>(requestHandler, columnHandles, response.get());
    }
}
