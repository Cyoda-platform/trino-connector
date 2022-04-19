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

import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.client.CyodaApiRequestHandler;
import com.facebook.presto.common.type.BigintType;
import com.facebook.presto.common.type.BooleanType;
import com.facebook.presto.common.type.DateType;
import com.facebook.presto.common.type.DoubleType;
import com.facebook.presto.common.type.IntegerType;
import com.facebook.presto.common.type.RealType;
import com.facebook.presto.common.type.SmallintType;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.TimestampType;
import com.facebook.presto.common.type.TinyintType;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.VarbinaryType;
import com.facebook.presto.spi.RecordCursor;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.hateoas.CollectionModel;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.facebook.presto.common.type.BooleanType.BOOLEAN;
import static com.facebook.presto.common.type.VarcharType.createUnboundedVarcharType;
import static com.facebook.presto.common.type.VarcharType.createVarcharType;
import static com.google.common.base.Preconditions.checkArgument;
import static io.airlift.slice.Slices.utf8Slice;
import static java.util.Objects.requireNonNull;

public class CyodaRecordCursor<T> implements RecordCursor {

    private final Iterator<T> response;
    private final CyodaApiRequestHandler<T> requestHandler;
    private final Map<Integer, CyodaColumnHandle> columnHandles;
    private T current;
    // AccumuloRecordCursor might be a good place to look
    private long bytesRead;
    private long nanoStart;
    private long nanoEnd;


    public CyodaRecordCursor(CyodaApiRequestHandler<T> requestHandler, List<CyodaColumnHandle> columnHandleList, CollectionModel<T> response) {
        this.requestHandler = requireNonNull(requestHandler, "requestHandler is null");
        requireNonNull(columnHandleList, "columnHandles is null");
        this.response = requireNonNull(response, "response is null").iterator();
        this.columnHandles = columnHandleList.stream().collect(Collectors.toMap(CyodaColumnHandle::getOrdinalPosition, x -> x));
    }

    @NonNull
    public static CyodaType getCyodaType(Type type) {
        requireNonNull(type, "type is null");
        final String base = type.getTypeSignature().getBase();
        if (StandardTypes.UUID.equals(base)) return CyodaType.UUID;
        if (StandardTypes.VARCHAR.equals(base)) return CyodaType.STRING;
        throw new IllegalArgumentException("Type " + type.getDisplayName() + " not mapped to a CyodaType");
    }

    @SuppressWarnings("squid:S125")
    public static Slice getColumnValue(Object columnValue, CyodaType cyodaType) {
        requireNonNull(cyodaType, "cyodaType is null");
        // Might be needed when we map other types
        // Type nativeType = cyodaType.getNativeType();
        if (columnValue == null) {
            return Slices.EMPTY_SLICE;
        } else {
            // VarcharEnumType
            // VarcharType
            // CharType
            // JsonType
            // LongDecimalType
            // VarbinaryType

            switch (cyodaType) {
                case UUID:
                    throw new UnsupportedOperationException("UUID does not work. " +
                            "It get's turned into a string in UuidType#getObjectValue back to the client and " +
                            "and then FixJsonDataUtils#fixValue tries to do a base64 decode");
//                    final UUID uuid = (UUID) columnValue;
//                    final Slice uuidSlice = wrappedLongArray(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
//                    return Optional.of(uuidSlice);
                case STRING:
                    final String string = columnValue.toString();
                    return (string == null) ? Slices.EMPTY_SLICE : utf8Slice(string);
                default:
                    throw new IllegalStateException("Handling of type " + cyodaType
                            + " is not implemented");
            }
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

        if (!response.hasNext()) {
            return false;
        }
        current = response.next();
        return true;
    }

    @Override
    public boolean getBoolean(int field) {
        checkFieldType(field, BOOLEAN);
        return Boolean.parseBoolean(getFieldValue(field).toString());
    }

    private Object getFieldValue(int field) {
        return requestHandler.getValue(current, field);
    }

    @Override
    public long getLong(int field) {
        Object value = getFieldValue(field);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof Date) {
            return ((Date) value).getTime();
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toInstant(ZoneOffset.UTC).toEpochMilli();
        }
        throw new IllegalStateException("Cannot retrieve long for " + getType(field));
    }

    @Override
    public double getDouble(int field) {
        Object value = getFieldValue(field);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw new IllegalStateException("Cannot retrieve double for " + getType(field));
    }

    @Override
    public Slice getSlice(int field) {

        final Object fieldValue = getFieldValue(field);
        final Type columnType = columnHandles.get(field).getColumnType();
        final String base = columnType.getTypeSignature().getBase();
        Slice value = getColumnValue(fieldValue, getCyodaType(columnType));
        return value;
    }

    @Override
    public Object getObject(int field) {
        return getFieldValue(field);
    }

    @Override
    public boolean isNull(int field) {
        return getFieldValue(field) == null;
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
