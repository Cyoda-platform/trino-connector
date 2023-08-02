package com.cyoda.presto.client.reporting.metaproviders;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import io.trino.spi.type.TypeManager;

import java.net.URI;
import java.net.URISyntaxException;

public abstract class TableMetadataProvider {
    protected final TypeManager typeManager;
    protected final CyodaConfig config;
    protected final CyodaConnectorId connectorId;

    public TableMetadataProvider(TypeManager typeManager, CyodaConfig config, CyodaConnectorId connectorId) {
        this.typeManager = typeManager;
        this.config = config;
        this.connectorId = connectorId;
    }

}
