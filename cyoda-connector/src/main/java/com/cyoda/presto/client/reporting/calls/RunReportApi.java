package com.cyoda.presto.client.reporting.calls;

import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.meta.ReportConfigKey;

public interface RunReportApi {
    String runReport(String queryId, AuthContext authContext, ReportConfigKey reportConfigKey);
}
