package com.cyoda.presto.client.treenode;

import com.cyoda.presto.client.reporting.stats.ContentIdLoadingCache;
import com.cyoda.presto.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.presto.client.treenode.dto.DataRequestDto;
import com.cyoda.presto.client.treenode.dto.EntityContentDto;
import com.cyoda.presto.client.treenode.dto.view.TrinoViewDefinitionDto;
import com.cyoda.presto.client.treenode.dto.view.TrinoViewDto;
import com.cyoda.presto.client.treenode.dto.schema.SchemaConfigDto;
import com.cyoda.presto.client.treenode.dto.schema.TableConfigDto;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import io.rsocket.core.Resume;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.trino.spi.connector.ConnectorViewDefinition;
import io.trino.spi.connector.SchemaTableName;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import reactor.util.retry.Retry;


import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CyodaRSocketClient {

    private final RSocketRequester rSocketRequester;

    private final TreeNodeClient treeNodeClient;
    private final ViewClient viewClient;
    private final CyodaCacheMonitor cacheMonitor;
    @Inject
    public CyodaRSocketClient(RSocketStrategies strategies,
                              CyodaCacheMonitor cacheMonitor) {


        this.rSocketRequester = RSocketRequester.builder()
                .rsocketStrategies(strategies)
                .rsocketConnector(connector -> connector
//                        .reconnect(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(2))) // Reconnect on failure
                        .reconnect(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                                .maxBackoff(Duration.ofSeconds(30)) // Adjust based on server start time
//                                .doBeforeRetry(retrySignal -> log.info("Attempting to reconnect...")))
                        )
                        .keepAlive(
                                Duration.ofSeconds(5), // KeepAlive interval
                                Duration.ofSeconds(20)
                        )
                )
                .transport(TcpClientTransport.create("localhost", 7000));
//                .connect(TcpClientTransport.create("localhost", 7000))
//                .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(2))).block();
        this.cacheMonitor = cacheMonitor;
        treeNodeClient = new TreeNodeClient();
        viewClient = new ViewClient();
    }

    public TreeNodeClient treeNode() {
        return treeNodeClient;
    }

    public ViewClient view() {
        return viewClient;
    }

    public class ViewClient {

        ContentIdLoadingCache<String, Map<SchemaTableName, ConnectorViewDefinition>> cache;
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
            List<TrinoViewDto> viewDtos = rSocketRequester.route("view.getViews")
                    .data(userId)
                    .retrieveFlux(TrinoViewDto.class).collectList().block();
            if (viewDtos == null) return Collections.emptyMap();
            return viewDtos.stream()
                    .collect(Collectors.toMap(TrinoViewDto::getSchemaTableName,
                            dto -> TrinoViewDefinitionDto.toModel(dto.getViewDefinition())));
        }

        public String addView(String userId, SchemaTableName viewName, ConnectorViewDefinition definition, boolean replace) {
            return rSocketRequester.route("view.addView")
                    .data(new TrinoViewDto(userId, viewName, TrinoViewDefinitionDto.fromModel(definition), null, replace))
                    .retrieveMono(String.class).block();
        }

        public String renameView(SchemaTableName viewName, SchemaTableName newViewName, String userId) {
            return rSocketRequester.route("view.renameView")
                    .data(new TrinoViewDto(userId, viewName, null, newViewName, false))
                    .retrieveMono(String.class).block();
        }

        public String dropView(SchemaTableName viewName, String userId) {
            return rSocketRequester.route("view.dropView")
                    .data(new TrinoViewDto(userId, viewName, null, null, true))
                    .retrieveMono(String.class).block();
        }
    }

    public class TreeNodeClient {
        public List<SchemaConfigDto> getSchemas(String userId) {
            return rSocketRequester.route("treeNode.getSchemas")
                    .data(userId)
                    .retrieveFlux(SchemaConfigDto.class).collectList().block();
        }

        public SchemaConfigDto getSchema(String schemaName) {
            return rSocketRequester.route("treeNode.getSchema")
                    .data(schemaName)
                    .retrieveMono(SchemaConfigDto.class).block();
        }

        public Iterable<EntityContentDto> getData(DataRequestDto dataRequest) {
            return rSocketRequester.route("treeNode.getData")
                    .data(dataRequest)
                    .retrieveFlux(EntityContentDto.class).toIterable();
        }
    }
}
