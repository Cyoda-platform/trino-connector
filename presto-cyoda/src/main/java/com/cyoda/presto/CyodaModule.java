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
import com.cyoda.presto.client.CyodaApiRequestHandlerProvider;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.reporting.data.ReportRowsApiHandler;
import com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler;
import com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportStatisticsApiHandler;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.TypeManager;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;

import javax.inject.Inject;

import static com.facebook.airlift.configuration.ConfigBinder.configBinder;
import static com.facebook.airlift.json.JsonBinder.jsonBinder;
import static com.facebook.airlift.json.JsonCodec.listJsonCodec;
import static com.facebook.airlift.json.JsonCodecBinder.jsonCodecBinder;
import static com.facebook.presto.common.type.TypeSignature.parseTypeSignature;
import static java.util.Objects.requireNonNull;

public class CyodaModule implements Module {
    private final String connectorId;
    private final TypeManager typeManager;


    public CyodaModule(String connectorId, TypeManager typeManager) {
        this.connectorId = requireNonNull(connectorId, "connector id is null");
        this.typeManager = requireNonNull(typeManager, "typeManager is null");
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(TypeManager.class).toInstance(typeManager);

        binder.bind(CyodaConnector.class).in(Scopes.SINGLETON);
        binder.bind(CyodaConnectorId.class).toInstance(new CyodaConnectorId(connectorId));
        binder.bind(CyodaMetadata.class).in(Scopes.SINGLETON);
        binder.bind(CyodaClient.class).in(Scopes.SINGLETON);
        binder.bind(CyodaSplitManager.class).in(Scopes.SINGLETON);
        binder.bind(CyodaPageSourceProvider.class).in(Scopes.SINGLETON);

        @SuppressWarnings({"squid:S3740", "rawtypes"})
        Multibinder<ApiRequestHandler> shapeBinder =
                Multibinder.newSetBinder(binder, ApiRequestHandler.class);
        shapeBinder.addBinding().to(ConfiguredReportsApiHandler.class);
        shapeBinder.addBinding().to(ReportHistoryApiHandler.class);
        shapeBinder.addBinding().to(ReportConfigDetailsApiHandler.class);
        shapeBinder.addBinding().to(ReportStatisticsApiHandler.class);
        shapeBinder.addBinding().to(ReportGroupsApiHandler.class);
        shapeBinder.addBinding().to(ReportRowsApiHandler.class);

        binder.bind(CyodaApiRequestHandlerProvider.class).in(Scopes.SINGLETON);

        configBinder(binder).bindConfig(CyodaConfig.class);

        jsonBinder(binder).addDeserializerBinding(Type.class).to(TypeDeserializer.class);

        binder.bind(RestTemplateCustomizer.class).in(Scopes.SINGLETON);

        jsonCodecBinder(binder).bindMapJsonCodec(String.class, listJsonCodec(CyodaTable.class));
    }

    public static final class TypeDeserializer
            extends FromStringDeserializer<Type> {
        private final TypeManager typeManager;

        @Inject
        public TypeDeserializer(TypeManager typeManager) {
            super(Type.class);
            this.typeManager = requireNonNull(typeManager, "typeManager is null");
        }

        @Override
        protected Type _deserialize(String value, DeserializationContext context) {
            return typeManager.getType(parseTypeSignature(value));
        }
    }
}
