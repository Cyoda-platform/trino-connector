package com.cyoda.presto;

import com.cyoda.presto.client.data.TableDataProvider;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.google.common.collect.ImmutableList;
import io.trino.spi.Page;
import io.trino.spi.connector.ConnectorPageSource;
import io.trino.split.MappedPageSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CyodaCachedPageSource<T> implements ConnectorPageSource {

    private final long completedBytes;
    private final ImmutableList<Page> pages;

    private final Map<String, Integer> columnIdxMap;

    private final ThreadLocal<Integer> cursor = ThreadLocal.withInitial(() -> 0);

    public CyodaCachedPageSource(TableDataProvider<T> dataProvider,
                                 CyodaTableHandle tableHandle,
                                 CyodaSplit split){
        List<CyodaColumnHandle> allColumns = tableHandle.getProjectedColumns();
        CyodaFilteringPageSource<T> source = new CyodaFilteringPageSource<>(
                dataProvider,
                tableHandle,
                allColumns,
                split);
        pages = ImmutableList.<Page>builder().addAll(new Iterator<>() {
            @Override
            public boolean hasNext() {
                return !source.isFinished();
            }
            @Override
            public Page next() {
                return source.getNextPage();
            }
        }).build();
        completedBytes = source.getCompletedBytes();
        columnIdxMap = new HashMap<>();
        for (int i = 0; i < allColumns.size(); i++) {
            columnIdxMap.put(allColumns.get(i).getColumnName(), i);
        }
    }

    public MappedPageSource mapNewPage(List<CyodaColumnHandle> columnHandles){
        List<Integer> delegateIdx = columnHandles.stream().map(cch -> columnIdxMap.get(cch.getColumnName())).toList();
        return new MappedPageSource(this, delegateIdx);
    }

    @Override
    public long getCompletedBytes() {
        return completedBytes;
    }

    @Override
    public long getReadTimeNanos() {
        return 0;
    }

    @Override
    public boolean isFinished() {
        return cursor.get() >= pages.size();
    }

    @Override
    public Page getNextPage() {
        int i = cursor.get();
        Page res = pages.get(i++);
        cursor.set(i);
        return res;
    }

    @Override
    public long getMemoryUsage() {
        return 0;
    }

    @Override
    public void close() throws IOException {
        cursor.set(0);
    }
}
