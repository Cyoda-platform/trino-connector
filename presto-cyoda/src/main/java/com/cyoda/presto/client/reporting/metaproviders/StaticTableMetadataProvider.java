package com.cyoda.presto.client.reporting.metaproviders;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.type.TypeManager;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.GROUPING_VERSION_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_GROUP_JSON_BASE64_VARIABLE;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ROW_NUMBER_COLUMN;

public class StaticTableMetadataProvider extends TableMetadataProvider {

    private final Map<String, StaticTableMetadata> standaloneTablesMap;

    private final ApiCallStats apiCallStats;
    private final CacheContent cacheContent;
    private final ReportGroups reportGroups;
    private final ReportRows reportRows;

    private final CyodaTableHandle.Template historyTableTemplate;
    private final CyodaTableHandle.Template groupsTableTemplate;


    @Inject
    public StaticTableMetadataProvider(TypeManager typeManager, CyodaConfig config, CyodaConnectorId connectorId) {
        super(typeManager, config, connectorId);
        standaloneTablesMap = Arrays.stream(com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata.values())
                .filter(t -> t.getStaticTableName() != null)
                .collect(Collectors.toMap(com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata::getStaticTableName, StaticTableMetadataProvider.StaticTableMetadata::new));
        apiCallStats = new ApiCallStats();
        if (!config.getLogApiCallStats()) {
            standaloneTablesMap.remove(apiCallStats.getTableHandle().getTableName());
        }
        reportGroups = new ReportGroups();
        reportRows = new ReportRows();
        cacheContent = new CacheContent();

        historyTableTemplate = CyodaTableHandle.Template.of(new StaticTableMetadata(com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata.REPORT_HISTORIES).getTableHandle());
        groupsTableTemplate = CyodaTableHandle.Template.of(reportGroups.getTableHandle());
    }

    public CyodaTableHandle getTableHandle(String tableName) {
        return Optional.ofNullable(standaloneTablesMap.get(tableName)).orElseThrow(
                () -> new NoSuchElementException(String.format(
                        "Metadata provider %s does not contain table with name %s",
                        this.getClass().getSimpleName(), tableName))
        ).getTableHandle();
    }

    public List<String> getTableList() {
        return standaloneTablesMap.keySet().stream().toList();
    }

    public boolean contains(String tableName) {
        return standaloneTablesMap.containsKey(tableName);
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

    public CyodaTableHandle.Template getHistoryTableTemplate() {
        return historyTableTemplate;
    }

    public CyodaTableHandle.Template getGroupsTableTemplate() {
        return groupsTableTemplate;
    }

    private List<CyodaColumnHandle> getCyodaColumnHandles(TableDefinition tableDefinition) {
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

    public class StaticTableMetadata {

        private final CyodaTableHandle tableHandle;

        protected StaticTableMetadata(com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata table) {
            tableHandle = createTableHandle(table);
        }

        private CyodaTableHandle createTableHandle(TableDefinition tableDefinition) {

            List<CyodaColumnHandle> columnHandles = getCyodaColumnHandles(tableDefinition);
            return new CyodaTableHandle(connectorId.toString(), config.getSchemaName(),
                    tableDefinition.getTableName(), columnHandles, tableDefinition.getTableType(),
                    null, null, false, false);
        }

        public CyodaTableHandle getTableHandle() {
            return tableHandle;
        }
    }

    public class Reports extends StaticTableMetadata {
        private final CyodaColumnHandle typeColumn;

        public Reports() {
            super(com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata.REPORTS);
            typeColumn = getTableHandle().getColumn(com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata.ReportsColumnDef.TYPE.getFieldName());
        }

        public CyodaColumnHandle getTypeColumn() {
            return typeColumn;
        }
    }


    public class ReportStats extends StaticTableMetadata {
        public ReportStats() {
            super(com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata.REPORT_STATS);
        }
    }

    public class ApiCallStats extends StaticTableMetadata {
        private final CyodaColumnHandle nodeIdColumn;
        public ApiCallStats() {
            super(com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata.API_CALL_STATS);
            nodeIdColumn = getTableHandle().getColumn(com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata.ApiCallStatsColumnDef.NODE_ID.getFieldName());
        }
        public CyodaColumnHandle getNodeIdColumn() {
            return nodeIdColumn;
        }
    }

    public class CacheContent extends StaticTableMetadata {
        private final CyodaColumnHandle cacheKeyColumn;
        public CacheContent() {
            super(com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata.CACHE_CONTENT);
            cacheKeyColumn = getTableHandle().getColumn(com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata.CacheContentColumnDef.CONTENT_ID.getFieldName());
        }
        public CyodaColumnHandle getCacheKeyColumn() {
            return cacheKeyColumn;
        }
    }


    public class ReportGroups extends StaticTableMetadata {
        private final CyodaColumnHandle reportIdColumn;
        private final CyodaColumnHandle groupingVersionColumn;
        private final CyodaColumnHandle groupJsonBase64Column;

        public ReportGroups() {
            super(com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata.REPORT_GROUPS);
            reportIdColumn = getTableHandle().getColumn(HISTORY_REPORT_ID_COLUMN);
            groupingVersionColumn = getTableHandle().getColumn(GROUPING_VERSION_COLUMN);
            groupJsonBase64Column = getTableHandle().getColumn(ROW_GROUP_JSON_BASE64_VARIABLE);
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

    public class ReportRows extends StaticTableMetadata {

        private final CyodaColumnHandle rowNumberColumn;
        private final CyodaColumnHandle reportIdColumn;
        private final CyodaColumnHandle groupingVersionColumn;
        private final CyodaColumnHandle groupJsonBase64Column;

        protected ReportRows() {
            super(com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata.REPORT_ROWS);
            rowNumberColumn = getTableHandle().getColumn(ROW_REPORT_ROW_NUMBER_COLUMN);
            reportIdColumn = getTableHandle().getColumn(ROW_REPORT_ID_COLUMN);
            groupingVersionColumn = getTableHandle().getColumn(GROUPING_VERSION_COLUMN);
            groupJsonBase64Column = getTableHandle().getColumn(ROW_GROUP_JSON_BASE64_VARIABLE);
            //we don't need this as a static table
            standaloneTablesMap.remove(com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata.REPORT_ROWS.getTableName());
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
