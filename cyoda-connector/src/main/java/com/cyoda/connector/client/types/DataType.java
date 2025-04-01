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

package com.cyoda.connector.client.types;

import com.cyoda.connector.client.logic.PredicatePushdownController;
import com.cyoda.connector.client.logic.converters.impl.BigDecimalPrestoValueConverter;
import com.cyoda.connector.logging.SupplierLogger;
import io.trino.spi.type.StandardTypes;
import io.trino.spi.type.TypeSignatureParameter;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This might help to figure out what the correct connector type is: io.trino.client.FixJsonDataUtils
 */
public enum DataType implements IDataType {
    STRING          (String.class,          StandardTypes.VARCHAR,      true, 0),
    BYTE            (Byte.class,            StandardTypes.TINYINT,      true, 0),
    DOUBLE          (Double.class,          StandardTypes.DOUBLE,       true, 0),
    INTEGER         (Integer.class,         StandardTypes.INTEGER,      true, 0),
    BIG_DECIMAL     (BigDecimal.class,      StandardTypes.DECIMAL,      true, 0,
            BigDecimalPrestoValueConverter.PRECISION, BigDecimalPrestoValueConverter.SCALE),
    UNBOUND_DECIMAL (BigDecimal.class,      StandardTypes.VARCHAR,      true, 0),
    BIG_INTEGER     (BigInteger.class,      StandardTypes.DECIMAL,      true, 0),
    UNBOUND_INTEGER (BigInteger.class,      StandardTypes.VARCHAR,      true, 0),
    BOOLEAN         (Boolean.class,         StandardTypes.BOOLEAN,      true, 0),
    LOCAL_DATE      (LocalDate.class,       StandardTypes.DATE,         true, 0),
    LOCAL_DATE_TIME (LocalDateTime.class,   StandardTypes.TIMESTAMP,    true, 0),
    SHORT           (Short.class,           StandardTypes.SMALLINT,     true, 0),
    CHARACTER       (Character.class,       StandardTypes.CHAR,         true, 0),
    LONG            (Long.class,            StandardTypes.BIGINT,       true, 0),
    FLOAT           (Float.class,           StandardTypes.REAL,         true, 0),
    DATE            (Date.class,            StandardTypes.TIMESTAMP,    true, 0),
    ZONED_DATE_TIME (ZonedDateTime.class,   StandardTypes.TIMESTAMP_WITH_TIME_ZONE, true, 0),
    YEAR            (Year.class,            StandardTypes.INTEGER,      true, 0),
    YEAR_MONTH      (YearMonth.class,       StandardTypes.DATE,         true, 0),
    LOCAL_TIME      (LocalTime.class,       StandardTypes.TIME,         true, 0, 9),
    UUID_TYPE       (UUID.class,            StandardTypes.UUID,         true, 0),
    TIME_UUID_TYPE  (UUID.class,            StandardTypes.UUID,         true, 0),
    BYTE_ARRAY      (byte[].class,          StandardTypes.VARBINARY,    false, 0),
    BYTE_BUFFER     (ByteBuffer.class,      StandardTypes.VARBINARY,    false, 0),
    LOCALE          (Locale.class,          StandardTypes.VARCHAR,      false, 0),
    OBJECT          (Object.class,          StandardTypes.JSON,         false, 0), //Unsure. We will transform these to Json strings.
    LIST            (List.class,            StandardTypes.ARRAY,        true, 1),
    MAP             (Map.class,             StandardTypes.MAP,          false, 2),
    SET             (Set.class,             StandardTypes.ARRAY,        false, 1);


    private static final SupplierLogger LOG = SupplierLogger.get(DataType.class);

    private final Class<?> javaType;
    private final String typeString;
    private final boolean comparable;
    private final int typeParametersCount;

    private final List<TypeSignatureParameter> staticParams;


    DataType(Class<?> javaType, String typeString, boolean comparable, int typeParametersCount, long...staticParams) {
        this.javaType = javaType;
        this.typeString = typeString;
        this.comparable = comparable;
        this.typeParametersCount = typeParametersCount;
        this.staticParams = Arrays.stream(staticParams).mapToObj(TypeSignatureParameter::numericParameter).collect(Collectors.toList());
    }
    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    public String getTypeString() {
        return typeString;
    }

    public int getTypeParametersCount() {
        return typeParametersCount;
    }

    public List<TypeSignatureParameter> getStaticParams() {
        return staticParams;
    }

    public boolean isComparable() {
        return comparable;
    }
    public PredicatePushdownController getPushDownController(){
        return isComparable() ? PredicatePushdownController.FULL_PUSHDOWN : PredicatePushdownController.DISABLE_PUSHDOWN;
    }

    public static final Map<Class<?>, DataType> objectClassToDataType = ImmutableMap.copyOf(
            Arrays.stream(DataType.values())
                    // for report BigDecimal and BigInteger should prioritize unbound types, because there is no way to ensure right capacity for that application
                    .filter(it -> it.javaType != null && it != BIG_DECIMAL && it != BIG_INTEGER)
                    .filter(it -> it != TIME_UUID_TYPE)
                    .collect(Collectors.toMap(it -> it.javaType, it -> it))
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

    //to extract metadata from joda .meta()
    public static @Nonnull DataType fromClassFSToObject(Class<?> clazz, String columnName) {
        DataType res = classToDataType.get(clazz);
        if (res != null)
            return res;
        else {
            LOG.warn(String.format("Warning for field \"%s\": Unable to determine DataType for class %s. Returning OBJECT.",
                    columnName, clazz.getName()));
            return OBJECT;
        }
    }

    //to extract metadata from report config
    public static @Nonnull DataType fromClassFSToString(Class<?> clazz, String columnName) {
        DataType res = classToDataType.get(clazz);
        if (res != null)
            return res;
        else {
            LOG.info(String.format("Field \"%s\": No native support for class %s. Processing as String",
                    columnName, clazz.getName()));
            return STRING;
        }
    }


}

