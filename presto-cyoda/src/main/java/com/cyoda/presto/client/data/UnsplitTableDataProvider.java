package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.FixedSplitSource;

import java.util.Collections;
import java.util.List;

public abstract class UnsplitTableDataProvider<T> extends TableDataProvider<T>{

    @Override
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        return Collections.singletonList(CyodaSplit.emptySplit(tableHandle, queryId));
    }

}
