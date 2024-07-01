package com.cyoda.connector.client.reporting.meta;

import com.cyoda.api.view.GridConfigFieldsView;
import com.cyoda.connector.client.reporting.FluxApiHandler;
import io.trino.spi.connector.SchemaTableName;

import javax.annotation.Nonnull;

import static com.cyoda.connector.client.reporting.meta.ReportDefinitionHandle.REPORT_SCHEMA_NAME_COLUMN;
import static com.cyoda.connector.client.reporting.meta.ReportDefinitionHandle.REPORT_TABLE_NAME_COLUMN;

public interface ConfiguredReportsApi extends FluxApiHandler<ReportListKey, GridConfigFieldsView> {

    static void addSchemaTableName(GridConfigFieldsView it) {
        String id = it.getId();
        SchemaTableName tableName = configIdToSchemaTableName(id);
        it.addField(REPORT_SCHEMA_NAME_COLUMN, tableName.getSchemaName());
        it.addField(REPORT_TABLE_NAME_COLUMN, tableName.getTableName());
    }

    static @Nonnull SchemaTableName configIdToSchemaTableName(@Nonnull String reportConfigId){
        int ownerIndex = reportConfigId.indexOf("-");
        if (ownerIndex == -1) {
            return new SchemaTableName("reports_owner_unknown", reportConfigId.toLowerCase());
        } else {
            return new SchemaTableName("reports_" + reportConfigId.substring(0, ownerIndex).toLowerCase(),
                    reportConfigId.substring(ownerIndex + 1));
        }
    }
}
