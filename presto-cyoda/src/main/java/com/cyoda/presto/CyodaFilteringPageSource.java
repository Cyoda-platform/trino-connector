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

import com.cyoda.presto.client.ApiRequestHandler;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.logic.converters.PrestoValueConverter;
import com.cyoda.presto.client.logic.converters.PrestoValueConverterProvider;
import com.cyoda.presto.client.types.DataTypeValue;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.Page;
import com.facebook.presto.common.PageBuilder;
import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.type.ArrayType;
import com.facebook.presto.common.type.MapType;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.ConnectorPageSource;
import com.facebook.presto.spi.PrestoException;
import com.google.common.collect.ImmutableList;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.cyoda.presto.CyodaErrorCode.CYODA_PAGING_ERROR;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.airlift.slice.Slices.EMPTY_SLICE;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("UnstableApiUsage")
public class CyodaFilteringPageSource<T>
        implements ConnectorPageSource
{
    private static final SupplierLogger LOG = SupplierLogger.get(CyodaFilteringPageSource.class);

    private final List<CyodaColumnHandle> columnHandles;
    private final ApiRequestHandler<T> requestHandler;

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

    public CyodaFilteringPageSource(
            ApiRequestHandler<T> requestHandler,
            CyodaTableHandle tableHandle,
            List<CyodaColumnHandle> columnHandles,
            CyodaClient cyodaClient, CompoundPredicateNode predicates
    ) {
        requireNonNull(requestHandler, "requestHandler is null");
        this.columnHandles = ImmutableList.copyOf(requireNonNull(columnHandles, "columnHandles is null"));
        requireNonNull(cyodaClient, "Cyoda client is null");
        this.requestHandler = requireNonNull(requestHandler, "requestHandler is null");
        this.finished = false;
        List<CyodaColumnHandle> handles = columnHandles.stream()
                .collect(toImmutableList());
        this.columnTypes = handles.stream()
                .map(CyodaColumnHandle::getColumnType)
                .collect(toImmutableList());
        this.totalRowNumber = new AtomicInteger();
        this.pages = new AtomicInteger();

        this.pageBuilder = new PageBuilder(this.columnTypes);

        this.responseIterable = requestHandler.asFlux(
                        tableHandle.getAuthPayload(),
                        cyodaClient.getRequestPageSize(),
                        tableHandle,
                        predicates,
                        SizeListener.NOT_LISTENING)
                .subscribeOn(Schedulers.parallel())  // Probably the default.
                .toIterable();
    }

    @Override
    public long getCompletedBytes()
    {
        return completedBytes;
    }

    @Override
    public long getCompletedPositions()
    {
        return completedPositions;
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
                finished = true;
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
            throw new PrestoException(CYODA_PAGING_ERROR,"Failure getting next page: " + e.getMessage(), e);
        } finally {
            readTimeNanos += System.nanoTime() - start;
            LOG.debug("Read time %.2f msec",()-> (double) readTimeNanos / 1.0E6);
        }
    }


    private void processNext(T item) {
        for (int i = 0; i < columnHandles.size(); i++) {
            Type type = columnTypes.get(i);
            BlockBuilder blockBuilder = pageBuilder.getBlockBuilder(i);
            CyodaColumnHandle columnHandle = columnHandles.get(i);
            DataTypeValue<?> supported = requestHandler.getValue(item, columnHandle);
            if (supported == null || supported.isNull()) {
                blockBuilder.appendNull();
                continue;
            }
            writeObject(type, blockBuilder, supported);
        }
    }

    private void writeObject(Type type, BlockBuilder blockBuilder, DataTypeValue<?> dataTypeValue) {
        switch (dataTypeValue.getDataType()) {
            case DOUBLE:
                type.writeDouble(blockBuilder, dataTypeValue.asDouble());
                break;
            case BOOLEAN:
                type.writeBoolean(blockBuilder, dataTypeValue.asBoolean());
                break;
            case BYTE:
            case FLOAT:
            case INTEGER:
            case SHORT:
            case LONG:
            case YEAR:
            case LOCAL_DATE:
            case LOCAL_DATE_TIME:
            case ZONED_DATE_TIME:
            case LOCAL_TIME:
            case DATE: {
                PrestoValueConverter<Object> prestoValueConverter = getPrestoValueConverter(dataTypeValue);
                Long value = Optional.ofNullable(dataTypeValue.value).map(prestoValueConverter::toLong)
                        .orElseThrow(()->new IllegalArgumentException(dataTypeValue.getDataType() + " value is null "));
                type.writeLong(blockBuilder, value);
                break;
            }
            case SET: {
                Type elementType = ((ArrayType) type).getElementType();
                BlockBuilder arrayBuilder = blockBuilder.beginBlockEntry();
                Optional.ofNullable((Set<?>) dataTypeValue.value).orElse(Collections.emptySet())
                        .forEach(item -> writeObject(elementType, arrayBuilder, DataTypeValue.byType(item, elementType)));
                blockBuilder.closeEntry();
                break;
            }
            case LIST: {
                Type elementType = ((ArrayType) type).getElementType();
                BlockBuilder arrayBuilder = blockBuilder.beginBlockEntry();
                Optional.ofNullable((Collection<?>) dataTypeValue.value).orElse(Collections.emptyList())
                        .forEach(item -> writeObject(elementType,arrayBuilder, DataTypeValue.byType(item,elementType)));
                blockBuilder.closeEntry();
                break;
            }
//            case ARRAY: {
//                Type elementType = ((ArrayType) type).getElementType();
//                BlockBuilder arrayBuilder = blockBuilder.beginBlockEntry();
//                Arrays.stream(Optional.ofNullable((Object[]) dataTypeValue.value).orElse(new Object[0]))
//                        .forEach(item -> writeObject(elementType,arrayBuilder, DataTypeValue.byType(item,elementType)));
//                blockBuilder.closeEntry();
//                break;
//            }
            case MAP: {

                MapType mapType = (MapType) type;
                Type keyType = mapType.getKeyType();
                Type valueType = mapType.getValueType();
                BlockBuilder mapBlockBuilder = blockBuilder.beginBlockEntry();
                for (Map.Entry<?, ?> entry : Optional.ofNullable((Map<?, ?>) dataTypeValue.value).orElse(Collections.emptyMap()).entrySet()) {
                    writeObject(keyType,mapBlockBuilder, DataTypeValue.byType(entry.getKey(),keyType));
                    writeObject(valueType,mapBlockBuilder, DataTypeValue.byType(entry.getValue(),valueType));
                }
                blockBuilder.closeEntry();
                break;
            }
            case OBJECT:
                Slice slice = dataTypeValue.stringify().map(Slices::utf8Slice).orElse(EMPTY_SLICE);
                type.writeSlice(blockBuilder, slice);
                break;
            case BIG_INTEGER:
            case BIG_DECIMAL:
            case CLASS:
            case LOCALE:
            case CHARACTER:
            case YEAR_MONTH:
            case STRING:
            case UUID_TYPE:
            default: {
                PrestoValueConverter<Object> prestoValueConverter = getPrestoValueConverter(dataTypeValue);
                Slice value = Optional.ofNullable(dataTypeValue.value).map(it->prestoValueConverter.toSlice(dataTypeValue.value))
                        .orElseThrow(() -> new IllegalArgumentException(dataTypeValue.getDataType() + " value is null "));
                type.writeSlice(blockBuilder, value);
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <S> PrestoValueConverter<S> getPrestoValueConverter(DataTypeValue<?> supported) {
        return PrestoValueConverterProvider.getPrestoValueConverter(supported.getDataType());
    }


    @Override
    public long getSystemMemoryUsage()
    {
        return 0;
    }

    @Override
    public void close()
    {
        finished = true;
    }

}
