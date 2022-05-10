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
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.spi.PrestoException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import javax.xml.crypto.Data;
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

    public Serializable parseToSerializable(String input) {
        switch (this) {
            case STRING:
                return input;
            case DOUBLE:
                return Double.parseDouble(input);
            case INTEGER:
                return Integer.parseInt(input);
            case LOCAL_DATE:
                return LocalDate.parse(input);
            case LOCAL_DATE_TIME:
                return LocalDateTime.parse(input);
            case ZONED_DATE_TIME:
                return ZonedDateTime.parse(input);
            case BOOLEAN:
                return Boolean.parseBoolean(input);
            case BIG_DECIMAL:
                return new BigDecimal(input);
            case BIG_INTEGER:
                return new BigInteger(input);
            case UUID_TYPE:
                return UUID.fromString(input);
            default:
                throw new PrestoException(CYODA_INCORRECT_TYPE_ERROR ,this + " cannot be parsed to a Serializable ");
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

    public static final List<DataType> numberTypes = ImmutableList.copyOf(
            Arrays.stream(DataType.values())
                    .filter(it -> it.javaType != null && Number.class.isAssignableFrom(it.javaType))
                    .collect(Collectors.toList())
    );

    private static final List<String> stringValues = Arrays.stream(DataType.values()).map(Enum::toString).collect(Collectors.toList());

    public static boolean isvalidDataTypeString(String str) {
        return stringValues.contains(str);
    }

}

