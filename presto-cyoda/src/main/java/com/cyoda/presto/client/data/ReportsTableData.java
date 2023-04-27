package com.cyoda.presto.client.data;

import com.cyoda.api.view.GridConfigFieldsView;
import com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle;

import java.util.Map;

public record ReportsTableData(Map<String, String> reportFields, ReportDefinitionHandle config) {

}
