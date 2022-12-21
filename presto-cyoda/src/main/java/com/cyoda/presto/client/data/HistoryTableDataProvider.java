package com.cyoda.presto.client.data;

import com.cyoda.core.model.reports.ReportHistoryFieldsView;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.HISTORY_REPORT_ID_COLUMN;

public class HistoryTableDataProvider extends TableDataProvider<ReportHistoryFieldsView> {

    private final CyodaTableHandle tableHandle;
    private final ReportHistoryApiHandler reportHistoryApiHandler;


    public HistoryTableDataProvider(CyodaTableHandle tableHandle, ReportHistoryApiHandler reportHistoryApiHandler) {
        this.tableHandle = tableHandle;
        this.reportHistoryApiHandler = reportHistoryApiHandler;
    }


    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull ReportHistoryFieldsView entity, CyodaColumnHandle columnHandle) {
        Map<String, Object> fields = entity.getReportHistoryFields();

        String columnName = columnHandle.getColumnName();
        if (HISTORY_REPORT_ID_COLUMN.equals(columnName)) {
            columnName = "id";
        }
        return fields.get(columnName);
    }

    @Override
    public Iterable<ReportHistoryFieldsView> getIterable(AuthContext authContext, CyodaTableHandle tableHandle, CompoundPredicateNode predicates) {
        return reportHistoryApiHandler.getByKey(tableHandle.getReportConfigId());
    }
}
