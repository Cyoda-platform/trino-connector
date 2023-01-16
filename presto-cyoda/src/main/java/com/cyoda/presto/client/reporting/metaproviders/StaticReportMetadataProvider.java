package com.cyoda.presto.client.reporting.metaproviders;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.type.TypeManager;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.GROUPING_VERSION_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_GROUP_JSON_BASE64_VARIABLE;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ROW_NUMBER_COLUMN;

public class StaticReportMetadataProvider extends TableMetadataProvider {

    private final Map<String, StaticTableMetadata> standaloneTablesMap = new HashMap<>();

    private final Reports reports;
    private final ReportStats reportStats;
    private final ApiCallStats apiCallStats;
    private final ReportHistory reportHistory;
    private final ReportGroups reportGroups;
    private final ReportRows reportRows;

    private final CyodaTableHandle.Template historyTableTemplate;
    private final CyodaTableHandle.Template groupsTableTemplate;


    @Inject
    public StaticReportMetadataProvider(TypeManager typeManager, CyodaConfig config, CyodaConnectorId connectorId) {
        super(typeManager, config, connectorId);
        reports = new Reports();
        reportStats = new ReportStats();
        reportHistory = new ReportHistory();
        reportGroups = new ReportGroups();
        reportRows = new ReportRows();
        apiCallStats = new ApiCallStats();
        standaloneTablesMap.put(reports.getTableHandle().getTableName(), reports);
        standaloneTablesMap.put(reportStats.getTableHandle().getTableName(), reportStats);
        standaloneTablesMap.put(apiCallStats.getTableHandle().getTableName(), apiCallStats);

        historyTableTemplate = CyodaTableHandle.Template.of(reportHistory.getTableHandle());
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

    public Reports getReports() {
        return reports;
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

    public abstract class StaticTableMetadata {

        private final CyodaTableHandle tableHandle;

        protected StaticTableMetadata(StaticReportTable table) {
            tableHandle = createTableHandle(table);
        }

        private CyodaTableHandle createTableHandle(TableDefinition tableDefinition) {

            List<CyodaColumnHandle> columnHandles = getCyodaColumnHandles(tableDefinition);
            return new CyodaTableHandle(connectorId.toString(), config.getSchemaName(),
                    tableDefinition.getTableName(), columnHandles, tableDefinition.getTableType(),
                    null, null, getUri(tableDefinition), false, false);
        }

        public CyodaTableHandle getTableHandle() {
            return tableHandle;
        }
    }

    public class Reports extends StaticTableMetadata {
        private final CyodaColumnHandle typeColumn;

        public Reports() {
            super(StaticReportTable.REPORTS);
            typeColumn = getTableHandle().getColumn(StaticReportTable.ReportsColumnDef.TYPE.getFieldName());
        }

        public CyodaColumnHandle getTypeColumn() {
            return typeColumn;
        }
    }


    public class ReportStats extends StaticTableMetadata {
        public ReportStats() {
            super(StaticReportTable.REPORT_STATS);
        }
    }

    public class ApiCallStats extends StaticTableMetadata {
        public ApiCallStats() {
            super(StaticReportTable.API_CALL_STATS);
        }
    }

    public class ReportHistory extends StaticTableMetadata {
        private final CyodaColumnHandle typeColumn;
        private final CyodaColumnHandle reportIdColumn;

        public ReportHistory() {
            super(StaticReportTable.REPORT_HISTORIES);
            typeColumn = getTableHandle().getColumn(StaticReportTable.ReportHistoryColumnDef.TYPE.getFieldName());
            reportIdColumn = getTableHandle().getColumn(StaticReportTable.ReportHistoryColumnDef.ID.getFieldName());
        }

        public CyodaColumnHandle getTypeColumn() {
            return typeColumn;
        }

        public CyodaColumnHandle getReportIdColumn() {
            return reportIdColumn;
        }
    }

    public class ReportGroups extends StaticTableMetadata {
        private final CyodaColumnHandle reportIdColumn;
        private final CyodaColumnHandle groupingVersionColumn;
        private final CyodaColumnHandle groupJsonBase64Column;

        public ReportGroups() {
            super(StaticReportTable.REPORT_GROUPS);
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
            super(StaticReportTable.REPORT_ROWS);
            rowNumberColumn = getTableHandle().getColumn(ROW_REPORT_ROW_NUMBER_COLUMN);
            reportIdColumn = getTableHandle().getColumn(ROW_REPORT_ID_COLUMN);
            groupingVersionColumn = getTableHandle().getColumn(GROUPING_VERSION_COLUMN);
            groupJsonBase64Column = getTableHandle().getColumn(ROW_GROUP_JSON_BASE64_VARIABLE);
            //we don't need this as a static table
            standaloneTablesMap.remove(StaticReportTable.REPORT_ROWS.getTableName());
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
