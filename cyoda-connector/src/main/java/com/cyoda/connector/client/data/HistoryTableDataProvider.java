package com.cyoda.connector.client.data;

import com.cyoda.core.model.reports.ReportHistoryFieldsView;
import com.cyoda.connector.CyodaSplit;
import com.cyoda.connector.auth.AuthContext;
import com.cyoda.connector.client.reporting.meta.ReportConfigKey;
import com.cyoda.connector.client.reporting.meta.ReportHistoryApi;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.handles.CyodaTableHandle;
import com.cyoda.connector.handles.CyodaTableMeta;
import io.trino.spi.connector.Constraint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HistoryTableDataProvider extends TableDataProvider<ReportHistoryFieldsView> {

    private final ReportHistoryApi reportHistoryApiHandler;


    public HistoryTableDataProvider(ReportHistoryApi reportHistoryApiHandler) {
        this.reportHistoryApiHandler = reportHistoryApiHandler;
    }


    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull ReportHistoryFieldsView entity, CyodaColumnHandle columnHandle) {
        Map<String, Object> fields = entity.getReportHistoryFields();

        String columnName = columnHandle.getColumnName();
//        if (HISTORY_REPORT_ID_COLUMN.equals(columnName)) {
//            columnName = "id";
//        }
        return fields.get(columnName);
    }

    @Override
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        return Collections.singletonList(
                new CyodaSplit(queryId, authContext.getUserId(), tableHandle.getTableMetaId(), null, null, null));
    }

    @Override
    public Iterable<ReportHistoryFieldsView> getIterable(CyodaTableMeta tableHandle, CyodaSplit split) {
        return reportHistoryApiHandler.getByKey(new ReportConfigKey(split.getCyodaTableMetaId(), split.getQueryId()));
    }
}
