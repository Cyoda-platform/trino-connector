package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.connector.ConnectorSplitSource;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.FixedSplitSource;

import java.util.Collections;

public abstract class UnsplitTableDataProvider<T> extends TableDataProvider<T>{

    @Override
    public ConnectorSplitSource getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        return new FixedSplitSource(Collections.singletonList(CyodaSplit.emptySplit(tableHandle, queryId)));
    }

}
