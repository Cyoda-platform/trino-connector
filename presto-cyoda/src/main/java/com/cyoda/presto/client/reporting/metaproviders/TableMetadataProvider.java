package com.cyoda.presto.client.reporting.metaproviders;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import io.trino.spi.type.TypeManager;

import java.util.List;

public abstract class TableMetadataProvider {
    protected final TypeManager typeManager;
    protected final CyodaConfig config;
    protected final CyodaConnectorId connectorId;

    public abstract List<CyodaTableHandle> listTables(AuthContext authContext);
    public abstract CyodaTableMeta getTableMeta(CyodaTableHandle tableHandle);

    public TableMetadataProvider(TypeManager typeManager, CyodaConfig config, CyodaConnectorId connectorId) {
        this.typeManager = typeManager;
        this.config = config;
        this.connectorId = connectorId;
    }

}
