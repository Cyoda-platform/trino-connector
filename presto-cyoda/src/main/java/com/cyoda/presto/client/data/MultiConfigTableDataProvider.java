package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportListKey;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.connector.Constraint;
import reactor.core.scheduler.Schedulers;

import java.util.List;

public abstract class MultiConfigTableDataProvider<T> extends TableDataProvider<T> {
    protected final ConfiguredReportsApiHandler reportsApiHandler;

    public MultiConfigTableDataProvider(ConfiguredReportsApiHandler reportsApiHandler) {
        this.reportsApiHandler = reportsApiHandler;
    }

    @Override
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        SizeListener listener = SizeListener.NOT_LISTENING;
        return reportsApiHandler.asFlux(new ReportListKey(authContext, queryId), listener)
                .map(confView -> CyodaSplit.configSplit(confView.getId(), authContext.getUserId(), queryId))
                .subscribeOn(Schedulers.parallel())
                .collectList()
                .block();
    }
}
