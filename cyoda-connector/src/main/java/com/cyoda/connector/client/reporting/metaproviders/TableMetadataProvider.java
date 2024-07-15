package com.cyoda.connector.client.reporting.metaproviders;

import com.cyoda.connector.CyodaConfig;
import com.cyoda.connector.auth.AuthContext;
import com.cyoda.connector.handles.CyodaTableHandle;
import com.cyoda.connector.handles.CyodaTableMeta;
import io.trino.spi.type.TypeManager;

import java.util.List;

public abstract class TableMetadataProvider {
    protected final TypeManager typeManager;
    protected final CyodaConfig config;

    public abstract List<CyodaTableHandle> listTables(AuthContext authContext);
    public abstract CyodaTableMeta getTableMeta(CyodaTableHandle tableHandle);

    public TableMetadataProvider(TypeManager typeManager, CyodaConfig config) {
        this.typeManager = typeManager;
        this.config = config;
    }

}
