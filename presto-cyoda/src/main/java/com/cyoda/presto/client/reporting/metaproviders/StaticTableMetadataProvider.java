package com.cyoda.presto.client.reporting.metaproviders;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import com.cyoda.presto.logging.SupplierLogger;
import io.trino.spi.type.TypeManager;

import jakarta.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.GROUPING_VERSION_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_GROUP_JSON_BASE64_VARIABLE;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ROW_NUMBER_COLUMN;

public class StaticTableMetadataProvider extends TableMetadataProvider {

    private static final SupplierLogger LOG = SupplierLogger.get(StaticTableMetadataProvider.class);
    private final Map<CyodaTableHandle, CyodaTableMeta> standaloneTablesMap;

    private final ApiCallStats apiCallStats;
    private final CacheContent cacheContent;
    private final ReportGroups reportGroups;
    private final ReportRows reportRows;

    private final CyodaTableMeta.Template historyTableTemplate;
    private final CyodaTableMeta.Template groupsTableTemplate;

    private final SimpleStaticCache<CyodaTableHandle, CyodaTableMeta> staticMetaCache = new SimpleStaticCache<>();
    //Since everything, that is provided by that class, are compiled from hardcoded sources
    //we can use hashmap as cache (just need to clear it once a day)
    static class SimpleStaticCache<K, V> extends ConcurrentHashMap<K,V> {
        private volatile Instant clearingDue = Instant.now().plus(1, ChronoUnit.DAYS);
        private void clearIfNeeded(){
            Instant now = Instant.now();
            if (clearingDue.compareTo(now) < 0){
                synchronized (this) {
                    if (clearingDue.compareTo(now) < 0) {
                        LOG.info("Daily cleaning of static cache, " + size() + " records removed.");
                        clear();
                        clearingDue = now.plus(1, ChronoUnit.DAYS);
                    }
                }
            }
        }
        @Override
        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            return super.computeIfAbsent(key, tableHandle -> {
                clearIfNeeded();
                return mappingFunction.apply(tableHandle);
            });
        }
    }


    @Inject
    public StaticTableMetadataProvider(TypeManager typeManager, CyodaConfig config, CyodaConnectorId connectorId) {
        super(typeManager, config, connectorId);
        standaloneTablesMap = Arrays.stream(StaticTableMetadata.values())
                .filter(t -> t.getStaticTableName() != null)
                .map(StaticTable::new)
                .collect(Collectors.toMap(StaticTable::getTableHandle, StaticTable::getTableMeta));
        apiCallStats = new ApiCallStats();
        if (!config.getLogApiCallStats()) {
            standaloneTablesMap.remove(apiCallStats.getTableHandle());
        }
        reportGroups = new ReportGroups();
        reportRows = new ReportRows();
        cacheContent = new CacheContent();

        historyTableTemplate = CyodaTableMeta.Template.of(new StaticTable(StaticTableMetadata.REPORT_HISTORIES).getTableMeta());
        groupsTableTemplate = CyodaTableMeta.Template.of(reportGroups.getTableMeta());
    }

