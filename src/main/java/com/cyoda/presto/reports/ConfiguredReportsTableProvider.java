package com.cyoda.presto.reports;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.CyodaTable;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.facebook.presto.common.type.DateType.DATE;
import static com.facebook.presto.common.type.VarcharType.createUnboundedVarcharType;
import static com.facebook.presto.type.UuidType.UUID;

public class ConfiguredReportsTableProvider implements ReportTableProvider{

    private static final String REPORT_DEFS_ENDPOINT = "/api/platform-api/reporting/definitions";

    private final CyodaConnectorId connectorId;
    private final CyodaConfig config;

    public ConfiguredReportsTableProvider(CyodaConnectorId connectorId, CyodaConfig config) {
        this.connectorId = connectorId;
        this.config = config;
    }

    @Nonnull
    public List<CyodaTable> createTableList() {
        // TODO: Replace this hard-coded prototype. The information should come from the Dist Reporting API component.
        ImmutableList.Builder<CyodaColumnHandle> builder = ImmutableList.builder();
        int pos = 0;
        builder.add(new CyodaColumnHandle(connectorId.toString(),"id", createUnboundedVarcharType(),pos++));
        builder.add(new CyodaColumnHandle(connectorId.toString(),"description", createUnboundedVarcharType(),pos++));
        builder.add(new CyodaColumnHandle(connectorId.toString(),"type", createUnboundedVarcharType(),pos++));
        builder.add(new CyodaColumnHandle(connectorId.toString(),"userId", UUID,pos++));
        builder.add(new CyodaColumnHandle(connectorId.toString(),"creationDate", DATE,pos++));

        String basePath = config.getServerUrl().toString();
        URI repDefUri = URI.create(basePath+REPORT_DEFS_ENDPOINT);
        List<URI> sources = Collections.singletonList(repDefUri);
        CyodaTable table = new CyodaTable(CyodaStaticReportTable.REPORTS.name(), builder.build(), sources, true);
        return Collections.singletonList(table);
    }
}
