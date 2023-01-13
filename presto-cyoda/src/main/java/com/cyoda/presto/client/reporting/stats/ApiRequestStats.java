package com.cyoda.presto.client.reporting.stats;

import com.google.common.base.MoreObjects;

import java.util.Date;

public record ApiRequestStats(String queryId, Date callTime, String requestUrl, long duration, Object response) {
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("queryId", queryId)
                .add("callTime", callTime)
                .add("duration(ms)", duration)
                .add("requestUrl", requestUrl)
                .add("response", response).toString();
    }
}

