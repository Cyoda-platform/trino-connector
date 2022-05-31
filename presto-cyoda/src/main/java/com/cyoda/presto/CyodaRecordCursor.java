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
import com.cyoda.presto.client.logic.converters.PrestoValueConverter;
import com.cyoda.presto.client.logic.converters.PrestoValueConverterProvider;
import com.cyoda.presto.client.types.DataTypeValue;
import com.cyoda.presto.client.types.SupportedDataType;
import com.cyoda.presto.client.types.TypesUtil;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.RecordCursor;
import io.airlift.slice.Slice;
import org.codehaus.plexus.util.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cyoda.presto.CyodaErrorCode.CYODA_INCORRECT_TYPE_ERROR;
import static com.facebook.presto.common.type.BooleanType.BOOLEAN;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Not needed see comments in {@link CyodaRecordSetProvider}
 * We have {@link CyodaFilteringPageSource }
 * @param <T>
 */
public class CyodaRecordCursor<T> implements RecordCursor,SizeListener {

    private static final SupplierLogger LOG = SupplierLogger.get(CyodaRecordCursor.class);

    private final Iterator<T> responseIter;
    private final ApiRequestHandler<T> requestHandler;
    private final Map<Integer, CyodaColumnHandle> columnHandles;
    private long maxEntries = -1;

    private T current;
    // AccumuloRecordCursor might be a good place to look
    private long bytesRead = 0;
    private long rowsRead = 0;
    private long nanoStart;
    private long nanoEnd;


    public CyodaRecordCursor(
            ApiRequestHandler<T> requestHandler,
            CyodaTableHandle tableHandle,
            List<CyodaColumnHandle> columnHandleList,
            Function<SizeListener,Iterator<T>> reponseIterGetter
    ) {
        this.requestHandler = requireNonNull(requestHandler, "requestHandler is null");
        requireNonNull(columnHandleList, "columnHandles is null");
        Iterator<T> responseIter = reponseIterGetter.apply(this);
        this.responseIter = requireNonNull(responseIter, "reponseIter is null");
        if (tableHandle.getProjectedColumns().isPresent()) {
            this.columnHandles = tableHandle.getProjectedColumns().get().stream().collect(Collectors.toMap(CyodaColumnHandle::getOrdinalPosition, x -> x));
        } else {
            this.columnHandles = columnHandleList.stream().collect(Collectors.toMap(CyodaColumnHandle::getOrdinalPosition, x -> x));
        }
    }

    public void sizeKnown(long size) {
        LOG.debug("Received size: %s",size);
        maxEntries = size;
    }

    @Override
    public long getCompletedBytes() {
        return bytesRead;
    }

    @Override
    public long getReadTimeNanos() {
        long thisNanoEnd = nanoEnd == 0 ? System.nanoTime() : nanoEnd;
        return nanoStart > 0L ? thisNanoEnd - nanoStart : 0L;
    }

    @Override
    public Type getType(int field) {
        checkArgument(field >= 0 && field < columnHandles.size(), "Invalid field index");
        return columnHandles.get(field).getColumnType();
    }

    @Override
    public boolean advanceNextPosition() {
        if (nanoStart == 0) {
            nanoStart = System.nanoTime();
        }

        // TODO: Calculate bytesRead in this method

        boolean maxRowsHaveBeenRead = maxEntries > 0 && rowsRead >= maxEntries;
        if (!responseIter.hasNext() || maxRowsHaveBeenRead) {
            LOG.debug("Finished reading %s / %s (Total)",()->rowsRead,()->maxEntries);
            return false;
        }

        current = responseIter.next();
        rowsRead++;
        LOG.debug("Got %s / %s (total) with %s",()->rowsRead,()->maxEntries,()->current.toString());
        return true;
    }

    @Override
    public boolean getBoolean(int field) {
        checkFieldType(field, BOOLEAN);
        return Boolean.parseBoolean(requireNonNull(getFieldValue(field).value).toString());
    }

    private DataTypeValue<?> getFieldValue(int field) {
        return requestHandler.getValue(current, columnHandles.get(field));
    }

    @Override
    public long getLong(int field) {
        DataTypeValue<?> supported = getFieldValue(field);
        PrestoValueConverter<Object> prestoValueConverter = getPrestoValueConverter(supported);
        return Optional.ofNullable(supported.value).map(prestoValueConverter::toLong)
                .orElseThrow(()->new IllegalArgumentException(supported.supportedDataType + " value is null "));
    }

    @SuppressWarnings("unchecked")
    private <S> PrestoValueConverter<S> getPrestoValueConverter(DataTypeValue<?> supported) {
        return PrestoValueConverterProvider.getPrestoValueConverter((SupportedDataType<S>) supported.supportedDataType);
    }


    @Override
    public double getDouble(int field) {
        DataTypeValue<?> supported = getFieldValue(field);
        if (supported.getDataType().isNumber() ) {
            return ((Number) requireNonNull(supported.value)).doubleValue();
        }
        throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR,"Cannot retrieve double for " + getType(field));
    }

    @Override
    public Slice getSlice(int field) {
        final DataTypeValue<?> supported = getFieldValue(field);
        return supported.asSlice(getType(field));
    }

    @Override
    public Object getObject(int field) {
        Type type = getType(field);
        checkArgument(TypesUtil.isArrayType(type) || TypesUtil.isMapType(type), "Expected field %s to be a type of array or map but is %s", field, type);

        return getFieldValue(field).value;
    }

    @Override
    public boolean isNull(int field) {
        DataTypeValue<?> fieldValue = getFieldValue(field);
        return fieldValue == null || fieldValue.value == null;
    }

    @SuppressWarnings("SameParameterValue")
    private void checkFieldType(int field, Type expected) {
        Type actual = getType(field);
        checkArgument(actual.equals(expected), "Expected field %s to be type %s but is %s", field, expected, actual);
    }

    @Override
    public void close() {
        nanoEnd = System.nanoTime();
    }

    /**
     * Checks that the given field is one of the provided types.
     *
     * @param field Ordinal of the field
     * @param expected An array of expected types
     * @throws IllegalArgumentException If the given field does not match one of the types
     */
    private void checkFieldType(int field, Type... expected)
    {
        Type actual = getType(field);
        for (Type type : expected) {
            if (actual.equals(type)) {
                return;
            }
        }

        throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR,
                format("Expected field %s to be a type of %s but is %s", field, StringUtils.join(expected, ","), actual));
    }



}
