package com.cyoda.presto;

import com.cyoda.presto.client.data.TableDataProvider;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import com.cyoda.presto.logging.SupplierLogger;
import com.google.common.collect.ImmutableList;
import io.trino.plugin.base.MappedPageSource;
import io.trino.spi.Page;
import io.trino.spi.connector.ConnectorPageSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CyodaCachedPageSource<T> implements ConnectorPageSource {

    private static final SupplierLogger LOG = SupplierLogger.get(CyodaCachedPageSource.class);
    private final long completedBytes;
    private final ImmutableList<Page> pages;

    private final Map<String, Integer> columnIdxMap;

    private final ThreadLocal<Integer> cursor = ThreadLocal.withInitial(() -> 0);

    public CyodaCachedPageSource(TableDataProvider<T> dataProvider,
                                 CyodaTableMeta tableHandle,
                                 CyodaSplit split){
        List<CyodaColumnHandle> allColumns = tableHandle.getProjectedColumns();
        CyodaFilteringPageSource<T> source = new CyodaFilteringPageSource<>(
                dataProvider,
                tableHandle,
                allColumns,
                split);
        List<Page> pagesPt = new ArrayList<>();
        while (!source.isFinished()){
            Page page = source.getNextPage();
            if (page !=  null){
                pagesPt.add(page);
            }
        }
        if (pagesPt.size() == 0){
            LOG.warn("Created empty page cache for split " + split.toString());
        }
        pages = ImmutableList.copyOf(pagesPt);
//        pages = ImmutableList.<Page>builder().addAll(new Iterator<>() {
//            @Override
//            public boolean hasNext() {
//                return !source.isFinished();
//            }
//            @Override
//            public Page next() {
//                return source.getNextPage();
//            }
//        }).build();
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
        if (cursor.get() >= pages.size()){
            LOG.warn(String.format("Cached page out of bounds - index:%s size:%s", cursor.get(), pages.size()));
            return null;
        }
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
