package com.cyoda.connector.client.reporting.stats;

import java.util.Date;
import java.util.Map;

public record ApiRequestStats(
        String queryId,
        Date callTime,
        String requestUrl,
        String handlerName,
        long duration,
        Map<String, String> request,
        Object response
) {}

