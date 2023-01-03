package com.cyoda.presto.client.data;

import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.DummyTableHandle;
import io.trino.spi.connector.Constraint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public class DummyTableDataProvider extends TableDataProvider<Map<String, String>>{

    @Override
    public Iterable<Map<String, String>> getIterable(AuthContext authContext, CyodaTableHandle tableHandle, Constraint constraint) {
        return Collections.singleton(((DummyTableHandle)tableHandle).getContent());
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull Map<String, String> entity, CyodaColumnHandle columnHandle) {
        return entity.get(columnHandle.getColumnName());
    }
}
