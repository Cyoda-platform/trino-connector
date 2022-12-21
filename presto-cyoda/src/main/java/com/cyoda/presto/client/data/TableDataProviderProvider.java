package com.cyoda.presto.client.data;

import com.cyoda.presto.client.reporting.data.ReportRowsApiHandler;
import com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler;
import com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportStatisticsApiHandler;
import com.cyoda.presto.handles.CyodaTableHandle;

import javax.inject.Inject;

public class TableDataProviderProvider {

    private final ReportsTableDataProvider reportsTableDataProvider;
    private final StatisticsTableDataProvider statisticsTableDataProvider;
    private final ReportHistoryApiHandler historyApiHandler;
    private final ReportGroupsApiHandler groupsApiHandler;
    private final ReportRowsApiHandler rowsApiHandler;
    @Inject
    public TableDataProviderProvider(ConfiguredReportsApiHandler reportsApiHandler,
                                     ReportConfigDetailsApiHandler configDetailsApiHandler,
                                     ReportStatisticsApiHandler statisticsApiHandler,
                                     ReportHistoryApiHandler historyApiHandler,
                                     ReportGroupsApiHandler groupsApiHandler,
                                     ReportRowsApiHandler rowsApiHandler) {
        reportsTableDataProvider = new ReportsTableDataProvider(reportsApiHandler, configDetailsApiHandler);
        statisticsTableDataProvider = new StatisticsTableDataProvider(reportsApiHandler, statisticsApiHandler);
        this.historyApiHandler = historyApiHandler;
        this.groupsApiHandler = groupsApiHandler;
        this.rowsApiHandler = rowsApiHandler;
    }

    public TableDataProvider<?> getDataProvider(CyodaTableHandle tableHandle){
        switch (tableHandle.getTableType()){
            case REPORTS -> {
                return reportsTableDataProvider;
            }
            case STATS -> {
                return statisticsTableDataProvider;
            }
            case HISTORY -> {
                return new HistoryTableDataProvider(tableHandle, historyApiHandler);
            }
            case GROUP -> {
                return new GroupsTableDataProvider(tableHandle, historyApiHandler, groupsApiHandler);
            }
            case DATA -> {
                return new DynamicTableDataProvider(tableHandle, historyApiHandler, groupsApiHandler, rowsApiHandler);
            }
            case DUMMY -> {
                return new DummyTableDataProvider();
            }
            default -> throw new RuntimeException("Unknown table type " + tableHandle.getTableType());
        }
    }


}
