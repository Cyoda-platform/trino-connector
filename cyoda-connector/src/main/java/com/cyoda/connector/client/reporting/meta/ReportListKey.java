package com.cyoda.connector.client.reporting.meta;

import com.cyoda.connector.auth.AuthContext;

import java.util.Objects;

public final class ReportListKey {
    private final AuthContext authContext;
    private final String queryId;

    public ReportListKey(AuthContext authContext, String queryId) {
        this.authContext = authContext;
        this.queryId = queryId;
    }

    public AuthContext authContext() {
        return authContext;
    }

    public String queryId() {
        return queryId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ReportListKey) obj;
        return Objects.equals(this.authContext, that.authContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authContext);
    }

    @Override
    public String toString() {
        return "ReportListKey[" +
                "authContext=" + authContext + ", " +
                "queryId=" + queryId + ']';
    }

}
