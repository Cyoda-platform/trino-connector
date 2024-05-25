package com.cyoda.presto.client.data;

import com.cyoda.api.view.GridConfigFieldsView;
import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.meta.ConfiguredReportsApi;
import com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApi;
import com.cyoda.presto.client.reporting.meta.ReportConfigKey;
import com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle;
import com.cyoda.presto.client.reporting.meta.ReportListKey;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import io.trino.spi.connector.Constraint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_COLUMNS_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_ERROR_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_JSON_COLUMN;

public class ReportsTableDataProvider extends TableDataProvider<ReportsTableData> {

    private final ConfiguredReportsApi reportsApiHandler;
    private final ReportConfigDetailsApi configDetailsApiHandler;

    public ReportsTableDataProvider(ConfiguredReportsApi reportsApiHandler, ReportConfigDetailsApi configDetailsApiHandler) {
        this.reportsApiHandler = reportsApiHandler;
        this.configDetailsApiHandler = configDetailsApiHandler;
    }

    @Override
    public Iterable<ReportsTableData> getIterable(CyodaTableMeta tableHandle, CyodaSplit split) {
        return split.getCustomData().entrySet().stream()
                .map(entry -> {
                    try {
                        ReportDefinitionHandle repDef = configDetailsApiHandler.getReportDefSingleHandle(
                                new ReportConfigKey(entry.getKey(), split.getQueryId()));
                        return new ReportsTableData((Map<String, String>) entry.getValue(), repDef, null);
                    } catch (Exception e){
                        return new ReportsTableData((Map<String, String>) entry.getValue(), null, e.getMessage());
                    }
                }).toList();
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull ReportsTableData entity, CyodaColumnHandle columnHandle) {
        if (entity.config() != null) {
            if (REPORT_JSON_COLUMN.equals(columnHandle.getColumnName())) {
                return entity.config().json;
            }
            if (REPORT_COLUMNS_COLUMN.equals(columnHandle.getColumnName())) {
                return entity.config().columns;
            }
        }
        if (REPORT_ERROR_COLUMN.equals(columnHandle.getColumnName())){
            return entity.error();
        }

        Map<String, String> fields = entity.reportFields();
        return fields.get(columnHandle.getColumnName());
    }

    @Override
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        Map<String, Map<String, String>> configFields = reportsApiHandler.asFlux(new ReportListKey(authContext, queryId), SizeListener.NOT_LISTENING)
                .collectMap(GridConfigFieldsView::getId, GridConfigFieldsView::getGridConfigFields).block();
        return Collections.singletonList(CyodaSplit.emptyCoordinatorSplit(queryId, authContext.getUserId(), configFields));
    }
}
