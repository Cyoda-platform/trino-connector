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

package com.cyoda.presto.client.types;

import com.facebook.airlift.json.JsonCodec;
import com.facebook.airlift.json.JsonObjectMapperProvider;
import com.facebook.presto.common.type.BigintType;
import com.facebook.presto.common.type.BooleanType;
import com.facebook.presto.common.type.DateType;
import com.facebook.presto.common.type.DecimalType;
import com.facebook.presto.common.type.Decimals;
import com.facebook.presto.common.type.DoubleType;
import com.facebook.presto.common.type.IntegerType;
import com.facebook.presto.common.type.JsonType;
import com.facebook.presto.common.type.RealType;
import com.facebook.presto.common.type.SmallintType;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.TimestampType;
import com.facebook.presto.common.type.TinyintType;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.TypeSignature;
import com.facebook.presto.common.type.VarbinaryType;
import com.facebook.presto.common.type.VarcharType;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.facebook.presto.type.UuidType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.primitives.UnsignedBytes;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.cyoda.presto.CyodaErrorCode.CYODA_INCORRECT_TYPE_ERROR;
import static com.cyoda.presto.client.types.DataType.*;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static io.airlift.slice.Slices.EMPTY_SLICE;
import static io.airlift.slice.Slices.wrappedBuffer;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.intBitsToFloat;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class DataTypeValue<T> implements Comparable<DataTypeValue<T>> {
    @Nullable public final T value;
    public final Class<T> javaType;
    public final SupportedDataType<T> supportedDataType;

    protected DataTypeValue(T value, SupportedDataType<T> supportedDataType) {
        this.value = value;
        this.supportedDataType = supportedDataType;
        this.javaType = (Class<T>) supportedDataType.getDataType().getJavaType();
    }
    protected DataTypeValue(T value, Class<T> javaType) {
        this.value = value;
        if (value != null && !javaType.isAssignableFrom(value.getClass())) {
            throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR,
                    format("Incompatible type. %s is not assignable from %s", javaType, value.getClass()));
        }
        this.javaType = javaType;
        final DataType fromJavaType = DataType.dataTypeFromClass(javaType);
        requireNonNull(fromJavaType, javaType + " not mapped as a DataType");
        this.supportedDataType = fromJavaType.asSupported();
    }

    private static <S> DataTypeValue<S> of(S value, Class<S> javaType) {
        return new DataTypeValue<>(value, javaType);
    }

    @SuppressWarnings({"java:S1452", "java:S3740", "rawtypes"})
    public static DataTypeValue<?> ofAny(Object value, Class<?> javaType) {
        return new DataTypeValue(value, javaType);
    }

    public static <T> DataTypeValue<T> of(T value) {
        //noinspection unchecked
        return new DataTypeValue<>(value, (Class<T>) value.getClass());
    }

    public static <T> DataTypeValue<T> of(T value, SupportedDataType<T> dataType) {
        return new DataTypeValue<>(value, dataType);
    }


    @SuppressWarnings({"java:S1452", "java:S3740", "rawtypes"})
    public static DataTypeValue<?> byType(Object value, Type type) {
        DataType dataType = fromType(type);
        return new DataTypeValue(value, dataType.getJavaType());
    }

    private static DataType fromType(Type type) {
        if ( type.getTypeSignature().getBase().equals(VarcharType.VARCHAR.getTypeSignature().getBase())) {
            return STRING;
        }
        if ( type.getTypeSignature().getBase().equals(IntegerType.INTEGER.getTypeSignature().getBase())) {
            return INTEGER;
        }
        if ( type.getTypeSignature().getBase().equals(JsonType.JSON.getTypeSignature().getBase())) {
            return OBJECT;
        }
        throw new UnsupportedOperationException("Mapping of "+type+" to DataType not yet implemented");
    }


    public static <S> DataTypeValue<S> ofPrestoNativeValue(Type type, Object nativeValue, Class<S> targetClass) {
        Object obj = getJavaValue(type, nativeValue);
        Preconditions.checkArgument(targetClass.isAssignableFrom(obj.getClass()),"%s is not assignable from %s",targetClass,obj.getClass());
        //noinspection unchecked
        return of((S) obj, targetClass);
    }

    public static DataTypeValue<Object> ofObject(Object value) {
        return new DataTypeValue<>(value, Object.class);
    }


    @Nullable
    public T getValue() {
        return value;
    }

    public Class<T> getJavaType() {
        return javaType;
    }

    public DataType getDataType() {
        return supportedDataType.getDataType();
    }

    @Override
    public String toString() {
        return "DataTypeValue{" +
                "value=" + value +
                ", javaType=" + javaType +
                ", dataType=" + supportedDataType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataTypeValue<?> that = (DataTypeValue<?>) o;
        return Objects.equals(value, that.value) && javaType.equals(that.javaType) && supportedDataType == that.supportedDataType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, javaType, supportedDataType);
    }

    private static final Supplier<ObjectMapper> OBJECT_MAPPER_SUPPLIER = Suppliers.memoize(
            () -> new JsonObjectMapperProvider().get().enable(INDENT_OUTPUT))::get;

    public Optional<String> stringify() {
        if ( this.value == null ) return Optional.empty();

        String result;
        switch (this.supportedDataType.getDataType()) {
            case STRING:
            case BYTE:
            case INTEGER:
            case SHORT:
            case CHARACTER:
            case LONG:
                result = value.toString();
                break;
            case DOUBLE:
            case BIG_DECIMAL:
            case BIG_INTEGER:
            case BOOLEAN:
            case FLOAT:
            case DATE:
            case YEAR:
            case YEAR_MONTH:
            case LOCAL_TIME:
            case LOCAL_DATE:
            case UUID_TYPE:
            case LOCAL_DATE_TIME:
            case ZONED_DATE_TIME:
            case CLASS:
            case LOCALE:
            case ARRAY:
            case LIST:
            case MAP:
            case SET: {// Let Jackson do the work, so that we have consistent formatting
                final String json = JsonCodec.jsonCodec(this.javaType).toJson(this.value);
                result = cleanUpJson(json);
                break;
            }
            case OBJECT: { // Let Jackson/JodaBeans do the work, so that we have consistent formatting
                try {
                    String json = OBJECT_MAPPER_SUPPLIER.get().writerFor(this.value.getClass()).writeValueAsString(this.value);
                    result = cleanUpJson(json);
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException(e);
                }
                break;
            }
            case BYTE_ARRAY:
                result = Base64.getEncoder().encodeToString((byte[]) this.value);
                break;
            case BYTE_BUFFER:
                ByteBuffer bb = (ByteBuffer) this.value;
                byte[] b = new byte[bb.remaining()];
                try {
                    bb.get(b);
                } finally {
                    bb.rewind();
                }
                result = Base64.getEncoder().encodeToString(b);
                break;
            case NULL:
                result = null;
                break;
            default: throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR, "[Cyoda] "+this.supportedDataType + " not supported for stringifying");
        }
        return Optional.ofNullable(result);

    }

    private String cleanUpJson(String json) {
        String result;
        // We get additional quotes from jackson, if the field is an Optional
        if ( isSingleElementJson(json) || json.startsWith("\"")) {
            result = json.substring(1, json.length() - 1);
        } else {
            result = json;
        }
        return result;
    }

    private static boolean isSingleElementJson(String json) {
        if ( json.startsWith("{") ) {
            try {
                Map<String,?> map = OBJECT_MAPPER_SUPPLIER.get().reader().forType(Map.class).readValue(json);
                return map.size() == 1;
            } catch (JsonProcessingException e) {
                return false;
            }
        } else return false;
    }

    public Optional<ByteBuffer> encode() {
        if ( this.supportedDataType.getDataType() == BYTE_ARRAY) return Optional.ofNullable(this.value).map(it->ByteBuffer.wrap((byte[]) it));
        if ( this.supportedDataType.getDataType() == BYTE_BUFFER) return Optional.ofNullable((ByteBuffer) this.value);
        if ( this.supportedDataType.getDataType() == STRING) return Optional.of(
                ByteBuffer.wrap(Base64.getEncoder().encode(
                        Optional.ofNullable(this.value).map(it->it.toString().getBytes(StandardCharsets.UTF_8))
                                .orElse(new byte[0])
                        )
                )
        );
        throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR, "[Cyoda] "+this.supportedDataType + " not supported for encoding");
    }


    public static TypeSignature toPrestoTypeSignature(DataType dataType) {
        switch (dataType) {
            case BOOLEAN: return BooleanType.BOOLEAN.getTypeSignature();
            case BYTE: return TinyintType.TINYINT.getTypeSignature();
            case SHORT: return SmallintType.SMALLINT.getTypeSignature();
            case INTEGER: return IntegerType.INTEGER.getTypeSignature();
            case LONG: return BigintType.BIGINT.getTypeSignature();
            case FLOAT: return RealType.REAL.getTypeSignature();
            case DOUBLE: return DoubleType.DOUBLE.getTypeSignature();
            case STRING: return VarcharType.VARCHAR.getTypeSignature();
            case DATE: return DateType.DATE.getTypeSignature();
            case LOCAL_DATE_TIME: return TimestampType.TIMESTAMP.getTypeSignature();
            case LOCAL_DATE: return BigintType.BIGINT.getTypeSignature();
            case YEAR: return VarcharType.VARCHAR.getTypeSignature();
            case OBJECT: return JsonType.JSON.getTypeSignature();
            default: throw new UnsupportedOperationException(dataType + " Not yet done");
        }
    }

    // TODO: Refactor logic to convert to/from Presto natiave value behind an interface for each datatype.
    // It will be used here, in parseToLong and asSlice, and also in Predicate newComparisonPredicate(...) for each type.
    // Taken from Kudu TypeHelper
    public static Object getJavaValue(Type type, Object nativeValue) {
        // It  needs to mirror the logic in asSlice / parseToLong
        if (type instanceof VarcharType) {
            return ((Slice) nativeValue).toStringUtf8();
        } else if (type == TimestampType.TIMESTAMP) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli((Long) nativeValue), ZoneId.of("UTC"));
        } else if (type == BigintType.BIGINT) {
            return nativeValue;
        } else if (type == IntegerType.INTEGER) {
            return ((Long) nativeValue).intValue();
        } else if (type == SmallintType.SMALLINT) {
            return ((Long) nativeValue).shortValue();
        } else if (type == TinyintType.TINYINT) {
            return ((Long) nativeValue).byteValue();
        } else if ( type == DateType.DATE) {
            return LocalDate.ofEpochDay((Long) nativeValue);
        } else if (type == DoubleType.DOUBLE) {
            return longBitsToDouble(((Long) nativeValue));
        } else if (type == RealType.REAL) {
            // conversion can result in precision lost
            return intBitsToFloat(((Long) nativeValue).intValue());
        } else if (type == BooleanType.BOOLEAN) {
            return nativeValue;
        } else if (type instanceof VarbinaryType) {
            return ((Slice) nativeValue).toByteBuffer();
        } else if (type instanceof DecimalType) {
            return nativeValue;
        } else if (type.getTypeSignature().getBase().equals(StandardTypes.UUID)) {
            return UUID.fromString(((Slice) nativeValue).toStringUtf8());
        } else {
            throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "Back conversion not implemented for " + type);
        }
    }

    public Long parseToLong() {
        switch (supportedDataType.getDataType()) {
            case BYTE:
                return asByte().longValue();
            case SHORT:
                return asShort().longValue();
            case LONG:
                return asLong();
            case FLOAT:
                return asFloat().longValue();
            case DOUBLE:
                return asDouble().longValue();
            case INTEGER:
                return asInt().longValue();
            case BIG_DECIMAL:
                return asBigDecimal().longValue();
            case BIG_INTEGER:
                return asBigInteger().longValue();
            case BOOLEAN:
                return Boolean.TRUE.equals(asBoolean()) ? 1L : 0L;
            case LOCAL_DATE:
                return asLocalDate().toEpochDay();
            case LOCAL_DATE_TIME:
                return asLocalDateTime().toInstant(ZoneOffset.UTC).toEpochMilli();
            case DATE:
                return asDate().getTime();
            case ZONED_DATE_TIME:
                return asZonedDateTime().toInstant().toEpochMilli();
            default:
                throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR,"Cannot retrieve long for " + supportedDataType);
        }
    }

    public Slice asSlice(Type type) {
        if ( isNull() ) return EMPTY_SLICE;
        if (type instanceof VarbinaryType) {
            return wrappedBuffer(encode().orElse(ByteBuffer.wrap(new byte[0])));
        }
        else if (type instanceof DecimalType) {
            return Decimals.encodeScaledValue(asBigDecimal());
        }
        else if (type instanceof VarcharType) {
            return stringify().map(Slices::utf8Slice).orElse(EMPTY_SLICE);
        }
        else if (type instanceof JsonType) {
            return stringify().map(Slices::utf8Slice).orElse(EMPTY_SLICE);
        }
        else if (type.getTypeSignature().getBase().equals(UuidType.UUID.getTypeSignature().getBase())) {
            return wrappedBuffer(uuidToBytes(asUUID()));
        }
        else {
            throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR, "Creating Slices not supported for type " + type);
        }

    }

    // Only useful for Trino. Presto wants a String!
    public static byte[] uuidToBytes(UUID uuid)
    {
        return ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }

    public Long asTimestampMillis() {
        switch (supportedDataType.getDataType()) {
            case LOCAL_DATE_TIME:
                return ((LocalDateTime) value).toInstant(ZoneOffset.UTC).toEpochMilli();
            case DATE:
                return ((Date) value).getTime();
            case ZONED_DATE_TIME:
                return ((ZonedDateTime) value).toInstant().toEpochMilli();
            default:
                throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR, supportedDataType + " is not a TimeStamp type");
        }
    }

    public String asString() {
        return getCast();
    }

    @SuppressWarnings("unchecked")
    private <S> S getCast() {
        try {
            return (S) supportedDataType.getClazz().cast(value);
        } catch (Exception e) {
            throw wrongDataTypeException(supportedDataType.getDataType());
        }
    }

    public Byte asByte() {
        return getCast();
    }

    public Integer asInt() {
        return getCast();
    }

    public Short asShort() {
        return getCast();
    }

    public Character asChar() {
        return getCast();
    }

    public Long asLong() {
        return getCast();
    }

    public Double asDouble() {
        return getCast();
    }

    public BigDecimal asBigDecimal() {
        return getCast();
    }

    public BigInteger asBigInteger() {
        return getCast();
    }

    public Boolean asBoolean() {
        return getCast();
    }

    public Float asFloat() {
        return getCast();
    }

    public Date asDate() {
        return getCast();
    }

    public Year asYear() {
        return getCast();
    }

    public YearMonth asYearMonth() {
        return getCast();
    }

    public LocalTime asLocalTime() {
        return getCast();
    }

    public LocalDate asLocalDate() {
        return getCast();
    }

    public UUID asUUID() {
        return getCast();
    }

    public LocalDateTime asLocalDateTime() {
        return getCast();
    }

    public ZonedDateTime asZonedDateTime() {
        return getCast();
    }

    public Class<?> asClass() {
        return getCast();
    }

    public Locale asLocale() {
        return getCast();
    }

    public <S> S[] asArray() {
        return getCast();
    }

    public <S> List<S> asList() {
        return getCast();
    }

    public <K,V> Map<K,V> asMap() {
        return getCast();
    }

    public <S> Set<S> asSet() {
        return getCast();
    }

    public byte[] asByteArray() {
        return getCast();
    }

    public ByteBuffer asByteBuffer() {
        ByteBuffer byteBuffer = getCast();
        byteBuffer.rewind();
        return byteBuffer;
    }

    public Number asNumber() {
        return getCast();
    }

    private PrestoException wrongDataTypeException(DataType required) {
        return new PrestoException(CYODA_INCORRECT_TYPE_ERROR,
                format("DataType %s is wrong. Need %s", this.supportedDataType, required));
    }

    public boolean isNull() {
        return value == null || supportedDataType.getDataType() == NULL;
    }

    private static byte[] getByteArray(ByteBuffer byteBuffer) {
        try {
            byte[] bb = new byte[byteBuffer.remaining()];
            byteBuffer.get(bb);
            return bb;
        } finally {
            byteBuffer.rewind();
        }
    }

    private static final Comparator<byte[]> COMPARATOR = UnsignedBytes.lexicographicalComparator();
    @SuppressWarnings("unchecked")
    @Override
    public int compareTo(DataTypeValue<T> o) {
        Preconditions.checkNotNull(o.value,"value must not be null");
        Preconditions.checkNotNull(this.value,"cannot compare with null values");
        Preconditions.checkArgument(this.value instanceof Comparable,"Cannot compare things that are not comparable");
        Preconditions.checkArgument(o.value instanceof Comparable,"Cannot compare things that are not comparable");
        if ( this.value instanceof ByteBuffer ) {
            return COMPARATOR.compare(getByteArray((ByteBuffer) (this.value)),getByteArray((ByteBuffer)o.value));
        }
        return ((Comparable<T>) this.value).compareTo(o.value);
    }

    public <S> DataTypeValue<S> as(Class<S> clazz, Function<T,S> convert) {
        if ( this.value != null && !clazz.isAssignableFrom(this.value.getClass())) {
            throw new IllegalArgumentException("value is not a "+clazz.getName());
        }
        return new DataTypeValue<>(convert.apply(this.value),clazz);
    }
}
