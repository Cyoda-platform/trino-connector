package com.cyoda.connector.client.data;

import com.cyoda.connector.CyodaConfig;
import com.cyoda.connector.client.reporting.data.ReportRowsApi;
import com.cyoda.connector.client.reporting.groups.ReportGroupsApi;
import com.cyoda.connector.client.reporting.meta.ConfiguredReportsApi;
import com.cyoda.connector.client.reporting.meta.ReportConfigDetailsApi;
import com.cyoda.connector.client.reporting.meta.ReportHistoryApi;
import com.cyoda.connector.client.reporting.meta.ReportStatisticsApi;
import com.cyoda.connector.client.reporting.metaproviders.StaticTableMetadataProvider;
import com.cyoda.connector.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.connector.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.connector.client.treenode.CyodaRSocketClient;
import com.cyoda.connector.handles.CyodaTableType;
import io.trino.spi.NodeManager;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.cyoda.connector.handles.CyodaTableType.*;

public class TableDataProviderProvider {

    private final Map<CyodaTableType, TableDataProvider<?>> providerMap;
    @Inject
    public TableDataProviderProvider(ConfiguredReportsApi reportsApiHandler,
                                     ReportConfigDetailsApi configDetailsApiHandler,
                                     ReportStatisticsApi statisticsApiHandler,
                                     ReportHistoryApi historyApiHandler,
                                     ReportGroupsApi groupsApiHandler,
                                     ReportRowsApi rowsApiHandler,
                                     StaticTableMetadataProvider reportMetadataProvider,
                                     CyodaApiRequestStatsMonitor statsMonitor,
                                     CyodaConfig config,
                                     NodeManager nodeManager,
                                     CyodaCacheMonitor cyodaCacheMonitor,
                                     CyodaRSocketClient treeNodeAPIClient) {
        providerMap = new HashMap<>();
        providerMap.put(
                REPORTS,
                new ReportsTableDataProvider(reportsApiHandler, configDetailsApiHandler)
        );
        providerMap.put(
                STATS,
                new StatisticsTableDataProvider(reportsApiHandler, statisticsApiHandler)
        );
        providerMap.put(
                HISTORY,
                new HistoryTableDataProvider(historyApiHandler)
        );
        providerMap.put(
                GROUP,
                new GroupsTableDataProvider(historyApiHandler, groupsApiHandler, reportMetadataProvider)
        );
        providerMap.put(
                DATA,
                new DynamicTableDataProvider(historyApiHandler,
                        groupsApiHandler,
                        rowsApiHandler,
                        reportMetadataProvider,
                        config,
                        cyodaCacheMonitor)
        );
        providerMap.put(
                CALL_STATS,
                new ApiCallStatsDataProvider(statsMonitor, nodeManager)
        );
        providerMap.put(
                CACHE_STATS,
                new CacheStatsDataProvider(nodeManager, cyodaCacheMonitor)
        );
        providerMap.put(
                CACHE_CONTENT,
                new CacheContentDataProvider(nodeManager, cyodaCacheMonitor)
        );
        providerMap.put(
                LOG_TABLE,
                new LogTableDataProvider(nodeManager)
        );
        providerMap.put(
                TREE_NODE_TABLE,
                new TreeNodeTableDataProvider(treeNodeAPIClient)
        );
    }

    public TableDataProvider<?> getDataProvider(CyodaTableType tableType){
        return Optional.ofNullable(providerMap.get(tableType)).orElseThrow(() ->
            new RuntimeException("Unknown table type " + tableType));
    }


}
