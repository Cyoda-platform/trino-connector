package com.cyoda.connector.client.reporting.meta;

import com.cyoda.connector.auth.AuthContext;

import java.util.Objects;

public final class ReportListKey {
    private final String queryId;
    private final String userId;

    public ReportListKey(String userId, String queryId) {
        this.userId = userId;
        this.queryId = queryId;
    }

    public String getUserId() {
        return userId;
    }

    public String queryId() {
        return queryId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ReportListKey) obj;
        return Objects.equals(this.userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "ReportListKey[" +
                "userId=" + userId + ", " +
                "queryId=" + queryId + ']';
    }

}
