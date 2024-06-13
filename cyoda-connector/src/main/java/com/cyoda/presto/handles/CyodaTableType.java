package com.cyoda.presto.handles;

public enum CyodaTableType {
    REPORTS,
    STATS,
    HISTORY,
    GROUP,
    DATA,
    CALL_STATS,
    CACHE_STATS,
    CACHE_CONTENT,
    LOG_TABLE,
    TREE_NODE_TABLE;

    public boolean isPushdownSupported(){
        return this == TREE_NODE_TABLE;
    }
}
