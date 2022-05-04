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

package com.cyoda.presto.client;

import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.SchemaTableName;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.facebook.presto.spi.StandardErrorCode.GENERIC_INTERNAL_ERROR;
import static java.util.Objects.requireNonNull;

public class CyodaApiRequestHandlerProvider {

    private final Map<String, ApiRequestHandler<?>> handlers;

    @SuppressWarnings({"unchecked", "squid:S3740", "rawtypes"})
    @Inject
    public CyodaApiRequestHandlerProvider(Set<ApiRequestHandler> handlerList) {
        handlers = handlerList.stream().collect(Collectors.toMap(ApiRequestHandler::getHandlerKey, x -> x));
    }

    @SuppressWarnings({"java:S1452"})
    public ApiRequestHandler<?> getHandler(String type) {
        requireNonNull(type, "type is null");
        return handlers.get(type);
    }

    @SuppressWarnings({"squid:S1452"})
    public ApiRequestHandler<?> getHandler(SchemaTableName tableName) {
        return handlers.values().stream().filter(h -> h.hasTable(tableName)).findAny().orElseThrow(
                () -> new PrestoException(GENERIC_INTERNAL_ERROR, "[Cyoda]:" + this.getClass().getSimpleName() +
                        ":unexpected error trying to get the Handler for table " + tableName)
        );
    }

    @SuppressWarnings({"squid:S1452"})
    public Collection<ApiRequestHandler<?>> getHandlers() {
        return handlers.values();
    }
}
