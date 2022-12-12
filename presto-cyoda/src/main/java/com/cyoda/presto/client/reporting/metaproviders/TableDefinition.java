package com.cyoda.presto.client.reporting.metaproviders;

import com.cyoda.presto.client.reporting.ColumnDefinition;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface TableDefinition {
    String getTableName();

    String getEndpoint();

    Collection<ColumnDefinition> getColumns();

    @Nonnull
    ColumnDefinition getColumn(String name);

    String getRequestHandlerKey();
}
