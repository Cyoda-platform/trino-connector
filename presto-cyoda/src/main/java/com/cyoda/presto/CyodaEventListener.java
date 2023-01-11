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

import com.cyoda.presto.logging.LoggableQueryFailureInfo;
import com.cyoda.presto.logging.LoggableQueryStatistics;
import com.cyoda.presto.logging.LoggableSplitStatistics;
import com.cyoda.presto.logging.SupplierLogger;
import io.airlift.json.ObjectMapperProvider;
import io.airlift.log.Logger;
import io.trino.spi.eventlistener.EventListener;
import io.trino.spi.eventlistener.QueryCompletedEvent;
import io.trino.spi.eventlistener.QueryContext;
import io.trino.spi.eventlistener.QueryCreatedEvent;
import io.trino.spi.eventlistener.QueryMetadata;
import io.trino.spi.eventlistener.SplitCompletedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Suppliers;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

public class CyodaEventListener implements EventListener {

    private final SupplierLogger log;

    @SuppressWarnings("FunctionalExpressionCanBeFolded")
    public static final Supplier<ObjectMapper> OBJECT_MAPPER_SUPPLIER = Suppliers.memoize(
            () -> new ObjectMapperProvider().get().enable(INDENT_OUTPUT))::get;

    static final String QUERY_CONTEXT_LOGGING_FLAG = "log-context";
    static final String QUERY_META_LOGGING_FLAG = "log-meta";
    static final String QUERY_SPLIT_LOGGING_FLAG = "log-split";
    static final String QUERY_STATS_LOGGING_FLAG = "log-stats";


    private final Map<String, String> config;

    public CyodaEventListener(Map<String, String> config, SupplierLogger log) {
        this.config = config;
        this.log = log;
    }

    @Override
    public void queryCreated(QueryCreatedEvent queryCreatedEvent) {
        try {
            getQueryMetaMessage(queryCreatedEvent.getMetadata()).ifPresent(msg ->
                    log.debug("QueryMetadata: \n%s",msg)
            );
        } catch (JsonProcessingException e) {
            log.error(e,"Cannot log QueryMetadata");
        }
        try {
            getQueryContextMessage(queryCreatedEvent.getContext()).ifPresent(msg -> log.debug("QueryContext: \n%s",msg));
        } catch (JsonProcessingException e) {
            log.error(e,"Cannot log QueryContext");
        }
    }

    @Override
    public void queryCompleted(QueryCompletedEvent queryCompletedEvent) {
        log.debug("Completed Query %s",queryCompletedEvent.getMetadata().getQueryId());

        try {
            getFailureInfoMessage(queryCompletedEvent).ifPresent(msg -> log.warn("Query Fails: \n%s",msg));
        } catch (JsonProcessingException e) {
            log.error(e,"Cannot log QueryFailureInfo");
        }

        try {
            getQueryStatisticsMessage(queryCompletedEvent).ifPresent(msg -> log.debug("Query Stats: \n%s", msg));
        } catch (JsonProcessingException e) {
            log.error(e,"Cannot log QueryStatistics");
        }
    }

    @Override
    public void splitCompleted(SplitCompletedEvent splitCompletedEvent) {
        log.debug("Completed Query Split for %s", splitCompletedEvent.getQueryId());
        try {
            getSplitStatisticsMessage(splitCompletedEvent).ifPresent(msg -> log.debug("Split Statistics: \n%s", msg));
        } catch (JsonProcessingException e) {
            log.error(e,"Cannot log SplitCompletedEvent");
        }
    }

    private Optional<String> getFailureInfoMessage(QueryCompletedEvent queryCompletedEvent) throws JsonProcessingException {
        if (queryCompletedEvent.getFailureInfo().isPresent()) {
            return Optional.of(OBJECT_MAPPER_SUPPLIER.get()
                    .writerFor(LoggableQueryFailureInfo.class)
                    .writeValueAsString(
                            LoggableQueryFailureInfo.from(
                                    queryCompletedEvent.getFailureInfo().get()
                            )
                    ));
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> getQueryStatisticsMessage(QueryCompletedEvent queryCompletedEvent) throws JsonProcessingException {
        if (log.isDebugEnabled() && config.containsKey(QUERY_STATS_LOGGING_FLAG) && config.get(QUERY_STATS_LOGGING_FLAG).startsWith("1")) {
            return Optional.of(OBJECT_MAPPER_SUPPLIER.get()
                    .writerFor(LoggableQueryStatistics.class)
                    .writeValueAsString(LoggableQueryStatistics.from(queryCompletedEvent.getStatistics()))
            );
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> getSplitStatisticsMessage(SplitCompletedEvent splitCompletedEvent) throws JsonProcessingException {
        if (log.isDebugEnabled() && config.containsKey(QUERY_SPLIT_LOGGING_FLAG) && config.get(QUERY_SPLIT_LOGGING_FLAG).startsWith("1")) {
            return Optional.of(OBJECT_MAPPER_SUPPLIER.get()
                    .writerFor(LoggableSplitStatistics.class)
                    .writeValueAsString(LoggableSplitStatistics.from(splitCompletedEvent.getStatistics()))
            );
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> getQueryMetaMessage(QueryMetadata metadata) throws JsonProcessingException {
        if (log.isDebugEnabled() && config.containsKey(QUERY_META_LOGGING_FLAG) && config.get(QUERY_META_LOGGING_FLAG).startsWith("1")) {
            return Optional.of(
                    OBJECT_MAPPER_SUPPLIER.get().writerFor(QueryMetadata.class).writeValueAsString(metadata)
            );
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> getQueryContextMessage(QueryContext context) throws JsonProcessingException {
        if (log.isDebugEnabled() && config.containsKey(QUERY_CONTEXT_LOGGING_FLAG) && config.get(QUERY_CONTEXT_LOGGING_FLAG).startsWith("1")) {
            return Optional.of(OBJECT_MAPPER_SUPPLIER.get().writerFor(QueryContext.class).writeValueAsString(context));
        } else {
            return Optional.empty();
        }
    }
}
