package com.cyoda.presto.client.reporting.stats;

public record RowPageRequestStats(int page, int size, long rnPage, long rnSize, long callMillis) {
}
