package com.cyoda.presto;

import com.cyoda.presto.client.data.TableDataProvider;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import io.airlift.slice.Slice;
import io.trino.spi.block.Block;
import io.trino.spi.connector.UpdatablePageSource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CyodaVirtualPageSource<T,K> extends CyodaFilteringPageSource<T> implements UpdatablePageSource {

    private final Consumer<Block> deleteOperation;

    public CyodaVirtualPageSource(TableDataProvider<T> dataProvider,
                                  CyodaTableMeta tableHandle,
                                  List<CyodaColumnHandle> columnHandles,
                                  CyodaSplit split,
                                  Consumer<Block> deleteOperation) {
        super(dataProvider, tableHandle, columnHandles, split);
        this.deleteOperation = deleteOperation;
    }

    @Override
    public void deleteRows(Block rowIds) {
        deleteOperation.accept(rowIds);
    }

    @Override
    public CompletableFuture<Collection<Slice>> finish() {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
}
