package com.cyoda.connector.client.data;

import com.cyoda.connector.client.reporting.meta.ReportDefinitionHandle;

import java.util.Map;

public record ReportsTableData(Map<String, String> reportFields, ReportDefinitionHandle config, String error) {

}
