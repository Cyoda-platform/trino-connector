package com.cyoda.presto;

import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableLayoutHandle;
import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.ColumnMetadata;
import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.ConnectorTableHandle;
import com.facebook.presto.spi.ConnectorTableLayout;
import com.facebook.presto.spi.ConnectorTableLayoutHandle;
import com.facebook.presto.spi.ConnectorTableLayoutResult;
import com.facebook.presto.spi.ConnectorTableMetadata;
import com.facebook.presto.spi.Constraint;
import com.facebook.presto.spi.SchemaTableName;
import com.facebook.presto.spi.SchemaTablePrefix;
import com.facebook.presto.spi.TableNotFoundException;
import com.facebook.presto.spi.connector.ConnectorMetadata;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class CyodaMetadata implements ConnectorMetadata {

    private final String connectorId;
    private final CyodaClient client;

    @Inject
    public CyodaMetadata(
            CyodaConnectorId connectorId,
            CyodaClient client)
    {
        this.connectorId = requireNonNull(connectorId, "connectorId is null").toString();
        this.client = requireNonNull(client, "client is null");
    }


    @Override
    public List<String> listSchemaNames(ConnectorSession session) {
        return ImmutableList.of(client.getSchemaName());
    }

    @Override
    public ConnectorTableHandle getTableHandle(ConnectorSession session, SchemaTableName tableName) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public List<ConnectorTableLayoutResult> getTableLayouts(ConnectorSession session, ConnectorTableHandle table, Constraint<ColumnHandle> constraint, Optional<Set<ColumnHandle>> desiredColumns) {
        CyodaTableHandle tableHandle = (CyodaTableHandle) table;
        ConnectorTableLayout layout = new ConnectorTableLayout(new CyodaTableLayoutHandle(tableHandle, constraint.getSummary()));
        return ImmutableList.of(new ConnectorTableLayoutResult(layout, constraint.getSummary()));    }

    @Override
    public ConnectorTableLayout getTableLayout(ConnectorSession session, ConnectorTableLayoutHandle handle) {
        return new ConnectorTableLayout(handle);
    }


    private ConnectorTableMetadata getTableMetadata(SchemaTableName tableName)
    {
        if (!client.getSchemaName().contains(tableName.getSchemaName())) {
            return null;
        }

        CyodaTable table = client.getTable(tableName);
        return new ConnectorTableMetadata(tableName, table.getColumnsMetadata());
    }

    @Override
    public ConnectorTableMetadata getTableMetadata(ConnectorSession session, ConnectorTableHandle table) {
        CyodaTableHandle handle = (CyodaTableHandle) table;
        checkArgument(handle.getConnectorId().equals(connectorId), "table is not for this connector");
        SchemaTableName tableName = new SchemaTableName(handle.getSchemaName(), handle.getTableName());
        ConnectorTableMetadata metadata = getTableMetadata(tableName);
        if (metadata == null) {
            throw new TableNotFoundException(tableName);
        }
        return metadata;    }

    @Override
    public Map<String, ColumnHandle> getColumnHandles(ConnectorSession session, ConnectorTableHandle tableHandle) {
        CyodaTableHandle handle = (CyodaTableHandle) tableHandle;
        checkArgument(handle.getConnectorId().equals(connectorId), "tableHandle is not for this connector");

        CyodaTable table = client.getTable(handle.toSchemaTableName());
        if (table == null) {
            throw new TableNotFoundException(handle.toSchemaTableName());
        }

        ImmutableMap.Builder<String, ColumnHandle> columnHandles = ImmutableMap.builder();
        for (CyodaColumnHandle column : table.getColumns()) {
            columnHandles.put(column.getColumnName(), column);
        }
        return columnHandles.build();
    }

    @Override
    public ColumnMetadata getColumnMetadata(ConnectorSession session, ConnectorTableHandle tableHandle, ColumnHandle columnHandle) {
        return ((CyodaColumnHandle) columnHandle).getColumnMetadata();
    }

    @Override
    public List<SchemaTableName> listTables(ConnectorSession session, Optional<String> filterSchema)
    {
        ImmutableList.Builder<SchemaTableName> builder = ImmutableList.builder();
        for (String tableName : client.getTableNames()) {
            builder.add(new SchemaTableName(client.getSchemaName(), tableName));
        }
        return builder.build();
    }

    private List<SchemaTableName> listTables(ConnectorSession session, SchemaTablePrefix prefix)
    {
        // List all tables if schema or table is null
        if (prefix.getSchemaName() == null || prefix.getTableName() == null) {
            return listTables(session, Optional.ofNullable(prefix.getSchemaName()));
        }

        // Make sure requested table exists, returning the single table of it does
        SchemaTableName table = new SchemaTableName(prefix.getSchemaName(), prefix.getTableName());
        if (getTableHandle(session, table) != null) {
            return ImmutableList.of(table);
        }

        // Else, return empty list
        return ImmutableList.of();
    }

    @Override
    public Map<SchemaTableName, List<ColumnMetadata>> listTableColumns(ConnectorSession session, SchemaTablePrefix prefix)
    {
        requireNonNull(prefix, "prefix is null");
        ImmutableMap.Builder<SchemaTableName, List<ColumnMetadata>> columns = ImmutableMap.builder();
        for (SchemaTableName tableName : listTables(session, prefix)) {
            ConnectorTableMetadata tableMetadata = getTableMetadata(tableName);
            // table can disappear during listing operation
            if (tableMetadata != null) {
                columns.put(tableName, tableMetadata.getColumns());
            }
        }
        return columns.build();
    }

}
