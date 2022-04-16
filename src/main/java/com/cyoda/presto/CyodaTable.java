package com.cyoda.presto;

import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.spi.ColumnMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import java.net.URI;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

public class CyodaTable {
    private final String name;
    private final List<CyodaColumnHandle> columns;
    private final List<ColumnMetadata> columnsMetadata;

    //TODO: This might need to be something pulled from Reporting API.
    private final List<URI> sources;

    private final Boolean isPaged;

    @JsonCreator
    public CyodaTable(
            @JsonProperty("name") String name,
            @JsonProperty("columns") List<CyodaColumnHandle> columns,
            @JsonProperty("sources") List<URI> sources,
            @JsonProperty("isPaged") Boolean isPaged)
    {
        checkArgument(!isNullOrEmpty(name), "name is null or is empty");
        this.name = requireNonNull(name, "name is null");
        this.isPaged = requireNonNull(isPaged, "isPaged is null");

        this.columns = ImmutableList.copyOf(requireNonNull(columns, "columns is null"));
        this.sources = ImmutableList.copyOf(requireNonNull(sources, "sources is null"));

        ImmutableList.Builder<ColumnMetadata> thisMeta = ImmutableList.builder();
        for (CyodaColumnHandle column : this.columns) {
            thisMeta.add(new ColumnMetadata(column.getColumnName(), column.getColumnType()));
        }
        this.columnsMetadata = thisMeta.build();
    }

    @JsonProperty
    public String getName()
    {
        return name;
    }

    @JsonProperty
    public List<CyodaColumnHandle> getColumns()
    {
        return columns;
    }

    @JsonProperty
    public List<URI> getSources()
    {
        return sources;
    }

    @JsonProperty
    public Boolean isPaged() {
        return isPaged;
    }

    public List<ColumnMetadata> getColumnsMetadata()
    {
        return columnsMetadata;
    }

}
