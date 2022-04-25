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
import com.facebook.presto.common.type.BigintType;
import com.facebook.presto.common.type.BooleanType;
import com.facebook.presto.common.type.DecimalType;
import com.facebook.presto.common.type.Decimals;
import com.facebook.presto.common.type.DoubleType;
import com.facebook.presto.common.type.IntegerType;
import com.facebook.presto.common.type.RealType;
import com.facebook.presto.common.type.SmallintType;
import com.facebook.presto.common.type.TimestampType;
import com.facebook.presto.common.type.TinyintType;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.VarbinaryType;
import com.facebook.presto.common.type.VarcharType;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.google.common.base.Preconditions;
import io.airlift.slice.Slice;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.cyoda.presto.CyodaErrorCode.CYODA_INCORRECT_TYPE_ERROR;
import static com.cyoda.presto.client.types.DataType.*;
import static io.airlift.slice.Slices.*;
import static java.lang.Float.intBitsToFloat;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class SupportedDataType<T> implements Comparable<SupportedDataType<T>> {
    public final T value;
    public final Class<T> javaType;
    public final DataType dataType;

    private SupportedDataType(T value, Class<T> javaType) {
        this.value = value;
        if (value != null && !javaType.isAssignableFrom(value.getClass())) {
            throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR,
                    format("Incompatible type. %s is not assignable from %s", javaType, value.getClass()));
        }
        this.javaType = javaType;
        final DataType fromJavaType = DataType.classToDataType.get(javaType);
        requireNonNull(fromJavaType, javaType + " not mapped as a DataType");
        this.dataType = fromJavaType;
    }

    public static <S> SupportedDataType<S> of(S value, Class<S> javaType) {
        return new SupportedDataType<>(value, javaType);
    }

    public static <S> SupportedDataType<S> ofPrestoNativeValue(Type type, Object nativeValue, Class<S> targetClass) {
        Object obj = getJavaValue(type, nativeValue);
        Preconditions.checkArgument(targetClass.isAssignableFrom(obj.getClass()));
        //noinspection unchecked
        return of((S) obj, targetClass);
    }

    public static SupportedDataType<Object> ofObject(Object value) {
        return new SupportedDataType<>(value, Object.class);
    }

    // Taken from Kudu TypeHelper
    public static Object getJavaValue(Type type, Object nativeValue) {
        if (type instanceof VarcharType) {
            return ((Slice) nativeValue).toStringUtf8();
        } else if (type == TimestampType.TIMESTAMP) {
            return ((Long) nativeValue) * 1000;
        } else if (type == BigintType.BIGINT) {
            return nativeValue;
        } else if (type == IntegerType.INTEGER) {
            return ((Long) nativeValue).intValue();
        } else if (type == SmallintType.SMALLINT) {
            return ((Long) nativeValue).shortValue();
        } else if (type == TinyintType.TINYINT) {
            return ((Long) nativeValue).byteValue();
        } else if (type == DoubleType.DOUBLE) {
            return nativeValue;
        } else if (type == RealType.REAL) {
            // conversion can result in precision lost
            return intBitsToFloat(((Long) nativeValue).intValue());
        } else if (type == BooleanType.BOOLEAN) {
            return nativeValue;
        } else if (type instanceof VarbinaryType) {
            return ((Slice) nativeValue).toByteBuffer();
        } else if (type instanceof DecimalType) {
            return nativeValue;
        } else {
            throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, "Back conversion not implemented for " + type);
        }
    }


    @Override
    public String toString() {
        return "SupportedDataType{" +
                "value=" + value +
                ", javaType=" + javaType +
                ", dataType=" + dataType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SupportedDataType<?> that = (SupportedDataType<?>) o;
        return Objects.equals(value, that.value) && javaType.equals(that.javaType) && dataType == that.dataType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, javaType, dataType);
    }

    public String stringify() {
        switch (this.dataType) {
            case STRING:
            case BYTE:
            case INTEGER:
            case SHORT:
            case CHARACTER:
            case LONG:
                return value.toString();
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
            case SET: // Let Jackson do the work, so that we have consistent formatting
                final String result = JsonCodec.jsonCodec(this.javaType).toJson(this.value);
                return result.substring(1,result.length()-1);
            case OBJECT: // Let Jackson do the work, so that we have consistent formatting
                return JsonCodec.jsonCodec(this.javaType).toJson(this.value);
            case BYTE_ARRAY:
                return Base64.getEncoder().encodeToString((byte[]) this.value);
            case BYTE_BUFFER:
                ByteBuffer bb = (ByteBuffer) this.value;
                byte[] b = new byte[bb.remaining()];
                bb.get(b);
                return Base64.getEncoder().encodeToString(b);
            case NULL: return "NULL";
            default: throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR, "[Cyoda] "+this.dataType + " not supported for stringifying");
        }

    }

    public ByteBuffer encode() {
        if ( this.dataType == BYTE_ARRAY) return ByteBuffer.wrap((byte[]) this.value);
        if ( this.dataType == BYTE_BUFFER) return (ByteBuffer) this.value;
        throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR, "[Cyoda] "+this.dataType + " not supported for encoding");
    }

    public Long parseToLong() {
        switch (dataType) {
            case BYTE:
                return asByte().longValue();
            case SHORT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case INTEGER:
            case BIG_DECIMAL:
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
                throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR,"Cannot retrieve long for " + dataType);
        }
    }

    public Slice asSlice(Type type) {
        if ( isNull() ) return EMPTY_SLICE;
        if (type instanceof VarbinaryType) {
            return (value == null) ? EMPTY_SLICE : wrappedBuffer(encode());
        }
        else if (type instanceof DecimalType) {
            return (value == null) ? EMPTY_SLICE : Decimals.encodeScaledValue(asBigDecimal());
        }
        else if (type instanceof VarcharType) {
            return (value == null) ? EMPTY_SLICE : utf8Slice(stringify());
        }
        else {
            throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR, "Creating Slices not supported for type " + type);
        }

    }

    public Long asTimestampMillis() {
        switch (dataType) {
            case LOCAL_DATE_TIME:
                return ((LocalDateTime) value).toInstant(ZoneOffset.UTC).toEpochMilli();
            case DATE:
                return ((Date) value).getTime();
            case ZONED_DATE_TIME:
                return ((ZonedDateTime) value).toInstant().toEpochMilli();
            default:
                throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR,dataType + " is not a TimeStamp type");
        }
    }

    public String asString() {
        return getCast();
    }

    @SuppressWarnings("unchecked")
    private <S> S getCast() {
        try {
            return (S) dataType.getJavaType().cast(value);
        } catch (Exception e) {
            throw wrongDataTypeException(dataType);
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
        return getCast();
    }

    public Number asNumber() {
        return getCast();
    }

    private PrestoException wrongDataTypeException(DataType required) {
        return new PrestoException(CYODA_INCORRECT_TYPE_ERROR,
                format("DataType %s is wrong. Need %s", this.dataType, required));
    }

    public boolean isNull() {
        return value == null || dataType == NULL;
    }


    @SuppressWarnings("unchecked")
    @Override
    public int compareTo(SupportedDataType<T> o) {
        return ((Comparable<T>) this.value).compareTo(o.value);
    }
}
