package com.cyoda.connector.client.treenode;

import com.cyoda.core.reports.DistributedReportInfoDto;
import com.cyoda.connector.CyodaConfig;
import com.cyoda.connector.client.reporting.stats.ContentIdLoadingCache;
import com.cyoda.connector.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.connector.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.connector.client.treenode.dto.ConfigRequestDto;
import com.cyoda.connector.client.treenode.dto.DataRequestDto;
import com.cyoda.connector.client.treenode.dto.EntityContentDto;
import com.cyoda.connector.client.treenode.dto.ReportRequestDto;
import com.cyoda.connector.client.treenode.dto.schema.SchemaConfigDto;
import com.cyoda.connector.client.treenode.dto.view.TrinoViewDefinitionDto;
import com.cyoda.connector.client.treenode.dto.view.TrinoViewDto;
import com.cyoda.connector.logging.SupplierLogger;
import com.cyoda.service.api.beans.GroupHeader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.trino.spi.connector.ConnectorViewDefinition;
import io.trino.spi.connector.SchemaTableName;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import reactor.core.CorePublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cyoda.connector.client.logic.converters.structure.SingleValueConverter.OBJECT_MAPPER_SUPPLIER;

public class CyodaRSocketClient {

    private static SupplierLogger LOG = SupplierLogger.get(CyodaRSocketClient.class);
    private final RSocketRequester rSocketRequester;
    private final CyodaCacheMonitor cacheMonitor;
    private final CyodaApiRequestStatsMonitor statsMonitor;
    private final CyodaConfig config;


    public final TreeNodeClient treeNodeClient;
    public final ViewClient viewClient;
    public final ReportsClient reportsClient;
    @Inject
    public CyodaRSocketClient(RSocketStrategies strategies,
                              CyodaCacheMonitor cacheMonitor,
                              CyodaConfig cyodaConfig,
                              CyodaApiRequestStatsMonitor statsMonitor, CyodaConfig config) {

        this.statsMonitor = statsMonitor;
        this.config = config;
        this.rSocketRequester = RSocketRequester.builder()
                .rsocketStrategies(strategies)
                .rsocketConnector(connector -> connector
//                        .reconnect(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(2))) // Reconnect on failure
                                .reconnect(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                                        .maxBackoff(Duration.ofSeconds(30))
                                        .doBeforeRetry(retrySignal -> LOG.info("Attempting to reconnect...")))

                                .keepAlive(
                                        Duration.ofSeconds(5), // KeepAlive interval
                                        Duration.ofSeconds(20)
                                )
                )
                .transport(TcpClientTransport.create(cyodaConfig.getRSocketBindAddress(),cyodaConfig.getRSocketPort()));
//                .connect(TcpClientTransport.create("localhost", 7000))
//                .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(2))).block();
        this.cacheMonitor = cacheMonitor;
        treeNodeClient = new TreeNodeClient();
        viewClient = new ViewClient();
        reportsClient = new ReportsClient();
    }

    public abstract class BaseRequester<Q,A,P extends CorePublisher<A>> {
        protected final String route;
        protected final String type;
        public final Function<RSocketRequester.RetrieveSpec, P> dataRetriever;
        public final Function<Q, Map<String, String>> requestMapper;

        private BaseRequester(String route, String type, Function<RSocketRequester.RetrieveSpec, P> dataRetriever, Function<Q, Map<String, String>> requestMapper) {
            this.route = route;
            this.type = type;
            this.dataRetriever = dataRetriever;
            this.requestMapper = requestMapper;
        }

        public P retrieveData(String queryId, Q request){

            RSocketRequester.RetrieveSpec retrieveSpec = rSocketRequester.route(route).data(request);
            return dataRetriever.apply(retrieveSpec);
        }
    }
    public class FluxRequester<Q, A> extends BaseRequester<Q, A, Flux<A>> {

        private FluxRequester(String route, String type, Function<RSocketRequester.RetrieveSpec, Flux<A>> dataRetriever, Function<Q, Map<String, String>> requestMapper) {
            super(route, type, dataRetriever, requestMapper);
        }

        @Override
        public Flux<A> retrieveData(String queryId, Q request) {
            Flux<A> flux = super.retrieveData(queryId, request);
            if (config.getLogApiCallStats()) {
                Date callTime = new Date();
                List<A> response = new ArrayList<>();
                if (config.getLogApiCallResponse()) {
                    flux = flux.doOnNext(response::add);
                }
                flux = flux.doFinally(signalType -> {
                            statsMonitor.registerApiCall(queryId, callTime, route, type, requestMapper.apply(request), response);
                        });
            }
            return flux;
        }
    }
    public class MonoRequester<Q, A> extends BaseRequester<Q, A, Mono<A>> {
        private class ResponseHolder {public A response = null;}
        private MonoRequester(String route, String type, Function<RSocketRequester.RetrieveSpec, Mono<A>> dataRetriever, Function<Q, Map<String, String>> requestMapper) {
            super(route, type, dataRetriever, requestMapper);
        }
        @Override
        public Mono<A> retrieveData(String queryId, Q request) {
            Mono<A> mono = super.retrieveData(queryId, request);
            if (config.getLogApiCallStats()) {
                Date callTime = new Date();
                ResponseHolder holder = new ResponseHolder();
                if (config.getLogApiCallResponse()) {
                    mono = mono.doOnNext(next-> holder.response = next);
                }
                mono = mono.doFinally(signalType -> {
                    statsMonitor.registerApiCall(queryId, callTime, route, type, requestMapper.apply(request), holder.response);
                });
            }
            return mono;
        }
    }