//    public CyodaTableMeta getTableHandle(String tableName) {
//        return Optional.ofNullable(standaloneTablesMap.get(tableName)).orElseThrow(
//                () -> new NoSuchElementException(String.format(
//                        "Metadata provider %s does not contain table with name %s",
//                        this.getClass().getSimpleName(), tableName))
//        ).getTableMeta();
//    }
    @Override
    public List<CyodaTableHandle> listTables(AuthContext authContext) {
        return standaloneTablesMap.keySet().stream().toList();
    }

    @Override
    public CyodaTableMeta getTableMeta(CyodaTableHandle tableHandle) {
        return staticMetaCache.computeIfAbsent(tableHandle, handle -> {
            switch (tableHandle.getTableType()){
                case HISTORY -> {
                    return getHistoryTableTemplate().createTableMeta(tableHandle);
                }
                case GROUP -> {
                    return getGroupsTableTemplate().createTableMeta(tableHandle);
                }
                default -> {
                    return standaloneTablesMap.get(tableHandle);
                }
            }
        });
    }

    public ApiCallStats getApiCallStats() {
        return apiCallStats;
    }

    public CacheContent getCacheContent() {
        return cacheContent;
    }

    public ReportGroups getReportGroups(){
        return reportGroups;
    }
    public ReportRows getReportRows() {
        return reportRows;
    }

    public CyodaTableMeta.Template getHistoryTableTemplate() {
        return historyTableTemplate;
    }

    public CyodaTableMeta.Template getGroupsTableTemplate() {
        return groupsTableTemplate;
    }

    private List<CyodaColumnHandle> getCyodaColumnHandles(StaticTableMetadata tableDefinition) {
        return tableDefinition.getColumns().stream()
                .sorted(Comparator.comparingInt(ColumnDefinition::getPos))
                .map(fieldDef -> new CyodaColumnHandle(
                        fieldDef.getFieldName(),
                        fieldDef.getDataType().toPrestoType(typeManager),
                        fieldDef.getDataType(),
                        fieldDef.getPos(),
                        true
                ))
                .toList();
    }

    public class StaticTable {

        private final CyodaTableHandle tableHandle;
        private final CyodaTableMeta tableMeta;

        protected StaticTable(StaticTableMetadata table) {
            tableMeta = createTableMeta(table);
            tableHandle = createTableHandle(table);
        }

        private CyodaTableMeta createTableMeta(StaticTableMetadata tableDefinition) {
            List<CyodaColumnHandle> columnHandles = getCyodaColumnHandles(tableDefinition);
            return new CyodaTableMeta(config.getSchemaName(),
                    getTableName(tableDefinition),
                    columnHandles, tableDefinition.getTableType(),
                    null, tableDefinition.getDescription(), false, false);
        }

        private String getTableName(StaticTableMetadata tableDefinition) {
            return tableDefinition.getStaticTableName() != null ? tableDefinition.getStaticTableName() : tableDefinition.name();
        }

        private CyodaTableHandle createTableHandle(StaticTableMetadata tableDefinition) {
            return new CyodaTableHandle(config.getSchemaName(), getTableName(tableDefinition), tableDefinition.getTableType());
        }

        public CyodaTableMeta getTableMeta() {
            return tableMeta;
        }

        public CyodaTableHandle getTableHandle() {
            return tableHandle;
        }
    }

    public class Reports extends StaticTable {
        private final CyodaColumnHandle typeColumn;

        public Reports() {
            super(StaticTableMetadata.REPORTS);
            typeColumn = getTableMeta().getColumn(StaticTableMetadata.ReportsColumnDef.TYPE.getFieldName());
        }

        public CyodaColumnHandle getTypeColumn() {
            return typeColumn;
        }
    }


    public class ReportStats extends StaticTable {
        public ReportStats() {
            super(StaticTableMetadata.REPORT_STATS);
        }
    }

    public class ApiCallStats extends StaticTable {
        private final CyodaColumnHandle nodeIdColumn;
        public ApiCallStats() {
            super(StaticTableMetadata.API_CALL_STATS);
            nodeIdColumn = getTableMeta().getColumn(StaticTableMetadata.ApiCallStatsColumnDef.NODE_ID.getFieldName());
        }
        public CyodaColumnHandle getNodeIdColumn() {
            return nodeIdColumn;
        }
    }

    public class CacheContent extends StaticTable {
        private final CyodaColumnHandle cacheKeyColumn;
        public CacheContent() {
            super(StaticTableMetadata.CACHE_CONTENT);
            cacheKeyColumn = getTableMeta().getColumn(StaticTableMetadata.CacheContentColumnDef.CONTENT_ID.getFieldName());
        }
        public CyodaColumnHandle getCacheKeyColumn() {
            return cacheKeyColumn;
        }
    }


    public class ReportGroups extends StaticTable {
        private final CyodaColumnHandle reportIdColumn;
        private final CyodaColumnHandle groupingVersionColumn;
        private final CyodaColumnHandle groupJsonBase64Column;

        public ReportGroups() {
            super(StaticTableMetadata.REPORT_GROUPS);
            reportIdColumn = getTableMeta().getColumn(HISTORY_REPORT_ID_COLUMN);
            groupingVersionColumn = getTableMeta().getColumn(GROUPING_VERSION_COLUMN);
            groupJsonBase64Column = getTableMeta().getColumn(ROW_GROUP_JSON_BASE64_VARIABLE);
        }

        public CyodaColumnHandle getReportIdColumn() {
            return reportIdColumn;
        }

        public CyodaColumnHandle getGroupingVersionColumn() {
            return groupingVersionColumn;
        }

        public CyodaColumnHandle getGroupJsonBase64Column() {
            return groupJsonBase64Column;
        }

    }

    public class ReportRows extends StaticTable {

        private final CyodaColumnHandle rowNumberColumn;
        private final CyodaColumnHandle reportIdColumn;
        private final CyodaColumnHandle groupingVersionColumn;
        private final CyodaColumnHandle groupJsonBase64Column;

        protected ReportRows() {
            super(StaticTableMetadata.REPORT_ROWS);
            rowNumberColumn = getTableMeta().getColumn(ROW_REPORT_ROW_NUMBER_COLUMN);
            reportIdColumn = getTableMeta().getColumn(ROW_REPORT_ID_COLUMN);
            groupingVersionColumn = getTableMeta().getColumn(GROUPING_VERSION_COLUMN);
            groupJsonBase64Column = getTableMeta().getColumn(ROW_GROUP_JSON_BASE64_VARIABLE);
        }

        public CyodaColumnHandle getRowNumberColumn() {
            return rowNumberColumn;
        }

        public CyodaColumnHandle getReportIdColumn() {
            return reportIdColumn;
        }

        public CyodaColumnHandle getGroupingVersionColumn() {
            return groupingVersionColumn;
        }

        public CyodaColumnHandle getGroupJsonBase64Column() {
            return groupJsonBase64Column;
        }
    }
}
