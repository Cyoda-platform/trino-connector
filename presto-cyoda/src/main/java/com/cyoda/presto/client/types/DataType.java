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

import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.types.impl.ArrayDataType;
import com.cyoda.presto.client.types.impl.BigDecimalDataType;
import com.cyoda.presto.client.types.impl.BigIntegerDataType;
import com.cyoda.presto.client.types.impl.BooleanDataType;
import com.cyoda.presto.client.types.impl.ByteArrayDataType;
import com.cyoda.presto.client.types.impl.ByteBufferDataType;
import com.cyoda.presto.client.types.impl.ByteDataType;
import com.cyoda.presto.client.types.impl.DateDataType;
import com.cyoda.presto.client.types.impl.DoubleDataType;
import com.cyoda.presto.client.types.impl.FloatDataType;
import com.cyoda.presto.client.types.impl.IntegerDataType;
import com.cyoda.presto.client.types.impl.ListDataType;
import com.cyoda.presto.client.types.impl.LocalDateDataType;
import com.cyoda.presto.client.types.impl.LocalDateTimeDataType;
import com.cyoda.presto.client.types.impl.LocalTimeDataType;
import com.cyoda.presto.client.types.impl.LongDataType;
import com.cyoda.presto.client.types.impl.MapDataType;
import com.cyoda.presto.client.types.impl.ObjectDataType;
import com.cyoda.presto.client.types.impl.SetDataType;
import com.cyoda.presto.client.types.impl.ShortDataType;
import com.cyoda.presto.client.types.impl.StringDataType;
import com.cyoda.presto.client.types.impl.UUIDDataType;
import com.cyoda.presto.client.types.impl.YearDataType;
import com.cyoda.presto.client.types.impl.YearMonthDataType;
import com.cyoda.presto.client.types.impl.ZonedDateTimeDataType;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.spi.PrestoException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cyoda.presto.CyodaErrorCode.CYODA_INCORRECT_TYPE_ERROR;

/**
 * This might help to figure out what the correct presto type is: com.facebook.presto.client.FixJsonDataUtils
 */
public enum DataType {
    STRING(String.class, StandardTypes.VARCHAR),
    BYTE(Byte.class, StandardTypes.TINYINT),
    DOUBLE(Double.class, StandardTypes.DOUBLE),
    INTEGER(Integer.class, StandardTypes.INTEGER),
    BIG_DECIMAL(BigDecimal.class, StandardTypes.DECIMAL),
    BIG_INTEGER(BigInteger.class, StandardTypes.BIGINT),
    BOOLEAN(Boolean.class,StandardTypes.BOOLEAN),
    LOCAL_DATE(LocalDate.class,StandardTypes.DATE),
    LOCAL_DATE_TIME(LocalDateTime.class,StandardTypes.TIMESTAMP),
    SHORT(Short.class,StandardTypes.SMALLINT),
    CHARACTER(Character.class,StandardTypes.CHAR),
    LONG(Long.class,StandardTypes.BIGINT),
    FLOAT(Float.class, StandardTypes.REAL),
    DATE(Date.class,StandardTypes.TIMESTAMP),
    ZONED_DATE_TIME(ZonedDateTime.class,StandardTypes.TIMESTAMP_WITH_TIME_ZONE),
    YEAR(Year.class,StandardTypes.INTEGER),
    YEAR_MONTH(YearMonth.class,StandardTypes.VARCHAR),
    LOCAL_TIME(LocalTime.class,StandardTypes.TIME), // Unsure
    UUID_TYPE(UUID.class,StandardTypes.VARCHAR),  // StandardType.UUID does not work. Presto wants a String. Trino supports UUID.
    BYTE_ARRAY(byte[].class,StandardTypes.VARBINARY),
    BYTE_BUFFER(ByteBuffer.class,StandardTypes.VARBINARY),
    CLASS(Class.class,StandardTypes.VARCHAR),
    LOCALE(Locale.class,StandardTypes.VARCHAR),
    NULL(null,null),
    OBJECT(Object.class,StandardTypes.JSON), //Unsure. We will transform these to Json strings.
    ARRAY(Object[].class,StandardTypes.ARRAY),
    LIST(List.class,StandardTypes.ARRAY),
    MAP(Map.class,StandardTypes.MAP),
    SET(Set.class,StandardTypes.ARRAY),
    ANY(Any.class,null); // Placeholder for anything. To differentiate from Object.


    private final Class<?> javaType;
    private final String typeString;


    DataType(Class<?> javaType, String typeString) {
        this.javaType = javaType;
        this.typeString = typeString;
    }


    public Class<?> getJavaType() {
        return javaType;
    }

    public String getTypeString() {
        return typeString;
    }

    public boolean isNumber() {
        return Number.class.isAssignableFrom(javaType);
    }

    public boolean isSerializable() {
        return Serializable.class.isAssignableFrom(javaType);
    }

    public boolean isBinary() {
        return this == BYTE_ARRAY || this == OBJECT ;
    }

