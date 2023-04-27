package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.DummyTableHandle;
import io.trino.spi.connector.Constraint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DummyTableDataProvider extends TableDataProvider<Map<String, String>> {

    @Override
    public Iterable<Map<String, String>> getIterable(CyodaTableHandle tableHandle, CyodaSplit split) {
        return Collections.singleton((Map<String, String>)split.getCustomData());
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull Map<String, String> entity, CyodaColumnHandle columnHandle) {
        return entity.get(columnHandle.getColumnName());
    }

    @Override
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        return Collections.singletonList(CyodaSplit.emptyCoordinatorSplit(
                tableHandle.getTableName(),
                queryId,
                ((DummyTableHandle)tableHandle).getContent()));
    }
}
