package com.cyoda.presto;

import com.cyoda.presto.http.QueryRunner;
import com.cyoda.presto.reports.ConfiguredReportsTableProvider;
import com.cyoda.presto.reports.CyodaStaticReportTable;
import com.cyoda.presto.reports.ReportTableProvider;
import com.facebook.presto.spi.SchemaTableName;
import com.google.common.collect.ImmutableMap;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("UnstableApiUsage")
public class CyodaClient {
    // For now, it's a static schema name.
    private static final String CYODA_SCHEMA_NAME = "cyoda";

    /**
     * TableName -> TableMetadata
     */
    private final Supplier<Map<String, CyodaTable>> tables;


    private final QueryRunner queryRunner;
    private final CyodaConfig config;
    private final CyodaConnectorId connectorId;


    @Inject
    public CyodaClient(CyodaConnectorId id, CyodaConfig config, @ForCyodaClient QueryRunner queryRunner)
    {
        this.connectorId = requireNonNull(id, "connectorId is null");
        this.config = requireNonNull(config, "config is null");
        this.queryRunner = requireNonNull(queryRunner, "queryRunner is null");

        tables = this::lookupSchema;
    }

    private Map<String, CyodaTable> lookupSchema() {

        ImmutableMap.Builder<String, ReportTableProvider> repTableBuilder = ImmutableMap.builder();
        repTableBuilder.put(CyodaStaticReportTable.REPORTS.name(),new ConfiguredReportsTableProvider(connectorId,config));
        // TODO: Add the others
        ImmutableMap<String, ReportTableProvider> repTableProviders = repTableBuilder.build();

        ImmutableMap.Builder<String, CyodaTable> builder = ImmutableMap.builder();
        repTableProviders.values().stream()
                .flatMap(r -> r.createTableList().stream())
                .forEach(v -> builder.put(v.getName(),v));

        return builder.build();
    }


    public Set<String> getTableNames()
    {
        return tables.get().keySet();
    }

    public CyodaTable getTable(SchemaTableName tableName) {
        requireNonNull(tableName, "tableName is null");
        Map<String, CyodaTable> tableMap = tables.get();
        if (tableMap == null) {
            return null;
        }
        return tableMap.get(tableName.getTableName());

    }

    public String getSchemaName() {
        return CYODA_SCHEMA_NAME;
    }

    public CyodaTable getTable(String schemaName, String tableName) {
        return getTable(new SchemaTableName(schemaName,tableName));
    }

    public QueryRunner getQueryRunner() {
        return queryRunner;
    }

    public enum CyodaAuthenticationType
    {
        NONE,
        BASIC,
        JWT
    }


}
