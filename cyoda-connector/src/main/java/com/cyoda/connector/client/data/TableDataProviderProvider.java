package com.cyoda.connector.client.data;

import com.cyoda.connector.CyodaConfig;
import com.cyoda.connector.client.reporting.data.ReportRowsApi;
import com.cyoda.connector.client.reporting.groups.ReportGroupsApi;
import com.cyoda.connector.client.reporting.meta.ConfiguredReportsApi;
import com.cyoda.connector.client.reporting.meta.ReportConfigDetailsApi;
import com.cyoda.connector.client.reporting.meta.ReportHistoryApi;
import com.cyoda.connector.client.reporting.meta.ReportStatisticsApi;
import com.cyoda.connector.client.reporting.metaproviders.StaticTableMetadataProvider;
import com.cyoda.connector.client.reporting.stats.ConditionPushdownLogMonitor;
import com.cyoda.connector.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.connector.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.connector.client.treenode.CyodaRSocketClient;
import com.cyoda.connector.handles.CyodaTableType;
import io.trino.spi.NodeManager;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.cyoda.connector.handles.CyodaTableType.*;

public class TableDataProviderProvider {

    private final Map<CyodaTableType, TableDataProvider<?>> providerMap;

    private static <T extends TableDataProvider<?>> void putToMap(Map<CyodaTableType, TableDataProvider<?>> providerMap,
                                                                  CyodaConfig config,
                                                                  CyodaTableType tableType,
                                                                  Supplier<T> supplier)
    {
        if (tableType.isEnabled(config)) {
            providerMap.put(tableType, supplier.get());
        }
    }
    @Inject
    public TableDataProviderProvider(ConfiguredReportsApi reportsApiHandler,
                                     ReportConfigDetailsApi configDetailsApiHandler,
                                     ReportStatisticsApi statisticsApiHandler,
                                     ReportHistoryApi historyApiHandler,
                                     ReportGroupsApi groupsApiHandler,
                                     ReportRowsApi rowsApiHandler,
                                     StaticTableMetadataProvider reportMetadataProvider,
                                     CyodaApiRequestStatsMonitor statsMonitor,
                                     ConditionPushdownLogMonitor pushdownLogMonitor,
                                     CyodaConfig config,
                                     NodeManager nodeManager,
                                     CyodaCacheMonitor cyodaCacheMonitor,
                                     CyodaRSocketClient treeNodeAPIClient) {
        providerMap = new HashMap<>();
        putToMap(providerMap, config, REPORTS,() -> new ReportsTableDataProvider(reportsApiHandler, configDetailsApiHandler));
        putToMap(providerMap, config, REPORTS,() -> new ReportsTableDataProvider(reportsApiHandler, configDetailsApiHandler));
        putToMap(providerMap, config, STATS,() -> new StatisticsTableDataProvider(reportsApiHandler, statisticsApiHandler));
        putToMap(providerMap, config, HISTORY,() -> new HistoryTableDataProvider(historyApiHandler));
        putToMap(providerMap, config, GROUP,() -> new GroupsTableDataProvider(historyApiHandler, groupsApiHandler, reportMetadataProvider));
        putToMap(providerMap, config, DATA,() ->
                new DynamicTableDataProvider(historyApiHandler, groupsApiHandler, rowsApiHandler, reportMetadataProvider, config, cyodaCacheMonitor));
        putToMap(providerMap, config, CALL_STATS,() -> new ApiCallStatsDataProvider(statsMonitor, nodeManager));
        putToMap(providerMap, config, PUSHDOWN_LOG,() -> new ConditionPushdownDataProvider(pushdownLogMonitor, nodeManager));
        putToMap(providerMap, config, CACHE_STATS,() -> new CacheStatsDataProvider(nodeManager, cyodaCacheMonitor));
        putToMap(providerMap, config, CACHE_CONTENT,() -> new CacheContentDataProvider(nodeManager, cyodaCacheMonitor));
        putToMap(providerMap, config, LOG_TABLE,() -> new LogTableDataProvider(nodeManager));
        putToMap(providerMap, config, TREE_NODE_TABLE,() -> new TreeNodeTableDataProvider(treeNodeAPIClient));
        putToMap(providerMap, config, TDB_RAW_DATA,() -> new RawTreeNodeDataProvider(treeNodeAPIClient));
    }

    public TableDataProvider<?> getDataProvider(CyodaTableType tableType){
        return Optional.ofNullable(providerMap.get(tableType)).orElseThrow(() ->
            new RuntimeException("Unknown table type " + tableType));
    }


}
