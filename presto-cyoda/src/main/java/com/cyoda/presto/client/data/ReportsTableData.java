package com.cyoda.presto.client.data;

import com.cyoda.api.view.GridConfigFieldsView;
import com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle;

public record ReportsTableData(GridConfigFieldsView reportFields, ReportDefinitionHandle config) {

}
