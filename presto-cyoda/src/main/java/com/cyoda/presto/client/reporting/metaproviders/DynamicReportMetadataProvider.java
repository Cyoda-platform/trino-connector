package com.cyoda.presto.client.reporting.metaproviders;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.reporting.BaseReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import io.trino.spi.type.TypeManager;
import reactor.core.publisher.Flux;

import javax.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.cyoda.presto.SizeListener.NOT_LISTENING;

public class DynamicReportMetadataProvider extends TableMetadataProvider {
    private static final SupplierLogger LOG = SupplierLogger.get(DynamicReportMetadataProvider.class);


    private final ReportConfigDetailsApiHandler reportConfigDetailsHandler;
    private final StaticReportMetadataProvider staticReportMetadataProvider;

    /**
     * TableName -> TableMetadata
     */
    private final LoadingCache<AuthContext,Map<String, CyodaTableHandle>> tableCache;

    @Inject
    public DynamicReportMetadataProvider(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                         StaticReportMetadataProvider staticReportMetadataProvider,
                                         ReportConfigDetailsApiHandler reportConfigDetailsApiHandler) {
            super(typeManager, config, connectorId);
        this.staticReportMetadataProvider = staticReportMetadataProvider;
        this.reportConfigDetailsHandler = reportConfigDetailsApiHandler;
        tableCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(2))
                .build(key -> {
                    LOG.debug("Reloading Tables Cache");
                    return getAllDynamicTables(key);
                });
    }

    protected Map<String, CyodaTableHandle> getAllDynamicTables(AuthContext authContext) {
        Map<String, CyodaTableHandle> result = new HashMap<>();
        Flux<ReportDefinitionHandle> flux = reportConfigDetailsHandler.asFlux(
                authContext,
                this.staticReportMetadataProvider.getReportDetails().getTableHandle(),
                CompoundPredicateNode.empty(null),
                NOT_LISTENING
        );;
        flux.doOnNext(item -> {
            String reportName = item.getReportName();
            String reportConfigId = item.getReportConfigId();
            String tableName = BaseReportsApiHandler.reportNameToTableName(reportConfigId);

            List<CyodaColumnHandle> columns = new java.util.ArrayList<>(List.copyOf(staticReportMetadataProvider.getReportRows().getTableHandle().getProjectedColumns()));
            columns.addAll(item.getColumns());
            CyodaTableHandle tableHandle = new CyodaTableHandle(connectorId.toString(), config.getSchemaName(), tableName,
                    columns, StaticReportTable.REPORT_ROWS.name(), reportConfigId, item.getDescription(),
                    getUri(StaticReportTable.REPORT_ROWS));


            result.put(tableName, tableHandle);
        }).blockLast();
        // If there are duplicates, last write wins.
        return ImmutableMap.copyOf(result);
    }

    public CyodaTableHandle getTableHandle(AuthContext authContext, String tableName) {
        Map<String, CyodaTableHandle> map = tableCache.get(authContext);
        return Optional.ofNullable(map.get(tableName)).orElseThrow(
                () -> new NoSuchElementException(String.format(
                        "Metadata provider %s does not contain table with name %s",
                        this.getClass().getSimpleName(), tableName))
        );
    }

    public List<String> getTableList(AuthContext authContext) {
        Map<String, CyodaTableHandle> map = tableCache.get(authContext);
        return map.keySet().stream().toList();
    }


}
