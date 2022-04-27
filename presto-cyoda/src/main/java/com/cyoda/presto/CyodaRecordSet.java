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

import com.cyoda.presto.client.CyodaApiRequestHandler;
import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.logic.PredicateBuilder;
import com.cyoda.presto.client.logic.PredicateNode;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.facebook.airlift.log.Logger;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.RecordCursor;
import com.facebook.presto.spi.RecordSet;
import com.google.common.collect.ImmutableList;
import org.springframework.hateoas.PagedModel;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static com.cyoda.presto.CyodaErrorCode.CYODA_RESULT_ERROR;
import static java.util.Objects.requireNonNull;

public class CyodaRecordSet<T> implements RecordSet {

    private static final Logger LOG = Logger.get(CyodaRecordSet.class);

    private final CyodaTableHandle tableHandle;
    private final List<CyodaColumnHandle> columnHandles;
    private final List<Type> columnTypes;

    private final Supplier<PagedModel<T>> response;
    private final CyodaClient client;
    private final String requestHandlerKey;

    public CyodaRecordSet(CyodaClient client, CyodaSplit split, List<CyodaColumnHandle> columnHandles) {

        this.client = requireNonNull(client, "client is null");
        this.requestHandlerKey = split.getTableHandle().getRequestHandlerKey();
        this.tableHandle = split.getTableHandle();
        PredicateNode<Any> predicates = PredicateBuilder.setupConstraintPredicates(split.getConstraint());

        this.columnHandles = requireNonNull(columnHandles, "column handles is null");
        ImmutableList.Builder<Type> types = ImmutableList.builder();
        for (CyodaColumnHandle column : columnHandles) {
            types.add(column.getColumnType());
        }
        this.columnTypes = types.build();
        response = () -> requestCollection(split, predicates).orElseThrow(() -> new PrestoException(CYODA_RESULT_ERROR, "No response from Cyoda API"));
    }

    private Optional<PagedModel<T>> requestCollection(CyodaSplit split, PredicateNode<Any> predicates) {
        @SuppressWarnings("unchecked")
        CyodaApiRequestHandler<T> handler = client.getRequestHandlerProvider().getHandler(split.getTableHandle().getRequestHandlerKey());
        LOG.debug("Handler %s is loaded", handler.getHandlerKey());
        return handler.retrievePage(
                0, 0,
                split.getTableHandle().getProjectedColumns().orElse(null), predicates
        );
    }

    @Override
    public List<Type> getColumnTypes() {
        return columnTypes;
    }

    @Override
    public RecordCursor cursor() {
        @SuppressWarnings("unchecked")
        CyodaApiRequestHandler<T> requestHandler = client.getRequestHandlerProvider().getHandler(requestHandlerKey);
        return new CyodaRecordCursor<>(requestHandler, tableHandle, columnHandles, response.get());
    }
}
