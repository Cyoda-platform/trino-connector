package com.cyoda.presto.client.reporting.metaproviders;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.type.TypeManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public abstract class TableMetadataProvider {
    protected final TypeManager typeManager;
    protected final CyodaConfig config;
    protected final CyodaConnectorId connectorId;

    public TableMetadataProvider(TypeManager typeManager, CyodaConfig config, CyodaConnectorId connectorId) {
        this.typeManager = typeManager;
        this.config = config;
        this.connectorId = connectorId;
    }

    protected URI getUri(TableDefinition tableDefinition){
        final URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(tableDefinition.getEndpoint());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Bad endpoint: " + tableDefinition.getEndpoint(), e);
        }
        return uri;
    }

}
