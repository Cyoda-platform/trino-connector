package com.cyoda.presto;

import com.facebook.presto.spi.connector.Connector;
import com.facebook.presto.spi.connector.ConnectorMetadata;
import com.facebook.presto.spi.connector.ConnectorSplitManager;
import com.facebook.presto.spi.connector.ConnectorTransactionHandle;
import com.facebook.presto.spi.transaction.IsolationLevel;

public class CyodaConnector implements Connector  {
    @Override
    public ConnectorTransactionHandle beginTransaction(IsolationLevel isolationLevel, boolean readOnly) {
        return null;
    }

    @Override
    public ConnectorMetadata getMetadata(ConnectorTransactionHandle transactionHandle) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public ConnectorSplitManager getSplitManager() {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
