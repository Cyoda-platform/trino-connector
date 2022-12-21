package com.cyoda.presto.client.data;

import com.cyoda.core.model.reports.DistributedReportInfoView;
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportStatisticsApiHandler;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import org.joda.beans.MetaProperty;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StatisticsTableDataProvider extends TableDataProvider<DistributedReportInfoView>{

    private final ConfiguredReportsApiHandler reportsApiHandler;
    private final ReportStatisticsApiHandler statisticsApiHandler;

    public StatisticsTableDataProvider(ConfiguredReportsApiHandler reportsApiHandler, ReportStatisticsApiHandler statisticsApiHandler) {
        this.reportsApiHandler = reportsApiHandler;
        this.statisticsApiHandler = statisticsApiHandler;
    }

    @Override
    public Iterable<DistributedReportInfoView> getIterable(AuthContext authContext, CyodaTableHandle tableHandle, CompoundPredicateNode predicates) {
        SizeListener listener = SizeListener.NOT_LISTENING;
        return reportsApiHandler.asFlux(authContext, listener)
                .flatMap(rep -> statisticsApiHandler.asFlux(rep.getId(), listener))
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
