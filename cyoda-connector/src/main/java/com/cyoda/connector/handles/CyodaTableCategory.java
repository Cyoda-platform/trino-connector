package com.cyoda.connector.handles;

import com.cyoda.connector.CyodaConfig;

import java.util.function.Function;

public enum CyodaTableCategory {
    REPORT(CyodaConfig::getReportingSchemaName),
    MAINTENANCE(CyodaConfig::getMaintenanceSchemaName),
    TREE_NODE(null);

    private Function<CyodaConfig, String> schemaNameGetter;
    CyodaTableCategory(Function<CyodaConfig, String> schemaNameGetter){
        this.schemaNameGetter = schemaNameGetter;
    }
    public String getSchemaName(CyodaConfig cyodaConfig){
        return schemaNameGetter.apply(cyodaConfig);
    }
}
