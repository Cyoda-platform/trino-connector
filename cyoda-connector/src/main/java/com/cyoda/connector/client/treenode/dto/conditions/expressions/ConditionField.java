package com.cyoda.connector.client.treenode.dto.conditions.expressions;

import com.cyoda.connector.handles.CyodaColumnHandle;

public class ConditionField {
    private final String fieldKey;
    private final CyodaColumnHandle.ColumnCategory fieldCategory;

    public ConditionField(String fieldKey, CyodaColumnHandle.ColumnCategory fieldCategory) {
        this.fieldKey = fieldKey;
        this.fieldCategory = fieldCategory;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public CyodaColumnHandle.ColumnCategory getFieldCategory() {
        return fieldCategory;
    }
}
