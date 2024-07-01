package com.cyoda.connector.client.data;

import com.cyoda.connector.CyodaSplit;
import com.cyoda.connector.SizeListener;
import com.cyoda.connector.auth.AuthContext;
import com.cyoda.connector.client.reporting.meta.ConfiguredReportsApi;
import com.cyoda.connector.client.reporting.meta.ReportListKey;
import com.cyoda.connector.handles.CyodaTableHandle;
import io.trino.spi.connector.Constraint;
import reactor.core.scheduler.Schedulers;

import java.util.List;

public abstract class MultiConfigTableDataProvider<T> extends TableDataProvider<T> {
    protected final ConfiguredReportsApi reportsApiHandler;

    public MultiConfigTableDataProvider(ConfiguredReportsApi reportsApiHandler) {
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
