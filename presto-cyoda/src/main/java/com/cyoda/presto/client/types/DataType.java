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
import com.cyoda.presto.client.logic.converters.impl.UUIDPrestoValueConverter;
import com.cyoda.presto.client.reporting.meta.ReportStatisticsApiHandler;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.type.BigintType;
import com.facebook.presto.common.type.BooleanType;
import com.facebook.presto.common.type.DateType;
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
import com.facebook.presto.common.type.TypeSignatureParameter;
import com.facebook.presto.common.type.VarcharType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import javax.annotation.Nonnull;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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

/**
 * This might help to figure out what the correct presto type is: com.facebook.presto.client.FixJsonDataUtils
 */
public enum DataType implements IDataType {
    STRING          (String.class,          StandardTypes.VARCHAR,      true, 0),
    BYTE            (Byte.class,            StandardTypes.TINYINT,      true, 0),
    DOUBLE          (Double.class,          StandardTypes.DOUBLE,       true, 0),
    INTEGER         (Integer.class,         StandardTypes.INTEGER,      true, 0),
    BIG_DECIMAL     (BigDecimal.class,      StandardTypes.DECIMAL,      true, 0,
            TypeSignatureParameter.of(38), TypeSignatureParameter.of(25)),
    BIG_INTEGER     (BigInteger.class,      StandardTypes.DECIMAL,      true, 0),
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
    LOCAL_TIME      (LocalTime.class,       StandardTypes.TIME,         true, 0), // Unsure
    UUID_TYPE       (UUID.class,            UUIDPrestoValueConverter.TYPE_STRING, true, 0),
    BYTE_ARRAY      (byte[].class,          StandardTypes.VARBINARY,    false, 0),
    BYTE_BUFFER     (ByteBuffer.class,      StandardTypes.VARBINARY,    false, 0),
    CLASS           (Class.class,           StandardTypes.VARCHAR,      false, 0),
    LOCALE          (Locale.class,          StandardTypes.VARCHAR,      false, 0),
//    NULL            (null,                  null, false, 0),
    OBJECT          (Object.class,          StandardTypes.JSON,         false, 0), //Unsure. We will transform these to Json strings.
//    ARRAY           (Object[].class,        StandardTypes.ARRAY,        false, 0),
    LIST            (List.class,            StandardTypes.ARRAY,        false, 1),
    MAP             (Map.class,             StandardTypes.MAP,          false, 2),
    SET             (Set.class,             StandardTypes.ARRAY,        false, 1);


    private static final SupplierLogger LOG = SupplierLogger.get(DataType.class);

    private final Class<?> javaType;
    private final String typeString;
    private final boolean comparable;
    private final int typeParametersCount;

    private final List<TypeSignatureParameter> staticParams;


    DataType(Class<?> javaType, String typeString, boolean comparable, int typeParametersCount, TypeSignatureParameter...staticParams) {
        this.javaType = javaType;
        this.typeString = typeString;
        this.comparable = comparable;
        this.typeParametersCount = typeParametersCount;
        this.staticParams = Arrays.asList(staticParams);
    }

    public static <T> Class<T> getJType(IDataType<T> dataType){
        return dataType.getJavaType();
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

    public static List<DataType> validateDataTypes(List<DataType> dataTypes, String columnName) {
        if (dataTypes == null || dataTypes.isEmpty())
            throw new IllegalArgumentException("No DataType for field " + columnName);
        if (dataTypes.size() - 1 != dataTypes.get(0).getTypeParametersCount())
            throw new IllegalArgumentException("Invalid DataType set (" + Arrays.toString(dataTypes.toArray()) + ") for field " + columnName);
        return dataTypes;
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

    public static Optional<DataType> fromClass(Class<?> clazz) {
       return Optional.ofNullable(classToDataType.get(clazz));
    }

    public static @Nonnull DataType fromClassExact(Class<?> clazz, String columnName) {
        return Optional.ofNullable(classToDataType.get(clazz)).orElseThrow(
                () -> new IllegalArgumentException(String.format("Error creating column \"%s\": Class [%s] is not supported.",
                        columnName, clazz.getName())));
    }

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

    public static List<DataType> fromReflectType(ParameterizedType type, String columnName){
        DataType mainType = DataType.fromClassExact((Class<?>) type.getRawType(), columnName);
        List<DataType> res = new ArrayList<>(mainType.getTypeParametersCount()+1);
        res.add(mainType);
        if (mainType.getTypeParametersCount() > 0) {
            java.lang.reflect.Type[] actualTypeArguments = type.getActualTypeArguments();
            if (actualTypeArguments.length < mainType.getTypeParametersCount())
                throw new IllegalArgumentException(String.format("Error creating column \"%s\": Not matching type arguments for type %s",
                        columnName, type));
            res.add(fromClassExact((Class<?>) actualTypeArguments[0],columnName));
            if (mainType.getTypeParametersCount() > 1)
                res.add(fromClassExact((Class<?>) actualTypeArguments[1],columnName));
        }
        return res;
    }


    public static DataType fromType(Type type) {
        if ( type.getTypeSignature().getBase().equals(VarcharType.VARCHAR.getTypeSignature().getBase())) {
            return STRING;
        }
        if ( type.getTypeSignature().getBase().equals(IntegerType.INTEGER.getTypeSignature().getBase())) {
            return INTEGER;
        }
        if ( type.getTypeSignature().getBase().equals(JsonType.JSON.getTypeSignature().getBase())) {
            return OBJECT;
        }
        if ( type.getTypeSignature().getBase().equals(BigDecimalType.BIG_DECIMAL_TYPE.getTypeSignature().getBase())) {
            return BIG_DECIMAL;
        }
        throw new UnsupportedOperationException("Mapping of "+type+" to DataType not yet implemented");
    }


//    public static final Set<String> supportedPrestoTypes = ImmutableSet.copyOf(
//            Arrays.stream(DataType.values())
//                    .map(DataType::getTypeString)
//                    .filter(Objects::nonNull)
//                    .collect(Collectors.toSet())
//    );

}

