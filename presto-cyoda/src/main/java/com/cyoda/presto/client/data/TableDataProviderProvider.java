package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.client.reporting.data.ReportRowsApiHandler;
import com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler;
import com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportStatisticsApiHandler;
import com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadataProvider;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.handles.CyodaTableHandle;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TableDataProviderProvider {

    private final Map<CyodaTableHandle.TableType, TableDataProvider<?>> providerMap;
    @Inject
    public TableDataProviderProvider(ConfiguredReportsApiHandler reportsApiHandler,
                                     ReportConfigDetailsApiHandler configDetailsApiHandler,
                                     ReportStatisticsApiHandler statisticsApiHandler,
                                     ReportHistoryApiHandler historyApiHandler,
                                     ReportGroupsApiHandler groupsApiHandler,
                                     ReportRowsApiHandler rowsApiHandler,
                                     StaticTableMetadataProvider reportMetadataProvider,
                                     CyodaApiRequestStatsMonitor statsMonitor,
                                     CyodaConfig config) {
        providerMap = new HashMap<>();
        providerMap.put(
                CyodaTableHandle.TableType.REPORTS,
                new ReportsTableDataProvider(reportsApiHandler, configDetailsApiHandler)
        );
        providerMap.put(
                CyodaTableHandle.TableType.STATS,
                new StatisticsTableDataProvider(reportsApiHandler, statisticsApiHandler)
        );
        providerMap.put(
                CyodaTableHandle.TableType.HISTORY,
                new HistoryTableDataProvider(historyApiHandler)
        );
        providerMap.put(
                CyodaTableHandle.TableType.GROUP,
                new GroupsTableDataProvider(historyApiHandler, groupsApiHandler, reportMetadataProvider)
        );
        providerMap.put(
                CyodaTableHandle.TableType.DATA,
                new DynamicTableDataProvider(historyApiHandler, groupsApiHandler, rowsApiHandler, reportMetadataProvider, config)
        );
        providerMap.put(
                CyodaTableHandle.TableType.DUMMY,
                new DummyTableDataProvider()
        );
        providerMap.put(
                CyodaTableHandle.TableType.CALL_STATS,
                new ApiCallStatsDataProvider(statsMonitor)
        );
    }

    public TableDataProvider<?> getDataProvider(CyodaTableHandle.TableType tableType){
        return Optional.ofNullable(providerMap.get(tableType)).orElseThrow(() ->
            new RuntimeException("Unknown table type " + tableType));
    }


}
