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

package com.cyoda.presto.client;

import com.google.common.collect.ImmutableMap;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import java.util.UUID;
import java.util.stream.Collectors;

public enum DataType {
    STRING(String.class),
    BYTE(Byte.class),
    DOUBLE(Double.class),
    INTEGER(Integer.class),
    BIG_DECIMAL(BigDecimal.class),
    BIG_INTEGER(BigInteger.class),
    BOOLEAN(Boolean.class),
    LOCAL_DATE(LocalDate.class),
    LOCAL_DATE_TIME(LocalDateTime.class),
    SHORT(Short.class),
    CHARACTER(Character.class),
    LONG(Long.class),
    FLOAT(Float.class),
    DATE(Date.class),
    ZONED_DATE_TIME(ZonedDateTime.class),
    YEAR(Year.class),
    YEAR_MONTH(YearMonth.class),
    LOCAL_TIME(LocalTime.class),
    UUID_TYPE(UUID.class),
    BYTE_ARRAY(byte[].class),
    CLASS(Class.class),
    LOCALE(Locale.class),
    NULL(null),
    OBJECT(null),
    ARRAY(null),
    ARRAY_ELEMENT(null),
    ANY(null);

    private final Class<?> javaType;

    DataType(Class<?> javaType) {
        this.javaType = javaType;
    }

    public Serializable parseToSerializable(String input) {
        switch (this) {
            case STRING:
            case ANY:  return input;
            case DOUBLE: return Double.parseDouble(input);
            case INTEGER: return Integer.parseInt(input);
            case LOCAL_DATE: return LocalDate.parse(input);
            case LOCAL_DATE_TIME: return LocalDateTime.parse(input);
            case ZONED_DATE_TIME: return ZonedDateTime.parse(input);
            case BOOLEAN: return Boolean.parseBoolean(input);
            case BIG_DECIMAL: return new BigDecimal(input);
            case BIG_INTEGER: return new BigInteger(input);
            case UUID_TYPE: return UUID.fromString(input);
            default: throw new IllegalArgumentException(this+" cannot be parsed to a Serializable ");
        }
    }

    public static final Map<Class<?>, DataType> classToDataType = ImmutableMap.copyOf(
            Arrays.stream(DataType.values()).filter(it -> it.javaType != null).collect(Collectors.toMap(it->it.javaType, it->it))
    );

    private static final List<String> stringValues = Arrays.stream(DataType.values()).map(Enum::toString).collect(Collectors.toList());
    public static boolean isvalidDataTypeString(String str) {
        return stringValues.contains(str);
    }


}
