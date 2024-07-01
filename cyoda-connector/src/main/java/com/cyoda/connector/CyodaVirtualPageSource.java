package com.cyoda.connector;

import com.cyoda.connector.client.data.TableDataProvider;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.handles.CyodaTableMeta;
import io.trino.spi.block.Block;

import java.util.List;
import java.util.function.Consumer;

public class CyodaVirtualPageSource<T,K> extends CyodaFilteringPageSource<T> {

    private final Consumer<Block> deleteOperation;

    public CyodaVirtualPageSource(TableDataProvider<T> dataProvider,
                                  CyodaTableMeta tableHandle,
                                  List<CyodaColumnHandle> columnHandles,
                                  CyodaSplit split,
                                  Consumer<Block> deleteOperation) {
        super(dataProvider, tableHandle, columnHandles, split);
        this.deleteOperation = deleteOperation;
    }

//    @Override
//    public void deleteRows(Block rowIds) {
//        deleteOperation.accept(rowIds);
//    }
//
//    @Override
//    public CompletableFuture<Collection<Slice>> finish() {
//        return CompletableFuture.completedFuture(Collections.emptyList());
//    }
}
