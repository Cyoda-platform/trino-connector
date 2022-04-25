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

import com.cyoda.presto.client.CyodaApiRequestHandler;
import com.cyoda.presto.client.types.SupportedDataType;
import com.cyoda.presto.client.types.TypesUtil;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.RecordCursor;
import io.airlift.slice.Slice;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.hateoas.PagedModel;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cyoda.presto.CyodaErrorCode.CYODA_INCORRECT_TYPE_ERROR;
import static com.facebook.presto.common.type.BigintType.BIGINT;
import static com.facebook.presto.common.type.BooleanType.BOOLEAN;
import static com.facebook.presto.common.type.DateType.DATE;
import static com.facebook.presto.common.type.IntegerType.INTEGER;
import static com.facebook.presto.common.type.RealType.REAL;
import static com.facebook.presto.common.type.SmallintType.SMALLINT;
import static com.facebook.presto.common.type.TimeType.TIME;
import static com.facebook.presto.common.type.TimestampType.TIMESTAMP;
import static com.facebook.presto.common.type.TinyintType.TINYINT;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class CyodaRecordCursor<T> implements RecordCursor {

    private final PagedModel<T> response;
    private final Iterator<T> reponseIter;
    private final CyodaApiRequestHandler<T> requestHandler;
    private final Map<Integer, CyodaColumnHandle> columnHandles;
    private final long maxRowsToRead;
    private final CyodaTableHandle tableHandle;

    private T current;
    // AccumuloRecordCursor might be a good place to look
    private long bytesRead = 0;
    private long rowsRead = 0;
    private long nanoStart;
    private long nanoEnd;


    public CyodaRecordCursor(
            CyodaApiRequestHandler<T> requestHandler,
            CyodaTableHandle tableHandle,
            List<CyodaColumnHandle> columnHandleList,
            PagedModel<T> response
    ) {
        this.requestHandler = requireNonNull(requestHandler, "requestHandler is null");
        this.tableHandle = requireNonNull(tableHandle, "tableHandle is null");
        requireNonNull(columnHandleList, "columnHandles is null");
        this.response = requireNonNull(response, "response is null");
        this.reponseIter = response.iterator();
        this.maxRowsToRead = getMaxRowsToRead(response);
        if (tableHandle.getProjectedColumns().isPresent()) {
            this.columnHandles = tableHandle.getProjectedColumns().get().stream().collect(Collectors.toMap(CyodaColumnHandle::getOrdinalPosition, x -> x));
        } else {
            this.columnHandles = columnHandleList.stream().collect(Collectors.toMap(CyodaColumnHandle::getOrdinalPosition, x -> x));
        }
    }

    private long getMaxRowsToRead(PagedModel<T> response) {
        PagedModel.PageMetadata metadata = response.getMetadata();
        if ( metadata != null ) {
            return metadata.getSize();
        } else {
            return 0;
        }
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

        boolean maxRowsHaveBeenRead = maxRowsToRead > 0 && rowsRead >= maxRowsToRead;
        if (!reponseIter.hasNext() || maxRowsHaveBeenRead) {
            return false;
        }

        current = reponseIter.next();
        rowsRead++;
        return true;
    }

    @Override
    public boolean getBoolean(int field) {
        checkFieldType(field, BOOLEAN);
        return Boolean.parseBoolean(getFieldValue(field).value.toString());
    }

    private SupportedDataType<?> getFieldValue(int field) {
        return requestHandler.getValue(current, columnHandles.get(field));
    }

    @Override
    public long getLong(int field) {
        checkFieldType(field, BIGINT, DATE, INTEGER, REAL, SMALLINT, TIME, TIMESTAMP, TINYINT);
        return getFieldValue(field).parseToLong();
    }

    @Override
    public double getDouble(int field) {
        SupportedDataType<?> supported = getFieldValue(field);

        if (supported.dataType.isNumber() ) {
            return ((Number) supported.value).doubleValue();
        }
        throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR,"Cannot retrieve double for " + getType(field));
    }

    @Override
    public Slice getSlice(int field) {
        final SupportedDataType<?> supported = getFieldValue(field);
        Type type = getType(field);
        return supported.asSlice(type);
    }



    @Override
    public Object getObject(int field) {
        Type type = getType(field);
        checkArgument(TypesUtil.isArrayType(type) || TypesUtil.isMapType(type), "Expected field %s to be a type of array or map but is %s", field, type);

        return getFieldValue(field).value;
    }

    @Override
    public boolean isNull(int field) {
        return getFieldValue(field).value == null;
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
