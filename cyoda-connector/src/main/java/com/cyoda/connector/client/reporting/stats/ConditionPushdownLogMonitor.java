package com.cyoda.connector.client.reporting.stats;

import com.cyoda.connector.CyodaConfig;
import com.cyoda.connector.logging.SupplierLogger;
import jakarta.inject.Inject;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ConditionPushdownLogMonitor extends BaseVirtualLogMonitor<ConditionPushdownLog> {


    @Inject
    public ConditionPushdownLogMonitor(CyodaConfig config){
        super(config);
    };

    public void registerPushdown(String queryId,
                                 String tableName,
                                 String codePoint,
                                 String domainCondition,
                                 String expression,
                                 String acceptedCondition,
                                 String additionalInfo) {
        add(new ConditionPushdownLog(
                queryId, tableName, new Date(), codePoint, domainCondition, expression, acceptedCondition, additionalInfo
        ));
    }

    @Override
    protected long getMaxRecords(CyodaConfig config) {
        return config.getPushdownLogMaxRecords();
    }

}
