package com.cyoda.presto.client.reporting.metaproviders;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import com.cyoda.presto.handles.CyodaTableType;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.type.TypeManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class TableMetadataProvider {
    protected final TypeManager typeManager;
    protected final CyodaConfig config;
    protected final CyodaConnectorId connectorId;

    public abstract List<CyodaTableHandle> listTables(AuthContext authContext, Optional<String> filterSchema);
    public abstract CyodaTableMeta getTableMeta(CyodaTableHandle tableHandle);

    public TableMetadataProvider(TypeManager typeManager, CyodaConfig config, CyodaConnectorId connectorId) {
        this.typeManager = typeManager;
        this.config = config;
        this.connectorId = connectorId;
    }

}
