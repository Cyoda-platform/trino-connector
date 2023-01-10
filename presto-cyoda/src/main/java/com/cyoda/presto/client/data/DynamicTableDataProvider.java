package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.data.ReportRowsApiHandler;
import com.cyoda.presto.client.reporting.data.RowHandle;
import com.cyoda.presto.client.reporting.groups.GroupsRequestKey;
import com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportMetadataProvider;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.connector.ConnectorSplitSource;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.FixedSplitSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_GROUPING_VERSION_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_GROUP_JSON_BASE64_VARIABLE;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ROW_NUMBER_COLUMN;

public class DynamicTableDataProvider extends TableDataProvider<RowHandle> {
    private final ReportHistoryApiHandler reportHistoryApiHandler;
    private final ReportGroupsApiHandler reportGroupsApiHandler;
    private final ReportRowsApiHandler reportRowsApiHandler;
    private final CyodaColumnHandle reportIdColumn;
    private final CyodaColumnHandle groupIdColumn;

    public DynamicTableDataProvider(ReportHistoryApiHandler reportHistoryApiHandler,
                                    ReportGroupsApiHandler reportGroupsApiHandler,
                                    ReportRowsApiHandler reportRowsApiHandler,
                                    StaticReportMetadataProvider reportMetadataProvider) {
        this.reportHistoryApiHandler = reportHistoryApiHandler;
        this.reportGroupsApiHandler = reportGroupsApiHandler;
        this.reportRowsApiHandler = reportRowsApiHandler;

        reportIdColumn = reportMetadataProvider.getReportRows().getReportIdColumn();
        groupIdColumn = reportMetadataProvider.getReportRows().getGroupJsonBase64Column();
    }

    @Override
    public ConnectorSplitSource getSplits(AuthContext authContext, CyodaTableHandle tableHandle, Constraint constraint) {
        return new FixedSplitSource(
                reportHistoryApiHandler.getByKey(tableHandle.getReportConfigId())
                .stream()
                .filter(fieldsView -> acceptVal(reportIdColumn, fieldsView.getReportId(), constraint))
                .flatMap(fieldsView -> reportGroupsApiHandler.getByKey(
                                new GroupsRequestKey(fieldsView.getReportId(), fieldsView.getGroupingVersion())).stream()
                ).filter(groupingHandle -> acceptVal(
                        groupIdColumn, groupingHandle.groupHeader.getGroupValuesJsonBase64(), constraint)
                )
                .map(groupingHandle -> new CyodaSplit(
                        tableHandle.getTableName(),
                        tableHandle.getReportConfigId(),
                        groupingHandle.reportId,
                        groupingHandle.groupingVersion,
                        groupingHandle.groupHeader.getGroupValuesJsonBase64()))
                .toList());
    }

    @Override
    public Iterable<RowHandle> getIterable(AuthContext authContext, CyodaTableHandle tableHandle, CyodaSplit split) {


        return reportRowsApiHandler.getIterable(split);
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull RowHandle field, CyodaColumnHandle columnHandle) {
        if (ROW_REPORT_ROW_NUMBER_COLUMN.equals(columnHandle.getColumnName())) {
            return field.rowNum();
        }
        if (ROW_REPORT_ID_COLUMN.equals(columnHandle.getColumnName())) {
            return field.reportId();
        }
        if (ROW_GROUPING_VERSION_COLUMN.equals(columnHandle.getColumnName())) {
            return field.groupingVersion();
        }
        if (ROW_GROUP_JSON_BASE64_VARIABLE.equals(columnHandle.getColumnName())) {
            return field.groupJsonBase64();
        }
        return columnHandle.getValue(field.reportRow());
    }

}
