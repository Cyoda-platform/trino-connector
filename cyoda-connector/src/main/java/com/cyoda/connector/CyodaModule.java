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
package com.cyoda.connector;

import com.cyoda.connector.auth.AuthService;
import com.cyoda.connector.client.data.TableDataProviderProvider;
import com.cyoda.connector.client.reporting.calls.DeleteReportsApi;
import com.cyoda.connector.client.reporting.calls.DeleteReportsApiRSocket;
import com.cyoda.connector.client.reporting.calls.RunReportApi;
import com.cyoda.connector.client.reporting.calls.RunReportApiRSocket;
import com.cyoda.connector.client.reporting.data.ReportRowsApi;
import com.cyoda.connector.client.reporting.data.ReportRowsApiRSocket;
import com.cyoda.connector.client.reporting.groups.ReportGroupsApi;
import com.cyoda.connector.client.reporting.groups.ReportGroupsApiRSocket;
import com.cyoda.connector.client.reporting.meta.ConfiguredReportsApi;
import com.cyoda.connector.client.reporting.meta.ConfiguredReportsApiRSocket;
import com.cyoda.connector.client.reporting.meta.ReportConfigDetailsApi;
import com.cyoda.connector.client.reporting.meta.ReportConfigDetailsApiRSocket;
import com.cyoda.connector.client.reporting.meta.ReportHistoryApi;
import com.cyoda.connector.client.reporting.meta.ReportHistoryApiRSocket;
import com.cyoda.connector.client.reporting.meta.ReportStatisticsApi;
import com.cyoda.connector.client.reporting.meta.ReportStatisticsApiRSocket;
import com.cyoda.connector.client.reporting.metaproviders.DynamicReportMetadataProvider;
import com.cyoda.connector.client.reporting.metaproviders.StaticTableMetadataProvider;
import com.cyoda.connector.client.reporting.metaproviders.TreeNodeMetadataProvider;
import com.cyoda.connector.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.connector.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.connector.client.treenode.CyodaRSocketClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.trino.spi.NodeManager;
import io.trino.spi.connector.ConnectorContext;
import io.trino.spi.type.Type;
import io.trino.spi.type.TypeManager;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketStrategies;

import jakarta.inject.Inject;

import java.util.Collections;

import static io.airlift.configuration.ConfigBinder.configBinder;
import static io.airlift.json.JsonBinder.jsonBinder;
import static io.trino.sql.analyzer.TypeSignatureTranslator.parseTypeSignature;
import static java.util.Objects.requireNonNull;

public class CyodaModule implements Module {
    private final String connectorId;
    private final TypeManager typeManager;
    private final NodeManager nodeManager;
    private final OpenTelemetry telemetry;
    private final Tracer tracer;


    public CyodaModule(String connectorId, ConnectorContext context) {
        this.connectorId = requireNonNull(connectorId, "connector id is null");
        typeManager = requireNonNull(context.getTypeManager(), "typeManager is null");
        nodeManager = requireNonNull(context.getNodeManager(), "nodeManager is null");
        telemetry = requireNonNull(context.getOpenTelemetry(), "openTelemetry is null");
        tracer = requireNonNull(context.getTracer(), "tracer is null");
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(TypeManager.class).toInstance(typeManager);
        binder.bind(NodeManager.class).toInstance(nodeManager);
        binder.bind(OpenTelemetry.class).toInstance(telemetry);
        binder.bind(Tracer.class).toInstance(tracer);

        binder.bind(CyodaConnectorId.class).toInstance(new CyodaConnectorId(connectorId));
        binder.bind(CyodaMetadata.class).in(Scopes.SINGLETON);
        binder.bind(CyodaSplitManager.class).in(Scopes.SINGLETON);
        binder.bind(CyodaPageSourceProvider.class).in(Scopes.SINGLETON);
        binder.bind(CyodaProcedureManager.class).in(Scopes.SINGLETON);
        binder.bind(CyodaNodePartitioningProvider.class).in(Scopes.SINGLETON);
        binder.bind(CyodaConnector.class).in(Scopes.SINGLETON);

        binder.bind(StaticTableMetadataProvider.class).in(Scopes.SINGLETON);
        binder.bind(DynamicReportMetadataProvider.class).in(Scopes.SINGLETON);
        binder.bind(TreeNodeMetadataProvider.class).in(Scopes.SINGLETON);

        binder.bind(ConfiguredReportsApi.class).to(ConfiguredReportsApiRSocket.class).in(Scopes.SINGLETON);
        binder.bind(ReportConfigDetailsApi.class).to(ReportConfigDetailsApiRSocket.class).in(Scopes.SINGLETON);
        binder.bind(ReportStatisticsApi.class).to(ReportStatisticsApiRSocket.class).in(Scopes.SINGLETON);
        binder.bind(ReportHistoryApi.class).to(ReportHistoryApiRSocket.class).in(Scopes.SINGLETON);
        binder.bind(ReportGroupsApi.class).to(ReportGroupsApiRSocket.class).in(Scopes.SINGLETON);
        binder.bind(ReportRowsApi.class).to(ReportRowsApiRSocket.class).in(Scopes.SINGLETON);
        binder.bind(RunReportApi.class).to(RunReportApiRSocket.class).in(Scopes.SINGLETON);
        binder.bind(DeleteReportsApi.class).to(DeleteReportsApiRSocket.class).in(Scopes.SINGLETON);

        binder.bind(CyodaRSocketClient.class).in(Scopes.SINGLETON);


        binder.bind(CyodaApiRequestStatsMonitor.class).in(Scopes.SINGLETON);
        binder.bind(CyodaCacheMonitor.class).in(Scopes.SINGLETON);

        binder.bind(TableDataProviderProvider.class).in(Scopes.SINGLETON);

        binder.bind(AuthService.class).in(Scopes.SINGLETON);

        configBinder(binder).bindConfig(CyodaConfig.class);

        jsonBinder(binder).addDeserializerBinding(Type.class).to(TypeDeserializer.class);

    }

    @Provides
    @Singleton
    RSocketStrategies rSocketStrategies() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
        return RSocketStrategies.builder()
                .encoder(new Jackson2JsonEncoder(objectMapper))
                .decoder(new Jackson2JsonDecoder(objectMapper))
                .build();
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
            //TODO check if type params are not lost
            return typeManager.getType(parseTypeSignature(value, Collections.EMPTY_SET));
        }
    }
}
