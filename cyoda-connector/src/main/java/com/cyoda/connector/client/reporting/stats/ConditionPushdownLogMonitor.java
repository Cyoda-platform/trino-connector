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
                                 String codePoint,
                                 String domainCondition,
                                 String expression,
                                 String acceptedCondition,
                                 String remainingCondition) {
        add(new ConditionPushdownLog(
                queryId, new Date(), codePoint, domainCondition, expression, acceptedCondition, remainingCondition
        ));
    }

    @Override
    protected long getMaxRecords(CyodaConfig config) {
        return config.getPushdownLogMaxRecords();
    }

}