    public class ReportsClient {
        public final FluxRequester<String, Map<String, String>> configsRequester =
                new FluxRequester<>("reports.getReportConfigs",
                        "RS-REPORTS", spec -> spec.retrieveFlux(new ParameterizedTypeReference<Map<String, String>>(){}),
                        request -> Map.of("userId", request));
        public final MonoRequester<String, String> definitionRequester =
                new MonoRequester<>("reports.getReportDefinition",
                        "RS-REPORTS", spec -> spec.retrieveMono(String.class),
                        request -> Map.of("configId", request));
        public final FluxRequester<String,Map<String, Object>> historiesRequester =
                new FluxRequester<>("reports.getReportHistories",
                        "RS-REPORTS", spec -> spec.retrieveFlux(new ParameterizedTypeReference<Map<String, Object>>(){}),
                        request -> Map.of("configId", request));
        public final FluxRequester<String, DistributedReportInfoDto> statsRequester =
                new FluxRequester<>("reports.getReportStatistics",
                        "RS-REPORTS", spec -> spec.retrieveFlux(DistributedReportInfoDto.class),
                        request -> Map.of("configId", request));
        public final FluxRequester<ReportRequestDto, GroupHeader> groupRequester =
                new FluxRequester<>("reports.getReportGroups",
                        "RS-REPORTS", spec -> spec.retrieveFlux(GroupHeader.class),
                        ReportRequestDto::toMap);
        public final FluxRequester<ReportRequestDto, Map<String, Object>> rowsRequester =
                new FluxRequester<>("reports.getReportRows",
                        "RS-REPORTS", spec -> spec.retrieveFlux(new ParameterizedTypeReference<Map<String, Object>>(){}),
                        ReportRequestDto::toMap);
        public final MonoRequester<ConfigRequestDto, String> runRequester =
                new MonoRequester<>("reports.runReport",
                        "RS-REPORTS", spec -> spec.retrieveMono(String.class),
                        ConfigRequestDto::toMap);
        public final MonoRequester<ConfigRequestDto, String> deleteRequester =
                new MonoRequester<>("reports.deleteReports",
                        "RS-REPORTS", spec -> spec.retrieveMono(String.class),
                        ConfigRequestDto::toMap);

    }
    public class ViewClient {

        private final ContentIdLoadingCache<String, Map<SchemaTableName, ConnectorViewDefinition>> cache;
        private final FluxRequester<String, TrinoViewDto> getRequester =
                new FluxRequester<>("view.getViews", "RS-VIEWS",
                        spec -> spec.retrieveFlux(TrinoViewDto.class),
                        request -> Map.of("userId", request));
        public final MonoRequester<TrinoViewDto, String> addRequester =
                new MonoRequester<>("view.addView", "RS-VIEWS",
                        spec -> spec.retrieveMono(String.class),
                        request -> {
                            try {
                                return Map.of("dto", OBJECT_MAPPER_SUPPLIER.get().writerFor(TrinoViewDto.class).writeValueAsString(request));
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        });
        public final MonoRequester<TrinoViewDto, String> renameRequester =
                new MonoRequester<>("view.renameView", "RS-VIEWS",
                        spec -> spec.retrieveMono(String.class),
                        request -> Map.of("userId", request.getUserId(),
                                "oldName", request.getSchemaTableName().toString(),
                                "newName", request.getNewName().toString()));
        public final MonoRequester<TrinoViewDto, String> deleteRequester =
                new MonoRequester<>("view.dropView", "RS-VIEWS",
                        spec -> spec.retrieveMono(String.class),
                        request -> Map.of("userId", request.getUserId(),
                                "name", request.getSchemaTableName().toString()));

        public ViewClient(){
            cache = new ContentIdLoadingCache<String, Map<SchemaTableName, ConnectorViewDefinition>>(Caffeine.newBuilder()
                    .expireAfterWrite(1, TimeUnit.SECONDS)
                    .recordStats()
                    .build(this::getViewsInternal));
            cacheMonitor.register("VIEW_CACHE", cache, Function.identity(), Map::size);
        }
        public Map<SchemaTableName, ConnectorViewDefinition> getViews(String userId) {
            return cache.get(userId);
        }

        private Map<SchemaTableName, ConnectorViewDefinition> getViewsInternal(String userId) {
            List<TrinoViewDto> viewDtos = getRequester.retrieveData("META", userId).collectList().block();
            if (viewDtos == null) return Collections.emptyMap();
            return viewDtos.stream()
                    .collect(Collectors.toMap(TrinoViewDto::getSchemaTableName,
                            dto -> TrinoViewDefinitionDto.toModel(dto.getViewDefinition())));
        }

    }

    public class TreeNodeClient {
        public final FluxRequester<String, SchemaConfigDto> schemaListRequester =
                new FluxRequester<>("treeNode.getSchemas", "RS-TDB",
                        spec -> spec.retrieveFlux(SchemaConfigDto.class),
                        request -> Map.of("userId", request));
        public final MonoRequester<String, SchemaConfigDto> schemaRequester =
                new MonoRequester<>("treeNode.getSchema", "RS-TDB",
                        spec -> spec.retrieveMono(SchemaConfigDto.class),
                        request -> Map.of("schemaName", request));
        public final FluxRequester<DataRequestDto, EntityContentDto> dataRequester =
                new FluxRequester<>("treeNode.getData", "RS-TDB",
                        spec -> spec.retrieveFlux(EntityContentDto.class),
                        DataRequestDto::toMap);

    }
}
