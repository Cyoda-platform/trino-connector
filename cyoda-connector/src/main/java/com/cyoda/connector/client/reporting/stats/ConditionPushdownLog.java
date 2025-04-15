package com.cyoda.connector.client.reporting.stats;

import java.util.Date;
import java.util.Map;

public record ConditionPushdownLog(
        String queryId,
        Date callTime,
        String codePoint,
        String domainCondition,
        String expression,
        String acceptedCondition,
        String remainingCondition
) {}

