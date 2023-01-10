/*
 * Copyright (C) 2022 Cyoda Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.cyoda.presto;

import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.data.TableDataProvider;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import io.trino.spi.Page;
import io.trino.spi.PageBuilder;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.Constraint;
import io.trino.spi.predicate.NullableValue;
import io.trino.spi.type.Type;
import io.trino.spi.connector.ConnectorPageSource;
import io.trino.spi.TrinoException;
import com.google.common.collect.ImmutableList;
import reactor.core.scheduler.Schedulers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static com.cyoda.presto.CyodaErrorCode.CYODA_PAGING_ERROR;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("UnstableApiUsage")
public class CyodaFilteringPageSource<K,T>
        implements ConnectorPageSource
{
    private static final SupplierLogger LOG = SupplierLogger.get(CyodaFilteringPageSource.class);

    private final List<CyodaColumnHandle> columnHandles;
    private final TableDataProvider<T> dataProvider;

    private boolean finished;
    private long readTimeNanos;
    private long completedBytes;
    private long completedPositions;
    private final Iterable<T> responseIterable;
    private Iterator<T> responseIterator;
    private final List<Type> columnTypes;

    private final PageBuilder pageBuilder;
    private final AtomicInteger totalRowNumber;
    private final AtomicInteger pages;
    private final CyodaSplit split;

    public CyodaFilteringPageSource(
            AuthContext authContext,
            TableDataProvider<T> dataProvider,
            CyodaTableHandle tableHandle,
            List<CyodaColumnHandle> columnHandles,
            CyodaSplit split
    ) {
        this.columnHandles = ImmutableList.copyOf(requireNonNull(columnHandles, "columnHandles is null"));
        this.dataProvider = requireNonNull(dataProvider, "dataProvider is null");
        this.finished = false;
        List<CyodaColumnHandle> handles = columnHandles.stream()
                .collect(toImmutableList());
        this.columnTypes = handles.stream()
                .map(CyodaColumnHandle::getColumnType)
                .collect(toImmutableList());
        this.totalRowNumber = new AtomicInteger();
        this.pages = new AtomicInteger();

        this.pageBuilder = new PageBuilder(this.columnTypes);
        this.split = split;
        split.startExecution();
        this.responseIterable = dataProvider.getIterable(
                        authContext,
                        tableHandle,
                        split);

    }

    @Override
    public long getCompletedBytes()
    {
        return completedBytes;
    }

    @Override
    public OptionalLong getCompletedPositions()
    {
        return OptionalLong.of(completedPositions);
    }

    @Override
    public long getReadTimeNanos()
    {
        return readTimeNanos;
    }

    @Override
    public boolean isFinished()
    {
        return finished;
    }

    @Override
    public Page getNextPage() {
        AtomicInteger rowNumber = new AtomicInteger();
        if (finished) {
            return null;
        }

        long start = System.nanoTime();
        if ( responseIterator == null ) {
            responseIterator = responseIterable.iterator();
        }
        LOG.debug("Getting iterator took %s msec",((double) ((System.nanoTime()-start))) / 1.0E6);

        try {
            while (responseIterator.hasNext()) {
                final T nextItem = responseIterator.next();
                int totalRows = totalRowNumber.incrementAndGet();
                int i = rowNumber.incrementAndGet();
                LOG.debug("Retrieved row %s / %s (Total) with %s",()->i,()->totalRows,()->nextItem);
                processNext(nextItem);
                pageBuilder.declarePosition();
                if (pageBuilder.isFull()) {
                    break;
                }
            }

            if (!responseIterator.hasNext()) {
                close();
            }

            // only return a page if the buffer is full, or we are finishing
            if (pageBuilder.isEmpty() || (!finished && !pageBuilder.isFull())) {
                return null;
            }

            Page page = pageBuilder.build();
            int pageNumber = pages.incrementAndGet();
            completedPositions += page.getPositionCount();
            completedBytes += page.getSizeInBytes();
            pageBuilder.reset();
            LOG.debug("Page %d is full with %d completed positions %.2f kB total size with %s",
                    ()->pageNumber,
                    ()->completedPositions,
                    ()-> (float)completedBytes/1024,
                    ()->page
            );
            return page;
        } catch (Exception e) {
            finished = true;
            throw new TrinoException(CYODA_PAGING_ERROR,"Failure getting next page: " + e.getMessage(), e);
        } finally {
            readTimeNanos += System.nanoTime() - start;
            LOG.debug("Read time %.2f msec",()-> (double) readTimeNanos / 1.0E6);
        }
    }

    @Override
    public long getMemoryUsage() {
        return 0;
    }


    private void processNext(T item) {
        for (int i = 0; i < columnHandles.size(); i++) {
            BlockBuilder blockBuilder = pageBuilder.getBlockBuilder(i);
            CyodaColumnHandle columnHandle = columnHandles.get(i);
            dataProvider.writeValue(item, columnHandle, blockBuilder);
        }
    }

    @Override
    public void close()
    {
        finished = true;
        split.endExecution();
    }

}
