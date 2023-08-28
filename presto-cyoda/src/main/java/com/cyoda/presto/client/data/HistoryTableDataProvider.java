package com.cyoda.presto.client.data;

import com.cyoda.core.model.reports.ReportHistoryFieldsView;
import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.meta.ReportConfigKey;
import com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import io.trino.spi.connector.Constraint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.HISTORY_REPORT_ID_COLUMN;

public class HistoryTableDataProvider extends TableDataProvider<ReportHistoryFieldsView> {

    private final ReportHistoryApiHandler reportHistoryApiHandler;


    public HistoryTableDataProvider(ReportHistoryApiHandler reportHistoryApiHandler) {
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
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        return Collections.singletonList(
                new CyodaSplit(queryId, tableHandle.getTableName(), tableHandle.getReportConfigId(), null, null, null));
    }

    @Override
    public Iterable<ReportHistoryFieldsView> getIterable(CyodaTableMeta tableHandle, CyodaSplit split) {
        return reportHistoryApiHandler.getByKey(new ReportConfigKey(split.getReportConfigId(), split.getQueryId()));
    }
}
