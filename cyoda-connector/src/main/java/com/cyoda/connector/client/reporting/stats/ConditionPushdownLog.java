package com.cyoda.connector.client.reporting.stats;

import java.util.Date;

public record ConditionPushdownLog(
        String queryId,
        String tableName,
        Date callTime,
        String codePoint,
        String domainCondition,
        String expression,
        String acceptedCondition,
        String additionalInfo
) {}

