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
import com.cyoda.presto.client.SupportedDataType;
import com.cyoda.presto.client.Types;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.common.type.BigintType;
import com.facebook.presto.common.type.BooleanType;
import com.facebook.presto.common.type.DateType;
import com.facebook.presto.common.type.DecimalType;
import com.facebook.presto.common.type.Decimals;
import com.facebook.presto.common.type.DoubleType;
import com.facebook.presto.common.type.IntegerType;
import com.facebook.presto.common.type.RealType;
import com.facebook.presto.common.type.SmallintType;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.TimestampType;
import com.facebook.presto.common.type.TinyintType;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.VarbinaryType;
import com.facebook.presto.common.type.VarcharType;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.RecordCursor;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.PagedModel;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.cyoda.presto.CyodaErrorCode.CYODA_INCORRECT_TYPE_ERROR;
import static com.cyoda.presto.CyodaErrorCode.CYODA_UNSUPPORTED_TYPE_ERROR;
import static com.facebook.presto.common.type.BigintType.BIGINT;
import static com.facebook.presto.common.type.BooleanType.BOOLEAN;
import static com.facebook.presto.common.type.DateType.DATE;
import static com.facebook.presto.common.type.IntegerType.INTEGER;
import static com.facebook.presto.common.type.RealType.REAL;
import static com.facebook.presto.common.type.SmallintType.SMALLINT;
import static com.facebook.presto.common.type.TimeType.TIME;
import static com.facebook.presto.common.type.TimestampType.TIMESTAMP;
import static com.facebook.presto.common.type.TinyintType.TINYINT;
import static com.facebook.presto.common.type.VarcharType.createUnboundedVarcharType;
import static com.facebook.presto.common.type.VarcharType.createVarcharType;
import static com.facebook.presto.spi.StandardErrorCode.NOT_SUPPORTED;
import static com.google.common.base.Preconditions.checkArgument;
import static io.airlift.slice.Slices.*;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class CyodaRecordCursor<T> implements RecordCursor {

    private final PagedModel<T> response;
    private final Iterator<T> reponseIter;
    private final CyodaApiRequestHandler<T> requestHandler;
    private final Map<Integer, CyodaColumnHandle> columnHandles;
    private final long maxRowsToRead;

    private T current;
    // AccumuloRecordCursor might be a good place to look
    private long bytesRead = 0;
    private long rowsRead = 0;
    private long nanoStart;
    private long nanoEnd;


    public CyodaRecordCursor(CyodaApiRequestHandler<T> requestHandler, List<CyodaColumnHandle> columnHandleList, PagedModel<T> response) {
        this.requestHandler = requireNonNull(requestHandler, "requestHandler is null");
        requireNonNull(columnHandleList, "columnHandles is null");
        this.response = requireNonNull(response, "response is null");
        this.reponseIter = response.iterator();
        this.maxRowsToRead = getMaxRowsToRead(response);
        this.columnHandles = columnHandleList.stream().collect(Collectors.toMap(CyodaColumnHandle::getOrdinalPosition, x -> x));
    }

    private long getMaxRowsToRead(PagedModel<T> response) {
        PagedModel.PageMetadata metadata = response.getMetadata();
        if ( metadata != null ) {
            return metadata.getSize();
        } else {
            return 0;
        }
    }

    @NonNull
    public static CyodaType getCyodaType(Type type) {
        requireNonNull(type, "type is null");
        final String base = type.getTypeSignature().getBase();
        if (StandardTypes.UUID.equals(base)) return CyodaType.UUID;
        if (StandardTypes.VARCHAR.equals(base)) return CyodaType.STRING;
        throw new PrestoException(CYODA_UNSUPPORTED_TYPE_ERROR,"Type " + type.getDisplayName() + " not mapped to a CyodaType");
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
        return requestHandler.getValue(current, field);
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
        checkArgument(Types.isArrayType(type) || Types.isMapType(type), "Expected field %s to be a type of array or map but is %s", field, type);

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

    /**
     * Taken from CassandraType of module presto-cassandra.
     */
    enum CyodaType {
        UUID(createVarcharType(Constants.UUID_STRING_UUID_LENGTH), java.util.UUID.class),
        TIMEUUID(createVarcharType(Constants.UUID_STRING_UUID_LENGTH), java.util.UUID.class),
        STRING(createVarcharType(Constants.UUID_STRING_MAX_LENGTH), String.class),
        BIGINT(BigintType.BIGINT, Long.class),
        BLOB(VarbinaryType.VARBINARY, ByteBuffer.class),
        BOOLEAN(BooleanType.BOOLEAN, Boolean.class),
        DOUBLE(DoubleType.DOUBLE, Double.class),
        FLOAT(RealType.REAL, Float.class),
        INET(createVarcharType(Constants.IP_ADDRESS_STRING_MAX_LENGTH), InetAddress.class),
        INT(IntegerType.INTEGER, Integer.class),
        SMALLINT(SmallintType.SMALLINT, Short.class),
        TINYINT(TinyintType.TINYINT, Byte.class),
        TEXT(createUnboundedVarcharType(), String.class),
        DATE(DateType.DATE, LocalDate.class),
        TIMESTAMP(TimestampType.TIMESTAMP, LocalDateTime.class),
        VARCHAR(createUnboundedVarcharType(), String.class),
        VARINT(createUnboundedVarcharType(), BigInteger.class),
        LIST(createUnboundedVarcharType(), null),
        MAP(createUnboundedVarcharType(), null),
        SET(createUnboundedVarcharType(), null);

        private final Type nativeType;
        private final Class<?> javaType;

        CyodaType(Type nativeType, Class<?> javaType) {
            this.nativeType = requireNonNull(nativeType, "nativeType is null");
            this.javaType = javaType;
        }

        public Type getNativeType() {
            return nativeType;
        }

        public Class<?> getJavaType() {
            return javaType;
        }

    }

    @SuppressWarnings("squid:S1068")
    private static class Constants {
        private static final int UUID_STRING_MAX_LENGTH = 36;
        private static final int UUID_STRING_UUID_LENGTH = 16;
        // IPv4: 255.255.255.255 - 15 characters
        // IPv6: FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF - 39 characters
        // IPv4 embedded into IPv6: FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:255.255.255.255 - 45 characters
        private static final int IP_ADDRESS_STRING_MAX_LENGTH = 45;
    }
}
