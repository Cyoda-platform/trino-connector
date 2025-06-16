package com.cyoda.connector.handles;

import com.cyoda.connector.CyodaConfig;
import com.cyoda.connector.CyodaConnectorFactory;

import java.util.function.Function;
import java.util.function.Predicate;

public enum CyodaTableCategory {
    REPORT(CyodaConfig::getReportingSchemaName, config -> config.getReportingSchemaName() != null),
    MAINTENANCE(config -> CyodaConnectorFactory.MAINTENANCE_SCHEMA_NAME, config -> true),
    TREE_NODE(config -> { throw new UnsupportedOperationException("getSchemaName is only supported for static tables");}, config -> true);

    private final Function<CyodaConfig, String> schemaNameGetter;
    private final Predicate<CyodaConfig> isCategoryEnabled;
    CyodaTableCategory(Function<CyodaConfig, String> schemaNameGetter, Predicate<CyodaConfig> isCategoryEnabled){
        this.schemaNameGetter = schemaNameGetter;
        this.isCategoryEnabled = isCategoryEnabled;
    }
    public String getSchemaName(CyodaConfig cyodaConfig){
        return schemaNameGetter.apply(cyodaConfig);
    }
    public boolean isEnabled(CyodaConfig cyodaConfig){
        return isCategoryEnabled.test(cyodaConfig);
    }
}
