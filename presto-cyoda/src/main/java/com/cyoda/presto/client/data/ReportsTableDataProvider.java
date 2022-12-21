package com.cyoda.presto.client.data;

import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;

import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_COLUMNS_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_JSON_COLUMN;

public class ReportsTableDataProvider extends TableDataProvider<ReportsTableData>{

    private final ConfiguredReportsApiHandler reportsApiHandler;
    private final ReportConfigDetailsApiHandler configDetailsApiHandler;

    public ReportsTableDataProvider(ConfiguredReportsApiHandler reportsApiHandler, ReportConfigDetailsApiHandler configDetailsApiHandler) {
        this.reportsApiHandler = reportsApiHandler;
        this.configDetailsApiHandler = configDetailsApiHandler;
    }

    @Override
    public Iterable<ReportsTableData> getIterable(AuthContext authContext, CyodaTableHandle tableHandle, CompoundPredicateNode predicates) {
        return reportsApiHandler.asFlux(authContext, SizeListener.NOT_LISTENING)
                .map(gridConfigView -> {
                    ReportDefinitionHandle repDef = configDetailsApiHandler.getReportDefSingleHandle(gridConfigView.getId());
                    return new ReportsTableData(gridConfigView, repDef);
                }).toIterable();
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull ReportsTableData entity, CyodaColumnHandle columnHandle) {
        if (REPORT_JSON_COLUMN.equals(columnHandle.getColumnName())) {
            return entity.config().json;
        }
        if (REPORT_COLUMNS_COLUMN.equals(columnHandle.getColumnName())) {
            return entity.config().columns;
        }

        Map<String, String> fields = entity.reportFields().getGridConfigFields();
        return fields.get(columnHandle.getColumnName());
    }
}
