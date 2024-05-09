package com.cyoda.presto.client.data;

import com.cyoda.core.reports.DistributedReportInfoDto;
import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.client.reporting.meta.ConfiguredReportsApi;
import com.cyoda.presto.client.reporting.meta.ReportConfigKey;
import com.cyoda.presto.client.reporting.meta.ReportStatisticsApi;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import org.joda.beans.MetaProperty;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StatisticsTableDataProvider extends MultiConfigTableDataProvider<DistributedReportInfoDto> {

    private final ReportStatisticsApi statisticsApiHandler;

    public StatisticsTableDataProvider(ConfiguredReportsApi reportsApiHandler, ReportStatisticsApi statisticsApiHandler) {
        super(reportsApiHandler);
        this.statisticsApiHandler = statisticsApiHandler;
    }

    @Override
    public Iterable<DistributedReportInfoDto> getIterable(CyodaTableMeta tableHandle, CyodaSplit split) {
        SizeListener listener = SizeListener.NOT_LISTENING;
        return statisticsApiHandler.asFlux(new ReportConfigKey(split.getCyodaTableMetaId(), split.getQueryId()), listener)
                .subscribeOn(Schedulers.parallel())
                .toIterable();
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
}