    // TODO: Need unit test to assert we have a SupportedDataType for each DataType.
    @SuppressWarnings("unchecked")
    public <S> SupportedDataType<S> asSupported() {
        switch (this) {
            case LOCAL_DATE: return (SupportedDataType<S>) LocalDateDataType.INSTANCE;
            case LOCAL_DATE_TIME: return (SupportedDataType<S>) LocalDateTimeDataType.INSTANCE;
            case LOCAL_TIME: return (SupportedDataType<S>) LocalTimeDataType.INSTANCE;
            case ZONED_DATE_TIME: return (SupportedDataType<S>) ZonedDateTimeDataType.INSTANCE;
            case DATE: return (SupportedDataType<S>) DateDataType.INSTANCE;
            case STRING: return (SupportedDataType<S>) StringDataType.INSTANCE;
            case OBJECT: return (SupportedDataType<S>) ObjectDataType.INSTANCE;
            case YEAR: return (SupportedDataType<S>) YearDataType.INSTANCE;
            case YEAR_MONTH: return (SupportedDataType<S>) YearMonthDataType.INSTANCE;
            case BOOLEAN: return (SupportedDataType<S>) BooleanDataType.INSTANCE;
            case LONG: return (SupportedDataType<S>) LongDataType.INSTANCE;
            case INTEGER: return (SupportedDataType<S>) IntegerDataType.INSTANCE;
            case SHORT: return (SupportedDataType<S>) ShortDataType.INSTANCE;
            case FLOAT: return (SupportedDataType<S>) FloatDataType.INSTANCE;
            case DOUBLE: return (SupportedDataType<S>) DoubleDataType.INSTANCE;
            case BYTE: return (SupportedDataType<S>) ByteDataType.INSTANCE;
            case BYTE_BUFFER: return (SupportedDataType<S>) ByteBufferDataType.INSTANCE;
            case BYTE_ARRAY: return (SupportedDataType<S>) ByteArrayDataType.INSTANCE;
            case BIG_DECIMAL: return (SupportedDataType<S>) BigDecimalDataType.INSTANCE;
            case BIG_INTEGER: return (SupportedDataType<S>) BigIntegerDataType.INSTANCE;
            case UUID_TYPE: return (SupportedDataType<S>) UUIDDataType.INSTANCE;
            case LIST: return (SupportedDataType<S>) ListDataType.INSTANCE;
            case MAP: return (SupportedDataType<S>) MapDataType.INSTANCE;
            case SET: return (SupportedDataType<S>) SetDataType.INSTANCE;
            case ARRAY: return (SupportedDataType<S>) ArrayDataType.INSTANCE;
            default:
                throw new UnsupportedOperationException(this+ " Not yet implemented");
        }
    }

    public static final Map<Class<?>, DataType> objectClassToDataType = ImmutableMap.copyOf(
            Arrays.stream(DataType.values()).filter(it -> it.javaType != null).collect(Collectors.toMap(it -> it.javaType, it -> it))
    );

    public static final Map<Class<?>, DataType> primitiveClassToDataType = ImmutableMap.<Class<?>, DataType>builder()
            .put(char.class,CHARACTER)
            .put(boolean.class,BOOLEAN)
            .put(byte.class,BYTE)
            .put(short.class,SHORT)
            .put(int.class,INTEGER)
            .put(long.class,LONG)
            .put(float.class,FLOAT)
            .put(double.class,DOUBLE)
            .build();

    private static final Map<Class<?>, DataType> classToDataType = ImmutableMap.<Class<?>, DataType>builder()
            .putAll(objectClassToDataType)
            .putAll(primitiveClassToDataType)
            .build();

    public static DataType dataTypeFromClass(Class<?> clazz) {
        return Optional.ofNullable(classToDataType.get(clazz))
                .orElse(Arrays.stream(DataType.values())
                        .filter(it -> it.javaType != null)
                        .filter(it->it.javaType.isAssignableFrom(clazz))
                        .findAny()
                        .orElse(null)
                );
    }

    public static Optional<DataType> fromClass(Class<?> clazz) {
       return Optional.ofNullable(classToDataType.get(clazz));
    }

    public static final Set<String> supportedPrestoTypes = ImmutableSet.copyOf(
            Arrays.stream(DataType.values())
                    .map(DataType::getTypeString)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
    );

    private static List<DataType> integerBasedTypes = ImmutableList.<DataType>builder()
            .add(BYTE)
            .add(SHORT)
            .add(INTEGER)
            .add(LONG)
            .add(YEAR)
            .add(YEAR_MONTH)
            .add(LOCAL_DATE_TIME)
            .add(ZONED_DATE_TIME)
            .add(LOCAL_DATE)
            .add(DATE)
            .build();

    // TODO: Theoretically, for performance purposes, we could add this as an attribute of DataType
    /**
     * Can this DataType be converted to/from a long ?
     * @param dataType to check
     * @return if this DataType can be converted to/from a long
     */
    public static boolean isIntType(DataType dataType) {
        return integerBasedTypes.contains(dataType);
    }

}

