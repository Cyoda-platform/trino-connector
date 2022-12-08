package com.cyoda.presto.client.reporting.metaproviders;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.type.TypeManager;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_REPORT_NAME_VARIABLE;
import static com.cyoda.presto.client.reporting.data.ReportRowsApiHandler.ROW_GROUP_JSON_BASE64_VARIABLE;
import static com.cyoda.presto.client.reporting.data.ReportRowsApiHandler.ROW_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.data.ReportRowsApiHandler.ROW_REPORT_ROW_NUMBER_COLUMN;
import static com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler.GROUPING_VERSION_COLUMN;

public class StaticReportMetadataProvider extends TableMetadataProvider {

    private final Map<String, StaticTableMetadata> metadataMap = new HashMap<>();

    private final Reports reports;
    private final ReportDetails reportDetails;
    private final ReportHistory reportHistory;
    private final ReportGroups reportGroups;
    private final ReportRows reportRows;


    @Inject
    public StaticReportMetadataProvider(TypeManager typeManager, CyodaConfig config, CyodaConnectorId connectorId) {
        super(typeManager, config, connectorId);
        reports = new Reports();
        reportDetails = new ReportDetails();
        reportHistory = new ReportHistory();
        reportGroups = new ReportGroups();
        reportRows = new ReportRows();
    }

    public CyodaTableHandle getTableHandle(String tableName) {
        return Optional.ofNullable(metadataMap.get(tableName)).orElseThrow(
                () -> new NoSuchElementException(String.format(
                        "Metadata provider %s does not contain table with name %s",
                        this.getClass().getSimpleName(), tableName))
        ).getTableHandle();
    }

    public List<String> getTableList() {
        return metadataMap.keySet().stream().toList();
    }

    public boolean contains(String tableName) {
        return metadataMap.containsKey(tableName);
    }

    public Reports getReports() {
        return reports;
    }

    public ReportDetails getReportDetails() {
        return reportDetails;
    }

    public ReportHistory getReportHistory() {
        return reportHistory;
    }

    public ReportGroups getReportGroups() {
        return reportGroups;
    }

    public ReportRows getReportRows() {
        return reportRows;
    }

    public abstract class StaticTableMetadata {

        private final CyodaTableHandle tableHandle;

        protected StaticTableMetadata(StaticReportTable table) {
            tableHandle = createTableHandle(table);
            metadataMap.put(table.getTableName(), this);
        }

        private CyodaColumnHandle createColumnHandle(ColumnDefinition fieldDef) {
            return new CyodaColumnHandle(
                    fieldDef.getFieldName(),
                    fieldDef.getDataType().toPrestoType(typeManager),
                    fieldDef.getDataType(),
                    fieldDef.getPos(),
                    true
            );
        }

        private CyodaTableHandle createTableHandle(TableDefinition tableDefinition) {

            List<CyodaColumnHandle> columnHandles = tableDefinition.getColumns().stream()
                    .map(this::createColumnHandle)
                    .toList();
            return new CyodaTableHandle(connectorId.toString(), config.getSchemaName(),
                    tableDefinition.getTableName(), columnHandles, tableDefinition.getRequestHandlerKey(),
                    null, null, getUri(tableDefinition));
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

    public class ReportDetails extends StaticTableMetadata {
        public ReportDetails() {
            super(StaticReportTable.REPORT_DETAILS);
        }
    }

    public class ReportHistory extends StaticTableMetadata {
        private final CyodaColumnHandle typeColumn;
        private final CyodaColumnHandle reportNameColumn;
        private final CyodaColumnHandle reportIdColumn;

        public ReportHistory(){
            super(StaticReportTable.REPORT_HISTORIES);
            typeColumn = getTableHandle().getColumn(StaticReportTable.ReportHistoryColumnDef.TYPE.getFieldName());
            reportNameColumn = getTableHandle().getColumn(StaticReportTable.ReportHistoryColumnDef.REPORT_NAME.getFieldName());
            reportIdColumn = getTableHandle().getColumn(StaticReportTable.ReportHistoryColumnDef.ID.getFieldName());
        }

        public CyodaColumnHandle getTypeColumn() {
            return typeColumn;
        }

        public CyodaColumnHandle getReportNameColumn() {
            return reportNameColumn;
        }

        public CyodaColumnHandle getReportIdColumn() {
            return reportIdColumn;
        }
    }

    public class ReportGroups extends StaticTableMetadata {
        private final CyodaColumnHandle reportIdColumn;
        private final CyodaColumnHandle groupingVersionColumn;
        private final CyodaColumnHandle groupJsonBase64Column;
        private final CyodaColumnHandle reportConfigIdColumn;

        public ReportGroups() {
            super(StaticReportTable.REPORT_GROUPS);
            reportIdColumn = getTableHandle().getColumn(HISTORY_REPORT_ID_COLUMN);
            groupingVersionColumn = getTableHandle().getColumn(GROUPING_VERSION_COLUMN);
            groupJsonBase64Column = getTableHandle().getColumn(ROW_GROUP_JSON_BASE64_VARIABLE);
            reportConfigIdColumn = getTableHandle().getColumn(HISTORY_REPORT_NAME_VARIABLE);
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

        public CyodaColumnHandle getReportConfigIdColumn() {
            return reportConfigIdColumn;
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
            metadataMap.remove(StaticReportTable.REPORT_ROWS.getTableName());
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
