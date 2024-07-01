package com.cyoda.connector.client.reporting.calls;

import com.cyoda.connector.auth.AuthContext;
import com.cyoda.connector.client.reporting.meta.ReportConfigKey;

public interface RunReportApi {
    String runReport(String queryId, AuthContext authContext, ReportConfigKey reportConfigKey);
}
