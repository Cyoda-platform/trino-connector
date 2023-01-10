package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.DummyTableHandle;
import io.trino.spi.connector.ConnectorSplitSource;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.FixedSplitSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public class DummyTableDataProvider extends TableDataProvider<Map<String, String>>{

    @Override
    public Iterable<Map<String, String>> getIterable(AuthContext authContext, CyodaTableHandle tableHandle, CyodaSplit split) {
        return Collections.singleton(((DummyTableHandle)tableHandle).getContent());
    }

    @Override
    public ConnectorSplitSource getSplits(AuthContext authContext, CyodaTableHandle tableHandle, Constraint constraint) {
        return new FixedSplitSource(Collections.singletonList(CyodaSplit.emptySplit(tableHandle.getTableName())));
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull Map<String, String> entity, CyodaColumnHandle columnHandle) {
        return entity.get(columnHandle.getColumnName());
    }
}
