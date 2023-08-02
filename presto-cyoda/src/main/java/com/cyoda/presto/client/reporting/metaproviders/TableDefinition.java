package com.cyoda.presto.client.reporting.metaproviders;

import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.handles.CyodaTableType;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface TableDefinition {
    String getTableName();

    Collection<ColumnDefinition> getColumns();

    @Nonnull
    ColumnDefinition getColumn(String name);

    CyodaTableType getTableType();
}
