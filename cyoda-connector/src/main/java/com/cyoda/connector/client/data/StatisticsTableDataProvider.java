package com.cyoda.connector.client.data;

import com.cyoda.connector.auth.AuthContext;
import com.cyoda.connector.client.reporting.meta.ReportListKey;
import com.cyoda.connector.handles.CyodaTableHandle;
import com.cyoda.core.reports.DistributedReportInfoDto;
import com.cyoda.connector.CyodaSplit;
import com.cyoda.connector.SizeListener;
import com.cyoda.connector.client.reporting.meta.ConfiguredReportsApi;
import com.cyoda.connector.client.reporting.meta.ReportConfigKey;
import com.cyoda.connector.client.reporting.meta.ReportStatisticsApi;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.handles.CyodaTableMeta;
import io.trino.spi.connector.Constraint;
import org.joda.beans.MetaProperty;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class StatisticsTableDataProvider extends TableDataProvider<DistributedReportInfoDto> {

    protected final ConfiguredReportsApi reportsApiHandler;
    private final ReportStatisticsApi statisticsApiHandler;

    public StatisticsTableDataProvider(ConfiguredReportsApi reportsApiHandler, ReportStatisticsApi statisticsApiHandler) {
        this.reportsApiHandler = reportsApiHandler;
        this.statisticsApiHandler = statisticsApiHandler;
    }

    @Override
    public Iterable<DistributedReportInfoDto> getIterable(CyodaTableMeta tableHandle, CyodaSplit split) {
        SizeListener listener = SizeListener.NOT_LISTENING;
        return Objects.requireNonNull(reportsApiHandler.asFlux(new ReportListKey(split.getUserId(), split.getQueryId()), listener)
                .subscribeOn(Schedulers.parallel())
                .collectList()
                .block(), "Failed to load report configurations").stream().flatMap(gcfv ->
                    statisticsApiHandler.asFlux(new ReportConfigKey(gcfv.getId(), split.getQueryId()), listener)
                            .subscribeOn(Schedulers.parallel())
                            .toStream()
                ).toList();

    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull DistributedReportInfoDto field, CyodaColumnHandle columnHandle) {
        MetaProperty<?> metaProperty = field.metaBean().metaPropertyMap().get(columnHandle.getColumnName());
        if (metaProperty == null) {
            throw new IllegalArgumentException(columnHandle.getColumnName() + " is not defined on ReportDefinitionsView");
        }
        return field.metaBean().metaProperty(columnHandle.getColumnName()).get(field);
    }

    @Override
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        return Collections.singletonList(new CyodaSplit(queryId, authContext.getUserId(), tableHandle));
    }
}
