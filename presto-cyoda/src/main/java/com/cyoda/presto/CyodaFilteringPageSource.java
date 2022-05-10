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
import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.logic.PredicateNode;
import com.cyoda.presto.client.types.SupportedDataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.cyoda.presto.CyodaErrorCode.CYODA_PAGING_ERROR;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.airlift.slice.Slices.EMPTY_SLICE;
import static java.lang.Float.floatToRawIntBits;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("UnstableApiUsage")
public class CyodaFilteringPageSource<T>
        implements ConnectorPageSource
{
    private final List<CyodaColumnHandle> columnHandles;
    private final ApiRequestHandler<T> requestHandler;

    private boolean finished;
    private long readTimeNanos;
    private long completedBytes;
    private long completedPositions;
    private final Supplier<Iterator<T>> responseSupplier;
    private Iterator<T> responseIterator = null;
    private final PageBuilder pageBuilder;
    private final List<Type> columnTypes;

    public CyodaFilteringPageSource(
            ApiRequestHandler<T> requestHandler,
            CyodaTableHandle tableHandle,
            List<CyodaColumnHandle> columnHandles,
            CyodaClient cyodaClient, PredicateNode<Any> predicates
    ) {
        requireNonNull(requestHandler, "requestHandler is null");
        this.columnHandles = ImmutableList.copyOf(requireNonNull(columnHandles, "columnHandles is null"));
        requireNonNull(cyodaClient, "Cyoda client is null");
        this.requestHandler = requireNonNull(requestHandler, "requestHandler is null");
        this.responseSupplier = () -> requestHandler.getResponseIterator(
                cyodaClient.getRequestPageSize(),
                tableHandle,
                predicates
        );
        this.finished = false;
        List<CyodaColumnHandle> handles = columnHandles.stream()
                .collect(toImmutableList());
        this.columnTypes = handles.stream()
                .map(CyodaColumnHandle::getColumnType)
                .collect(toImmutableList());
        this.pageBuilder = new PageBuilder(this.columnTypes);
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
        if (finished) {
            return null;
        }

        if ( responseIterator == null) {
            responseIterator = responseSupplier.get();
        }

        long start = System.nanoTime();
        try {
            while (responseIterator.hasNext()) {
                final T nextItem = responseIterator.next();
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
            completedPositions += page.getPositionCount();
            completedBytes += page.getSizeInBytes();
            pageBuilder.reset();
            return page;
        } catch (Exception e) {
            finished = true;
            throw new PrestoException(CYODA_PAGING_ERROR,"Failure getting next page: " + e.getMessage(), e);
        } finally {
            readTimeNanos += System.nanoTime() - start;
        }
    }


    private void processNext(T nextItem) {
        for (int i = 0; i < columnHandles.size(); i++) {
            Type type = columnTypes.get(i);
            BlockBuilder blockBuilder = pageBuilder.getBlockBuilder(i);
            CyodaColumnHandle columnHandle = columnHandles.get(i);
            SupportedDataType<?> supported = requestHandler.getValue(nextItem, columnHandle);
            if (supported == null || supported.isNull()) {
                blockBuilder.appendNull();
                continue;
            }
            writeObject(type, blockBuilder, supported);
        }
    }

    private void writeObject(Type type, BlockBuilder blockBuilder, SupportedDataType<?> supported) {
        switch (supported.dataType) {
            case BOOLEAN:
                type.writeBoolean(blockBuilder, supported.asBoolean());
                break;
            case BYTE:
                type.writeLong(blockBuilder, supported.asByte().longValue());
                break;
            case INTEGER:
                type.writeLong(blockBuilder, supported.asInt().longValue());
                break;
            case SHORT:
                type.writeLong(blockBuilder, supported.asShort().longValue());
                break;
            case LONG:
                type.writeLong(blockBuilder, supported.asLong());
                break;
            case BIG_INTEGER:
                type.writeLong(blockBuilder, supported.asBigInteger().longValue());
                break;
            case DOUBLE:
                type.writeDouble(blockBuilder, supported.asDouble());
                break;
            case FLOAT:
                type.writeLong(blockBuilder, floatToRawIntBits(supported.asFloat()));
                break;
            case DATE:
            case LOCAL_DATE_TIME:
            case ZONED_DATE_TIME:
                type.writeLong(blockBuilder, supported.asTimestampMillis());
                break;
            case LOCAL_DATE:
                type.writeLong(blockBuilder,supported.asLocalDate().toEpochDay());
                break;
            case YEAR:
                type.writeLong(blockBuilder,supported.asYear().getValue());
                break;
            case LOCAL_TIME:
                type.writeLong(blockBuilder,supported.asLocalDate().toEpochDay());
                break;
            case SET: {
                Type elementType = ((ArrayType) type).getElementType();
                BlockBuilder arrayBuilder = blockBuilder.beginBlockEntry();
                Optional.ofNullable((Set<?>) supported.value).orElse(Collections.emptySet())
                        .forEach(item -> writeObject(elementType, arrayBuilder, SupportedDataType.byType(item, elementType)));
                blockBuilder.closeEntry();
                break;
            }
            case LIST: {
                Type elementType = ((ArrayType) type).getElementType();
                BlockBuilder arrayBuilder = blockBuilder.beginBlockEntry();
                Optional.ofNullable((Collection<?>) supported.value).orElse(Collections.emptyList())
                        .forEach(item -> writeObject(elementType,arrayBuilder,SupportedDataType.byType(item,elementType)));
                blockBuilder.closeEntry();
                break;
            }
            case ARRAY: {
                Type elementType = ((ArrayType) type).getElementType();
                BlockBuilder arrayBuilder = blockBuilder.beginBlockEntry();
                Arrays.stream(Optional.ofNullable((Object[]) supported.value).orElse(new Object[0]))
                        .forEach(item -> writeObject(elementType,arrayBuilder,SupportedDataType.byType(item,elementType)));
                blockBuilder.closeEntry();
                break;
            }
            case MAP: {

                // WARNING: This is not going to work. ofObject() is not going to give us much here.
                // This is just an idea...
                MapType mapType = (MapType) type;
                Type keyType = mapType.getKeyType();
                Type valueType = mapType.getValueType();
                BlockBuilder mapBlockBuilder = blockBuilder.beginBlockEntry();
                for (Map.Entry<?, ?> entry : Optional.ofNullable((Map<?, ?>) supported.value).orElse(Collections.emptyMap()).entrySet()) {
                    writeObject(keyType,mapBlockBuilder, SupportedDataType.byType(entry.getKey(),keyType));
                    writeObject(valueType,mapBlockBuilder, SupportedDataType.byType(entry.getValue(),valueType));
                }
                blockBuilder.closeEntry();
                break;
            }
            case OBJECT:
                Slice slice = supported.stringify().map(Slices::utf8Slice).orElse(EMPTY_SLICE);
                type.writeSlice(blockBuilder, slice);
                break;
            case BIG_DECIMAL:
            case CLASS:
            case LOCALE:
            case CHARACTER:
            case YEAR_MONTH:
            case STRING:
            case UUID_TYPE:
            default:
                type.writeSlice(blockBuilder, supported.asSlice(type));
                break;
        }
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
