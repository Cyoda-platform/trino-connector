package com.cyoda.connector.client.reporting.meta;

import java.util.Objects;

public final class ReportConfigKey {
    private final String configId;
    private final String queryId;

    public ReportConfigKey(String configId, String queryId) {
        this.configId = configId;
        this.queryId = queryId;
    }

    public String configId() {
        return configId;
    }

    public String queryId() {
        return queryId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ReportConfigKey) obj;
        return Objects.equals(this.configId, that.configId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configId);
    }

    @Override
    public String toString() {
        return "ReportConfigKey[" +
                "configId=" + configId + ", " +
                "queryId=" + queryId + ']';
    }

}
