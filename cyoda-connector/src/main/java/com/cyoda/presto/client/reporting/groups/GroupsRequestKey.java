package com.cyoda.presto.client.reporting.groups;

import java.util.UUID;

public record GroupsRequestKey(String reportId, UUID groupingVersion, String queryId) {

    @Override
    public int hashCode() {
        return reportId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return reportId.equals(((GroupsRequestKey) obj).reportId);
    }

    @Override
    public String toString() {
        return reportId;
    }
}
