package com.cyoda.presto.client.data;

import com.cyoda.core.model.reports.DistributedReportInfoView;
import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigKey;
import com.cyoda.presto.client.reporting.meta.ReportListKey;
import com.cyoda.presto.client.reporting.meta.ReportStatisticsApiHandler;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.connector.ConnectorSplitSource;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.FixedSplitSource;
import org.joda.beans.MetaProperty;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;

public class StatisticsTableDataProvider extends UnsplitTableDataProvider<DistributedReportInfoView>{

    private final ConfiguredReportsApiHandler reportsApiHandler;
    private final ReportStatisticsApiHandler statisticsApiHandler;

    public StatisticsTableDataProvider(ConfiguredReportsApiHandler reportsApiHandler, ReportStatisticsApiHandler statisticsApiHandler) {
        this.reportsApiHandler = reportsApiHandler;
        this.statisticsApiHandler = statisticsApiHandler;
    }

    @Override
    public Iterable<DistributedReportInfoView> getIterable(AuthContext authContext, CyodaTableHandle tableHandle, CyodaSplit split) {
        SizeListener listener = SizeListener.NOT_LISTENING;
        return reportsApiHandler.asFlux(new ReportListKey(authContext, split.getQueryId()), listener)
                .flatMap(rep -> statisticsApiHandler.asFlux(new ReportConfigKey(rep.getId(), split.getQueryId()), listener))
                .subscribeOn(Schedulers.parallel())
                .toIterable();
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull DistributedReportInfoView field, CyodaColumnHandle columnHandle) {
        MetaProperty<?> metaProperty = field.metaBean().metaPropertyMap().get(columnHandle.getColumnName());
        if (metaProperty == null) {
            throw new IllegalArgumentException(columnHandle.getColumnName() + " is not defined on ReportDefinitionsView");
        }
        return field.metaBean().metaProperty(columnHandle.getColumnName()).get(field);
    }
}
