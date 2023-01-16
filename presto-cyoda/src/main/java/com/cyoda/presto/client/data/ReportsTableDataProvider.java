package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigKey;
import com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle;
import com.cyoda.presto.client.reporting.meta.ReportListKey;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.connector.ConnectorSplitSource;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.FixedSplitSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_COLUMNS_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_JSON_COLUMN;

public class ReportsTableDataProvider extends UnsplitTableDataProvider<ReportsTableData>{

    private final ConfiguredReportsApiHandler reportsApiHandler;
    private final ReportConfigDetailsApiHandler configDetailsApiHandler;

    public ReportsTableDataProvider(ConfiguredReportsApiHandler reportsApiHandler, ReportConfigDetailsApiHandler configDetailsApiHandler) {
        this.reportsApiHandler = reportsApiHandler;
        this.configDetailsApiHandler = configDetailsApiHandler;
    }

    @Override
    public Iterable<ReportsTableData> getIterable(AuthContext authContext, CyodaTableHandle tableHandle, CyodaSplit split) {
        return reportsApiHandler.asFlux(new ReportListKey(authContext, split.getQueryId()), SizeListener.NOT_LISTENING)
                .map(gridConfigView -> {
                    ReportDefinitionHandle repDef = configDetailsApiHandler.getReportDefSingleHandle(
                            new ReportConfigKey(gridConfigView.getId(), split.getQueryId()));
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
