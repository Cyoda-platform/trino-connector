package com.cyoda.presto.client.reporting.metaproviders;

import com.cyoda.api.view.GridConfigFieldsView;
import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.reporting.BaseReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigKey;
import com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle;
import com.cyoda.presto.client.reporting.meta.ReportListKey;
import com.cyoda.presto.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.DummyTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import io.trino.spi.type.TypeManager;
import reactor.core.publisher.Flux;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.cyoda.presto.SizeListener.NOT_LISTENING;

public class DynamicReportMetadataProvider extends TableMetadataProvider {
    private static final SupplierLogger LOG = SupplierLogger.get(DynamicReportMetadataProvider.class);


    private final AuthService auth;
    private final ConfiguredReportsApiHandler configuredReportsApiHandler;
    private ReportConfigDetailsApiHandler reportConfigDetailsApiHandler;
    private final StaticTableMetadataProvider staticTableMetadataProvider;

    /**
     * TableName -> TableMetadata
     */
    private final LoadingCache<AuthContext, Map<String, CyodaTableHandle>> tableByUserCache;
    private final LoadingCache<TableMetaCacheKey, CyodaTableHandle> tableMetaCache;

    @Inject
    public DynamicReportMetadataProvider(CyodaConnectorId connectorId, CyodaConfig config,
                                         TypeManager typeManager, AuthService auth,
                                         StaticTableMetadataProvider staticTableMetadataProvider,
                                         ConfiguredReportsApiHandler configuredReportsApiHandler,
                                         ReportConfigDetailsApiHandler reportConfigDetailsApiHandler,
                                         CyodaCacheMonitor cacheMonitor) {
        super(typeManager, config, connectorId);
        this.auth = auth;
        this.staticTableMetadataProvider = staticTableMetadataProvider;
        this.configuredReportsApiHandler = configuredReportsApiHandler;
        this.reportConfigDetailsApiHandler = reportConfigDetailsApiHandler;
        tableByUserCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(2))
                .build(key -> {
                    LOG.debug("Loading Tables Cache for user " + key.getUserId());
                    return tableByUserCacheLoad(key);
                });
        cacheMonitor.register("AUTH", tableByUserCache, AuthContext::getUserId, Map::size);
        tableMetaCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofDays(1))
                .build(key -> {
                    LOG.debug("Loading config " + key);
                    return getTableHandleFromCyoda(key.configId);
                });
        cacheMonitor.register("META", tableMetaCache, key -> key.configId, x->1);
    }

    private CyodaTableHandle getTableHandleFromCyoda(String configId) {
        String tableName = BaseReportsApiHandler.reportNameToTableName(configId);
        ReportDefinitionHandle definitionHandle = null;
        try {
            definitionHandle = reportConfigDetailsApiHandler.getReportDefSingleHandle(new ReportConfigKey(configId, "META"));

            List<CyodaColumnHandle> columns = new ArrayList<>(List.copyOf(staticTableMetadataProvider.getReportRows().getTableHandle().getProjectedColumns()));
            columns.addAll(definitionHandle.getColumns());
            columns.sort(Comparator.comparingInt(CyodaColumnHandle::getOrdinalPosition));
            return new CyodaTableHandle(connectorId.toString(), config.getSchemaName(), tableName,
                    columns, CyodaTableHandle.TableType.DATA, configId, definitionHandle.getDescription(),
                    getUri(StaticReportTable.REPORT_ROWS),
                    !definitionHandle.getGroupingColumns().isEmpty(),
                    !definitionHandle.isSingleton());
        } catch (Exception e) {
            Map<String, String> errorDetail = new HashMap<>();
            errorDetail.put("Error Message", e.getMessage());
            errorDetail.put("Stack Trace", getStackTrace(e));
            if (definitionHandle != null)
                errorDetail.put("Definition Handle", definitionHandle.toString());
            return new DummyTableHandle(connectorId.toString(), config.getSchemaName(),
                    "!fail-" + tableName, errorDetail);
        }
    }

    private static String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
            sb.append("\n");
            sb.append(stackTraceElement);
        }
        return sb.toString();
    }

    protected Map<String, CyodaTableHandle> tableByUserCacheLoad(AuthContext authContext) {
        Map<String, CyodaTableHandle> result = new HashMap<>();
        Flux<GridConfigFieldsView> flux = configuredReportsApiHandler.asFlux(
                new ReportListKey(authContext, "META"),
                NOT_LISTENING
        );
        flux.doOnNext(item -> {
            TableMetaCacheKey cacheKey = TableMetaCacheKey.of(item);
            CyodaTableHandle tableHandle = tableMetaCache.get(cacheKey);
            result.put(tableHandle.getTableName(), tableHandle);
            if (tableHandle.hasHistory()) {
                addHistoryTable(result, tableHandle);
            }
            if (tableHandle.hasGroups()) {
                addGroupsTable(result, tableHandle);
            }
        }).blockLast();
        // If there are duplicates, last write wins.
        return ImmutableMap.copyOf(result);
    }

    private void addHistoryTable(Map<String, CyodaTableHandle> result, CyodaTableHandle tableHandle) {
        String supName = tableHandle.getTableName() + "_history";
        result.put(supName, staticTableMetadataProvider
                .getHistoryTableTemplate().createTableHandle(
                        supName,
                        tableHandle.getReportConfigId(),
                        tableHandle.getDescription()
                ));
    }
    private void addGroupsTable(Map<String, CyodaTableHandle> result, CyodaTableHandle tableHandle) {
        String supName = tableHandle.getTableName() + "_groups";
        result.put(supName, staticTableMetadataProvider
                .getGroupsTableTemplate().createTableHandle(
                        supName,
                        tableHandle.getReportConfigId(),
                        tableHandle.getDescription()
                ));
    }

    public CyodaTableHandle getTableHandle(AuthContext authContext, String tableName) {
        Map<String, CyodaTableHandle> map = tableByUserCache.get(authContext);
        return Optional.ofNullable(map.get(tableName)).orElseThrow(
                () -> new NoSuchElementException(String.format(
                        "Metadata provider %s does not contain table with name %s, existing keys: %s",
                        this.getClass().getSimpleName(), tableName, Arrays.toString(map.keySet().toArray())))
        );
    }

    public List<String> getTableList(AuthContext authContext) {
        Map<String, CyodaTableHandle> map = tableByUserCache.get(authContext);
        return map.keySet().stream().toList();
    }

    private static class TableMetaCacheKey {
        private final String configId;
        private final long createDate;
        private final long lastUpdateDate;

        private TableMetaCacheKey(String configId, long createDate, long lastUpdateDate) {
            this.configId = configId;
            this.createDate = createDate;
            this.lastUpdateDate = lastUpdateDate;
        }

        public static TableMetaCacheKey of(GridConfigFieldsView view) {
            return new TableMetaCacheKey(
                    view.getId(),
                    parseDate(view.getCreationDate()),
                    parseDate(view.getUpdateDate()));
        }

        private static long parseDate(String value) {
            if (value == null || "null".equals(value)) return 0;
            LocalDateTime localDateTime = LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
            return Timestamp.valueOf(localDateTime).getTime();
        }

        @Override
        public int hashCode() {
            return configId.hashCode() + (int) lastUpdateDate;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            //skip instanceof because we know how to use this class
            TableMetaCacheKey other = (TableMetaCacheKey) obj;
            return configId.equals(other.configId)
                    && lastUpdateDate == other.lastUpdateDate
                    && createDate == other.createDate;
        }

        @Override
        public String toString() {
            return configId + "(" + lastUpdateDate + ")";
        }
    }


}
