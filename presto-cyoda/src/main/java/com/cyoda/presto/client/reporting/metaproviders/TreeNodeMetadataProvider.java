package com.cyoda.presto.client.reporting.metaproviders;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.treenode.TreeNodeTrinoAPIMock;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import io.trino.spi.type.TypeManager;

import javax.inject.Inject;
import java.util.List;

public class TreeNodeMetadataProvider extends TableMetadataProvider {

    private final TreeNodeTrinoAPIMock apiMock = new TreeNodeTrinoAPIMock();
    @Inject
    public TreeNodeMetadataProvider(TypeManager typeManager, CyodaConfig config, CyodaConnectorId connectorId) {
        super(typeManager, config, connectorId);
    }

    @Override
    public List<CyodaTableHandle> listTables(AuthContext authContext) {
        return null;
    }

    @Override
    public CyodaTableMeta getTableMeta(CyodaTableHandle tableHandle) {
        return null;
    }
}
