package com.cyoda.connector.client.reporting.calls;

import io.trino.spi.connector.ConnectorSession;

public interface DeleteReportsApi {
    String deleteReports(ConnectorSession session, String configId);
}
