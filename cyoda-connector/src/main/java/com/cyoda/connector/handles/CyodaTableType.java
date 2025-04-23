package com.cyoda.connector.handles;

import com.cyoda.connector.CyodaConfig;

import static com.cyoda.connector.handles.CyodaTableCategory.MAINTENANCE;
import static com.cyoda.connector.handles.CyodaTableCategory.REPORT;
import static com.cyoda.connector.handles.CyodaTableCategory.TREE_NODE;

public enum CyodaTableType {
    REPORTS(REPORT),
    STATS(REPORT),
    HISTORY(REPORT),
    GROUP(REPORT),
    DATA(REPORT),
    CALL_STATS(MAINTENANCE),
    PUSHDOWN_LOG(MAINTENANCE),
    CACHE_STATS(MAINTENANCE),
    CACHE_CONTENT(MAINTENANCE),
    LOG_TABLE(MAINTENANCE),
    TREE_NODE_TABLE(TREE_NODE),
    TDB_RAW_DATA(MAINTENANCE);

    private final CyodaTableCategory tableCategory;

    CyodaTableType(CyodaTableCategory tableCategory) {
        this.tableCategory = tableCategory;
    }
    public String getSchemaName(CyodaConfig cyodaConfig){
        return tableCategory.getSchemaName(cyodaConfig);
    }
    public CyodaTableCategory getTableCategory() {
        return tableCategory;
    }
    public boolean isPushdownSupported(){
        return this == TREE_NODE_TABLE;
    }
}

