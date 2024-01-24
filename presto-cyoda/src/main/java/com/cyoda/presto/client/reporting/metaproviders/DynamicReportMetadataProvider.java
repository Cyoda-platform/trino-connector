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
import com.cyoda.presto.client.reporting.stats.ContentIdLoadingCache;
import com.cyoda.presto.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import com.cyoda.presto.handles.CyodaTableType;
import com.cyoda.presto.logging.SupplierLogger;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.type.TypeManager;
import reactor.core.publisher.Flux;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.cyoda.presto.SizeListener.NOT_LISTENING;

public class DynamicReportMetadataProvider extends TableMetadataProvider {
    private static final SupplierLogger LOG = SupplierLogger.get(DynamicReportMetadataProvider.class);


    private final ConfiguredReportsApiHandler configuredReportsApiHandler;
    private final ReportConfigDetailsApiHandler reportConfigDetailsApiHandler;
    private final StaticTableMetadataProvider staticTableMetadataProvider;

    private final ContentIdLoadingCache<TableMetaCacheKey, CyodaTableMeta> tableMetaCache;

    @Inject
    public DynamicReportMetadataProvider(CyodaConnectorId connectorId, CyodaConfig config,
                                         TypeManager typeManager, AuthService auth,
                                         StaticTableMetadataProvider staticTableMetadataProvider,
                                         ConfiguredReportsApiHandler configuredReportsApiHandler,
                                         ReportConfigDetailsApiHandler reportConfigDetailsApiHandler,
                                         CyodaCacheMonitor cacheMonitor) {
        super(typeManager, config, connectorId);
        this.staticTableMetadataProvider = staticTableMetadataProvider;
        this.configuredReportsApiHandler = configuredReportsApiHandler;
        this.reportConfigDetailsApiHandler = reportConfigDetailsApiHandler;
        tableMetaCache = new ContentIdLoadingCache<>(Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(config.getCacheReportMetaHoursAfterAccess()))
                .recordStats()
                .build(key -> {
                    LOG.debug("Loading config " + key);
                    return getTableHandleFromCyoda(key.configId);
                }));
        cacheMonitor.register("META", tableMetaCache, key -> key.configId, x->1);
    }

    @Override
    //results of this method supposed to be cached outside
    public List<CyodaTableHandle> listTables(AuthContext authContext) {
        try {
            List<CyodaTableHandle> result = new ArrayList<>();
            Flux<GridConfigFieldsView> flux = configuredReportsApiHandler.asFlux(
                    new ReportListKey(authContext, "META"),
                    NOT_LISTENING
            );
            flux.doOnNext(item -> {
                TableMetaCacheKey cacheKey = TableMetaCacheKey.of(item);
                CyodaTableMeta tableMeta = tableMetaCache.get(cacheKey);
                if (tableMeta == null) {
                    LOG.error("Failed to get meta for table " + item.getId());
                    return;
                }
                result.add(tableMeta.toMainHandle(cacheKey.createDate, cacheKey.lastUpdateDate));
                if (tableMeta.hasHistory()) {
                    result.add(tableMeta.toSuppHandle("_history", CyodaTableType.HISTORY));
                }
                if (tableMeta.hasGroups()) {
                    result.add(tableMeta.toSuppHandle("_groups", CyodaTableType.GROUP));
                }
            }).blockLast();
            // If there are duplicates, last write wins.
            return result;
        } catch (Exception e){
            LOG.error(e, "Loading table list for a user failed with message " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public CyodaTableMeta getTableMeta(CyodaTableHandle tableHandle) {
        return tableMetaCache.get(TableMetaCacheKey.of(tableHandle));
    }

    private CyodaTableMeta getTableHandleFromCyoda(String configId) {
        SchemaTableName tableName = BaseReportsApiHandler.configIdToSchemaTableName(configId);
        ReportDefinitionHandle definitionHandle;
        try {
            definitionHandle = reportConfigDetailsApiHandler.getReportDefSingleHandle(new ReportConfigKey(configId, "META"));

            List<CyodaColumnHandle> columns = new ArrayList<>(List.copyOf(staticTableMetadataProvider.getReportRows().getTableMeta().getProjectedColumns()));
            columns.addAll(definitionHandle.getColumns());
            columns.sort(Comparator.comparingInt(CyodaColumnHandle::getOrdinalPosition));
            return new CyodaTableMeta(connectorId.toString(), tableName.getSchemaName(), tableName.getTableName(),
                    columns, CyodaTableType.DATA, configId, definitionHandle.getDescription(),
                    !definitionHandle.getGroupingColumns().isEmpty(),
                    !definitionHandle.isSingleton());
        } catch (Exception e) {
            LOG.error(e);
            return null;
        }
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
        public static TableMetaCacheKey of(CyodaTableHandle handle) {
            return new TableMetaCacheKey(
                    handle.getReportConfigId(), handle.getCreateDate(), handle.getLastUpdateDate()
            );
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
