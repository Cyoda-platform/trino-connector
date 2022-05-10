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

package com.cyoda.presto.client.logic;

import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.client.types.SupportedDataType;
import com.cyoda.presto.client.util.DecimalUtil;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.common.type.BigintType;
import com.facebook.presto.common.type.BooleanType;
import com.facebook.presto.common.type.DateType;
import com.facebook.presto.common.type.DecimalType;
import com.facebook.presto.common.type.DoubleType;
import com.facebook.presto.common.type.IntegerType;
import com.facebook.presto.common.type.RealType;
import com.facebook.presto.common.type.SmallintType;
import com.facebook.presto.common.type.TimestampType;
import com.facebook.presto.common.type.TinyintType;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.VarbinaryType;
import com.facebook.presto.common.type.VarcharType;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.logic.Predicate.ComparisonOp.*;
import static com.cyoda.presto.client.logic.Predicate.PredicateType.EQUALITY;
import static com.cyoda.presto.client.logic.Predicate.PredicateType.RANGE;
import static java.lang.Double.doubleToLongBits;
import static java.lang.Float.floatToRawIntBits;
import static java.nio.charset.StandardCharsets.UTF_8;

public class PredicateTest {

    public static final String REQUEST_HANDLER_KEY = "mockRequestHandler";
    public static final String CONNECTOR_ID = "connectorId";

    private TypeManager mockTypeManager;

    private CyodaColumnHandle boolCol;
    private CyodaColumnHandle byteCol;
    private CyodaColumnHandle shortCol;
    private CyodaColumnHandle intCol;
    private CyodaColumnHandle longCol;
    private CyodaColumnHandle floatCol;
    private CyodaColumnHandle doubleCol;
    private CyodaColumnHandle stringCol;
    private CyodaColumnHandle binaryCol;
    private CyodaColumnHandle bigDecimalCol;
    private CyodaColumnHandle bigIntegerCol;
    private CyodaColumnHandle dateCol;
    private CyodaColumnHandle localDatetimeCol;

    private Predicate<Integer> intRange(Integer lower, Integer upper) {
        Preconditions.checkArgument(lower < upper);
        return new Predicate<>(RANGE, intCol, SupportedDataType.of(lower), SupportedDataType.of(upper));
    }

    private Predicate<Long> longRange(long lower, long upper) {
        Preconditions.checkArgument(lower < upper);
        return new Predicate<>(RANGE, intCol, SupportedDataType.of(lower), SupportedDataType.of(upper));
    }

    private Predicate<Integer> intInList(Integer... values) {
        SortedSet<SupportedDataType<Integer>> valueSet = toSupportedDataType(values);
        return Predicate.newInListPredicate(intCol, valueSet);
    }

    private Predicate<Long> longInList(Long... values) {
        SortedSet<SupportedDataType<Long>> valueSet = toSupportedDataType(values);
        return Predicate.newInListPredicate(intCol, valueSet);
    }

    private Predicate<Boolean> boolInList(Boolean... values) {
        SortedSet<SupportedDataType<Boolean>> valueSet = toSupportedDataType(values);
        return Predicate.newInListPredicate(boolCol, valueSet);
    }

    private Predicate<String> stringInList(String... values) {
        SortedSet<SupportedDataType<String >> valueSet = toSupportedDataType(values);
        return Predicate.newInListPredicate(stringCol, valueSet);
    }

    @BeforeMethod
    public void setup() {

        int pos = 0;
        boolCol = new CyodaColumnHandle(CONNECTOR_ID,"bool", BooleanType.BOOLEAN, DataType.BOOLEAN,pos++, REQUEST_HANDLER_KEY,false);
        byteCol = new CyodaColumnHandle(CONNECTOR_ID,"byte", TinyintType.TINYINT,DataType.BYTE,pos++, REQUEST_HANDLER_KEY,false);
        shortCol = new CyodaColumnHandle(CONNECTOR_ID,"short", SmallintType.SMALLINT,DataType.SHORT, pos++, REQUEST_HANDLER_KEY,false);
        intCol = new CyodaColumnHandle(CONNECTOR_ID,"int", IntegerType.INTEGER, DataType.INTEGER, pos++, REQUEST_HANDLER_KEY,false);

        longCol = new CyodaColumnHandle(CONNECTOR_ID,"long", DecimalType.createDecimalType(), DataType.LONG, pos++, REQUEST_HANDLER_KEY,false);

        floatCol = new CyodaColumnHandle(CONNECTOR_ID,"float", RealType.REAL, DataType.FLOAT,pos++, REQUEST_HANDLER_KEY,false);
        doubleCol = new CyodaColumnHandle(CONNECTOR_ID,"double", DoubleType.DOUBLE, DataType.DOUBLE,pos++, REQUEST_HANDLER_KEY,false);
        stringCol = new CyodaColumnHandle(CONNECTOR_ID,"string", VarcharType.VARCHAR, DataType.STRING,pos++, REQUEST_HANDLER_KEY);
        binaryCol = new CyodaColumnHandle(CONNECTOR_ID,"binary", VarbinaryType.VARBINARY, DataType.BYTE_ARRAY,pos++, REQUEST_HANDLER_KEY);

        bigDecimalCol = new CyodaColumnHandle(CONNECTOR_ID,"bigDecimal", DecimalType.createDecimalType(), DataType.BIG_DECIMAL,pos++, REQUEST_HANDLER_KEY);

        bigIntegerCol = new CyodaColumnHandle(CONNECTOR_ID,"bigInt", BigintType.BIGINT, DataType.BIG_INTEGER,pos++, REQUEST_HANDLER_KEY);
        localDatetimeCol = new CyodaColumnHandle(CONNECTOR_ID, "localDatetime", TimestampType.TIMESTAMP, DataType.LOCAL_DATE_TIME,pos++,REQUEST_HANDLER_KEY);
        dateCol = new CyodaColumnHandle(CONNECTOR_ID,"date", DateType.DATE, DataType.LOCAL_DATE,pos, REQUEST_HANDLER_KEY);
    }

    private <T extends Comparable<T>> void testMerge(Predicate<T> a,
                           Predicate<T> b,
                           Predicate<T> expected) {

        Assert.assertEquals(expected, a.merge(b));
        Assert.assertEquals(expected, b.merge(a));
    }



    /**
     * Tests merges on all types of integer predicates.
     */
    @Test
    public void testMergeInt() {

        // Equality + Equality
        //--------------------

        // |
        // |
        // =
        // |
        testMerge(Predicate.newComparisonPredicate(intCol, EQUAL, 0),
                Predicate.newComparisonPredicate(intCol, EQUAL, 0),
                Predicate.newComparisonPredicate(intCol, EQUAL, 0));
        // |
        //  |
        // =
        // None
        testMerge(Predicate.newComparisonPredicate(intCol, EQUAL, 0),
                Predicate.newComparisonPredicate(intCol, EQUAL, 1),
                Predicate.none(intCol));

        // Range + Equality
        //--------------------

        // [-------->
        //      |
        // =
        //      |
        testMerge(Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                Predicate.newComparisonPredicate(intCol, EQUAL, 10),
                Predicate.newComparisonPredicate(intCol, EQUAL, 10));

        //    [-------->
        //  |
        // =
        // None
        testMerge(Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 10),
                Predicate.newComparisonPredicate(intCol, EQUAL, 0),
                Predicate.none(intCol));

        // <--------)
        //      |
        // =
        //      |
        testMerge(Predicate.newComparisonPredicate(intCol, LESS, 10),
                Predicate.newComparisonPredicate(intCol, EQUAL, 5),
                Predicate.newComparisonPredicate(intCol, EQUAL, 5));

        // <--------)
        //            |
        // =
        // None
        testMerge(Predicate.newComparisonPredicate(intCol, LESS, 0),
                Predicate.newComparisonPredicate(intCol, EQUAL, 10),
                Predicate.none(intCol));

        // Unbounded Range + Unbounded Range
        //--------------------

        // [--------> AND
        // [-------->
        // =
        // [-------->

        testMerge(Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 0));

        // [--------> AND
        //    [----->
        // =
        //    [----->
        testMerge(Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 5));

        // <--------) AND
        // <--------)
        // =
        // <--------)

        testMerge(Predicate.newComparisonPredicate(intCol, LESS, 0),
                Predicate.newComparisonPredicate(intCol, LESS, 0),
                Predicate.newComparisonPredicate(intCol, LESS, 0));

        // <--------) AND
        // <----)
        // =
        // <----)

        testMerge(Predicate.newComparisonPredicate(intCol, LESS, 0),
                Predicate.newComparisonPredicate(intCol, LESS, -10),
                Predicate.newComparisonPredicate(intCol, LESS, -10));

        //    [--------> AND
        // <-------)
        // =
        //    [----)
        testMerge(Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                Predicate.newComparisonPredicate(intCol, LESS, 10),
                intRange(0, 10));

        //     [-----> AND
        // <----)
        // =
        //     |
        testMerge(Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                Predicate.newComparisonPredicate(intCol, LESS, 6),
                Predicate.newComparisonPredicate(intCol, EQUAL, 5));

        //     [-----> AND
        // <---)
        // =
        // None
        testMerge(Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                Predicate.newComparisonPredicate(intCol, LESS, 5),
                Predicate.none(intCol));

        //       [-----> AND
        // <---)
        // =
        // None
        testMerge(Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                Predicate.newComparisonPredicate(intCol, LESS, 3),
                Predicate.none(intCol));

        // Range + Range
        //--------------------

        // [--------) AND
        // [--------)
        // =
        // [--------)

        testMerge(intRange(0,10),
                intRange(0,10),
                intRange(0,10));

        // [--------) AND
        // [----)
        // =
        // [----)
        testMerge(intRange(0, 10),
                intRange(0, 5),
                intRange(0, 5));

        // [--------) AND
        //   [----)
        // =
        //   [----)
        testMerge(intRange(0, 10),
                intRange(3, 8),
                intRange(3, 8));

        // [-----) AND
        //   [------)
        // =
        //   [---)
        testMerge(intRange(0, 8),
                intRange(3, 10),
                intRange(3, 8));
        // [--) AND
        //    [---)
        // =
        // None
        testMerge(intRange(0, 5),
                intRange(5, 10),
                Predicate.none(intCol));

        // [--) AND
        //       [---)
        // =
        // None
        testMerge(intRange(0, 3),
                intRange(5, 10),
                Predicate.none(intCol));

        // Lower Bound + Range
        //--------------------

        // [------------>
        //       [---)
        // =
        //       [---)
        testMerge(Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                intRange(5, 10),
                intRange(5, 10));

        // [------------>
        // [--------)
        // =
        // [--------)
        testMerge(Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                intRange(5, 10),
                intRange(5, 10));

        //      [------------>
        // [--------)
        // =
        //      [---)
        testMerge(Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                intRange(0, 10),
                intRange(5, 10));

        //          [------->
        // [-----)
        // =
        // None
        testMerge(Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 10),
                intRange(0, 5),
                Predicate.none(intCol));

        // Upper Bound + Range
        //--------------------

        // <------------)
        //       [---)
        // =
        //       [---)
        testMerge(Predicate.newComparisonPredicate(intCol, LESS, 10),
                intRange(3, 8),
                intRange(3, 8));

        // <------------)
        //     [--------)
        // =
        //     [--------)
        testMerge(Predicate.newComparisonPredicate(intCol, LESS, 10),
                intRange(5, 10),
                intRange(5, 10));


        // <------------)
        //         [--------)
        // =
        //         [----)
        testMerge(Predicate.newComparisonPredicate(intCol, LESS, 5),
                intRange(0, 10),
                intRange(0, 5));

        // Range + Equality
        //--------------------

        //   [---) AND
        // |
        // =
        // None
        testMerge(intRange(3, 5),
                Predicate.newComparisonPredicate(intCol, EQUAL, 1),
                Predicate.none(intCol));

        // [---) AND
        // |
        // =
        // |
        testMerge(intRange(0, 5),
                Predicate.newComparisonPredicate(intCol, EQUAL, 0),
                Predicate.newComparisonPredicate(intCol, EQUAL, 0));

        // [---) AND
        //   |
        // =
        //   |
        testMerge(intRange(0, 5),
                Predicate.newComparisonPredicate(intCol, EQUAL, 3),
                Predicate.newComparisonPredicate(intCol, EQUAL, 3));

        // [---) AND
        //     |
        // =
        // None
        testMerge(intRange(0, 5),
                Predicate.newComparisonPredicate(intCol, EQUAL, 5),
                Predicate.none(intCol));

        // [---) AND
        //       |
        // =
        // None
        testMerge(intRange(0, 5),
                Predicate.newComparisonPredicate(intCol, EQUAL, 7),
                Predicate.none(intCol));

        // IN list + IN list
        //--------------------

        // | | |
        //   | | |
        testMerge(intInList(0, 10, 20),
                intInList(20, 10, 20, 30),
                intInList(10, 20));

        // |   |
        //    | |
        testMerge(intInList(0, 20),
                intInList(15, 30),
                Predicate.none(intCol));

        // IN list + NOT NULL
        //--------------------

        testMerge(intInList(10),
                Predicate.newIsNotNullPredicate(intCol),
                Predicate.newComparisonPredicate(intCol, EQUAL, 10));

        testMerge(intInList(10, -100),
                Predicate.newIsNotNullPredicate(intCol),
                intInList(-100, 10));

        // IN list + Equality
        //--------------------

        // | | |
        //   |
        // =
        //   |
        testMerge(intInList(0, 10, 20),
                Predicate.newComparisonPredicate(intCol, EQUAL, 10),
                Predicate.newComparisonPredicate(intCol, EQUAL, 10));

        // | | |
        //       |
        // =
        // none
        testMerge(intInList(0, 10, 20),
                Predicate.newComparisonPredicate(intCol, EQUAL, 30),
                Predicate.none(intCol));

        // IN list + Range
        //--------------------

        // | | | | |
        //   [---)
        // =
        //   | |
        testMerge(intInList(0, 10, 20, 30, 40),
                intRange(10, 30),
                intInList(10, 20));

        // | |   | |
        //    [--)
        // =
        // none
        testMerge(intInList(0, 10, 20, 30),
                intRange(25, 30),
                Predicate.none(intCol));

        // | | | |
        //    [------>
        // =
        //   | |
        testMerge(intInList(0, 10, 20, 30),
                Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 15),
                intInList(20, 30));

        // | | |
        //    [------>
        // =
        //     |
        testMerge(intInList(0, 10, 20),
                Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 15),
                Predicate.newComparisonPredicate(intCol, EQUAL, 20));

        // | |
        //    [------>
        // =
        // none
        testMerge(intInList(0, 10),
                Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 15),
                Predicate.none(intCol));

        // | | | |
        // <--)
        // =
        // | |
        testMerge(intInList(0, 10, 20, 30),
                Predicate.newComparisonPredicate(intCol, LESS, 15),
                intInList(0, 10));

        // |  | |
        // <--)
        // =
        // |
        testMerge(intInList(0, 10, 20),
                Predicate.newComparisonPredicate(intCol, LESS, 10),
                Predicate.newComparisonPredicate(intCol, EQUAL, 0));

        //      | |
        // <--)
        // =
        // none
        testMerge(intInList(10, 20),
                Predicate.newComparisonPredicate(intCol, LESS, 5),
                Predicate.none(intCol));

        // None
        //--------------------

        // None AND
        // [---->
        // =
        // None
        testMerge(Predicate.none(intCol),
                Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                Predicate.none(intCol));

        // None AND
        // <----)
        // =
        // None
        testMerge(Predicate.none(intCol),
                Predicate.newComparisonPredicate(intCol, LESS, 0),
                Predicate.none(intCol));

        // None AND
        // [----)
        // =
        // None
        testMerge(Predicate.none(intCol),
                intRange(3, 7),
                Predicate.none(intCol));

        // None AND
        //  |
        // =
        // None
        testMerge(Predicate.none(intCol),
                Predicate.newComparisonPredicate(intCol, EQUAL, 5),
                Predicate.none(intCol));

        // None AND
        // None
        // =
        // None
        testMerge(Predicate.<Integer>none(intCol),
                Predicate.<Integer>none(intCol),
                Predicate.<Integer>none(intCol));

        // IS NOT NULL
        //--------------------

        // IS NOT NULL AND
        // NONE
        // =
        // NONE
        testMerge(Predicate.newIsNotNullPredicate(intCol),
                Predicate.<Integer>none(intCol),
                Predicate.<Integer>none(intCol));

        // IS NOT NULL AND
        // IS NULL
        // =
        // NONE
        testMerge(Predicate.newIsNotNullPredicate(intCol),
                Predicate.newIsNullPredicate(intCol),
                Predicate.<Integer>none(intCol));

        // IS NOT NULL AND
        // IS NOT NULL
        // =
        // IS NOT NULL
        testMerge(Predicate.<Integer>newIsNotNullPredicate(intCol),
                Predicate.newIsNotNullPredicate(intCol),
                Predicate.newIsNotNullPredicate(intCol));

        // IS NOT NULL AND
        // |
        // =
        // |
        testMerge(Predicate.newIsNotNullPredicate(intCol),
                Predicate.newComparisonPredicate(intCol, EQUAL, 5),
                Predicate.newComparisonPredicate(intCol, EQUAL, 5));

        // IS NOT NULL AND
        // [------->
        // =
        // [------->
        testMerge(Predicate.newIsNotNullPredicate(intCol),
                Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 5));

        // IS NOT NULL AND
        // <---------)
        // =
        // <---------)
        testMerge(Predicate.newIsNotNullPredicate(intCol),
                Predicate.newComparisonPredicate(intCol, LESS, 5),
                Predicate.newComparisonPredicate(intCol, LESS, 5));

        // IS NOT NULL AND
        // [-------)
        // =
        // [-------)
        testMerge(Predicate.newIsNotNullPredicate(intCol),
                intRange(0, 12),
                intRange(0, 12));


        // IS NOT NULL AND
        // |   |   |
        // =
        // |   |   |
        testMerge(Predicate.newIsNotNullPredicate(intCol),
                intInList(0, 10, 20),
                intInList(0, 10, 20));

        // IS NULL
        //--------------------

        // IS NULL AND
        // NONE
        // =
        // NONE
        testMerge(Predicate.<Integer>newIsNullPredicate(intCol),
                Predicate.none(intCol),
                Predicate.none(intCol));

        // IS NULL AND
        // IS NULL
        // =
        // IS_NULL
        testMerge(Predicate.<Integer>newIsNullPredicate(intCol),
                Predicate.newIsNullPredicate(intCol),
                Predicate.newIsNullPredicate(intCol));

        // IS NULL AND
        // IS NOT NULL
        // =
        // NONE
        testMerge(Predicate.<Integer>newIsNullPredicate(intCol),
                Predicate.newIsNotNullPredicate(intCol),
                Predicate.none(intCol));

        // IS NULL AND
        // |
        // =
        // NONE
        testMerge(Predicate.newIsNullPredicate(intCol),
                Predicate.newComparisonPredicate(intCol, EQUAL, 5),
                Predicate.none(intCol));

        // IS NULL AND
        // [------->
        // =
        // NONE
        testMerge(Predicate.newIsNullPredicate(intCol),
                Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                Predicate.none(intCol));

        // IS NULL AND
        // <---------)
        // =
        // NONE
        testMerge(Predicate.newIsNullPredicate(intCol),
                Predicate.newComparisonPredicate(intCol, LESS, 5),
                Predicate.none(intCol));

        // IS NULL AND
        // [-------)
        // =
        // NONE
        testMerge(Predicate.newIsNullPredicate(intCol),
                intRange(0, 12),
                Predicate.none(intCol));

        // IS NULL AND
        // |   |   |
        // =
        // NONE
        testMerge(Predicate.newIsNullPredicate(intCol),
                intInList(0, 10, 20),
                Predicate.none(intCol));
    }

    /**
     * Tests tricky merges on a var length type.
     */
    @Test
    public void testMergeString() {

        //         [----->
        //  <-----)
        // =
        // None
        testMerge(Predicate.newComparisonPredicate(stringCol, GREATER_EQUAL, "b\0"),
                Predicate.newComparisonPredicate(stringCol, LESS, "b"),
                Predicate.none(stringCol));

        //        [----->
        //  <-----)
        // =
        // None
        testMerge(Predicate.newComparisonPredicate(stringCol, GREATER_EQUAL, "b"),
                Predicate.newComparisonPredicate(stringCol, LESS, "b"),
                Predicate.none(stringCol));

        //       [----->
        //  <----)
        // =
        //       |
        testMerge(Predicate.newComparisonPredicate(stringCol, GREATER_EQUAL, "b"),
                Predicate.newComparisonPredicate(stringCol, LESS, "b\0"),
                Predicate.newComparisonPredicate(stringCol, EQUAL, "b"));

        //     [----->
        //  <-----)
        // =
        //     [--)
        testMerge(Predicate.newComparisonPredicate(stringCol, GREATER_EQUAL, "a"),
                Predicate.newComparisonPredicate(stringCol, LESS, "a\0\0"),
                new Predicate<>(RANGE, stringCol, SupportedDataType.of("a"), SupportedDataType.of("a\0\0"))
        );

        //     [----->
        //   | | | |
        // =
        //     | | |
        testMerge(Predicate.newComparisonPredicate(stringCol, GREATER_EQUAL, "a"),
                stringInList("a", "c", "b", ""),
                stringInList("a", "b", "c"));

        //   IS NOT NULL
        //   | | | |
        // =
        //   | | | |
        testMerge(Predicate.newIsNotNullPredicate(stringCol),
                stringInList("a", "c", "b", ""),
                stringInList("", "a", "b", "c"));
    }

    @Test
    public void testBoolean() {

        // b >= false
        Assert.assertEquals(Predicate.newIsNotNullPredicate(boolCol),
                Predicate.newComparisonPredicate(boolCol, GREATER_EQUAL, false));
        // b > false
        Assert.assertEquals(Predicate.newComparisonPredicate(boolCol, EQUAL, true),
                Predicate.newComparisonPredicate(boolCol, GREATER, false));
        // b = false
        Assert.assertEquals(Predicate.newComparisonPredicate(boolCol, EQUAL, false),
                Predicate.newComparisonPredicate(boolCol, EQUAL, false));
        // b < false
        Assert.assertEquals(Predicate.none(boolCol),
                Predicate.newComparisonPredicate(boolCol, LESS, false));
        // b <= false
        Assert.assertEquals(Predicate.newComparisonPredicate(boolCol, EQUAL, false),
                Predicate.newComparisonPredicate(boolCol, LESS_EQUAL, false));

        // b >= true
        Assert.assertEquals(Predicate.newComparisonPredicate(boolCol, EQUAL, true),
                Predicate.newComparisonPredicate(boolCol, GREATER_EQUAL, true));
        // b > true
        Assert.assertEquals(Predicate.none(boolCol),
                Predicate.newComparisonPredicate(boolCol, GREATER, true));
        // b = true
        Assert.assertEquals(Predicate.newComparisonPredicate(boolCol, EQUAL, true),
                Predicate.newComparisonPredicate(boolCol, EQUAL, true));
        // b < true
        Assert.assertEquals(Predicate.newComparisonPredicate(boolCol, EQUAL, false),
                Predicate.newComparisonPredicate(boolCol, LESS, true));
        // b <= true
        Assert.assertEquals(Predicate.newIsNotNullPredicate(boolCol),
                Predicate.newComparisonPredicate(boolCol, LESS_EQUAL, true));

        // b IN ()
        Assert.assertEquals(Predicate.none(boolCol), boolInList());

        // b IN (true)
        Assert.assertEquals(Predicate.newComparisonPredicate(boolCol, EQUAL, true),
                boolInList(true, true, true));

        // b IN (false)
        Assert.assertEquals(Predicate.newComparisonPredicate(boolCol, EQUAL, false),
                boolInList(false));

        // b IN (false, true)
        Assert.assertEquals(Predicate.newIsNotNullPredicate(boolCol),
                boolInList(false, true, false, true));
    }

    /**
     * Tests basic predicate merges across all types.
     */
    @Test
    public void testAllTypesMerge() {

        testMerge(Predicate.newComparisonPredicate(boolCol, GREATER_EQUAL, false),
                Predicate.newComparisonPredicate(boolCol, LESS, true),
                new Predicate<>(EQUALITY, boolCol, SupportedDataType.of(false), null)
        );

        testMerge(Predicate.newComparisonPredicate(boolCol, GREATER_EQUAL, false),
                Predicate.newComparisonPredicate(boolCol, LESS_EQUAL, true),
                Predicate.newIsNotNullPredicate(boolCol));

        testMerge(Predicate.newComparisonPredicate(byteCol, GREATER_EQUAL, 0),
                Predicate.newComparisonPredicate(byteCol, LESS, 10),
                new Predicate<>(RANGE,
                        byteCol,
                        SupportedDataType.of(0),
                        SupportedDataType.of(10)
                )
        );

        testMerge(Predicate.newInListPredicate(byteCol,
                        toSupportedDataType((byte) 12, (byte) 14, (byte) 16, (byte) 18)
                ),
                Predicate.newInListPredicate(byteCol,
                        toSupportedDataType((byte) 14, (byte) 18, (byte) 20)
                ),
                Predicate.newInListPredicate(byteCol,
                        toSupportedDataType((byte) 14, (byte) 18)
                )
        );

        testMerge(Predicate.newComparisonPredicate(shortCol, GREATER_EQUAL, (short)0),
                Predicate.newComparisonPredicate(shortCol, LESS, (short)10),
                new Predicate<>(RANGE,
                        shortCol,
                        SupportedDataType.of((short) 0),
                        SupportedDataType.of((short) 10)));

        testMerge(Predicate.newInListPredicate(shortCol,
                        toSupportedDataType((short) 12, (short) 14, (short) 16, (short) 18)
                ),
                Predicate.newInListPredicate(shortCol,
                        toSupportedDataType((short) 14, (short) 18, (short) 20)
                ),
                Predicate.newInListPredicate(shortCol,
                        toSupportedDataType((short) 14, (short) 18)
                )
        );

        testMerge(Predicate.newComparisonPredicate(longCol, GREATER_EQUAL, 0L),
                Predicate.newComparisonPredicate(longCol, LESS, 10L),
                new Predicate<>(RANGE,
                        longCol,
                        SupportedDataType.of(0L),
                        SupportedDataType.of(10L)));

        testMerge(Predicate.newInListPredicate(longCol,
                        toSupportedDataType(12L, 14L, 16L, 18L)
                ),
                Predicate.newInListPredicate(longCol,
                        toSupportedDataType(14L, 18L, 20L)
                ),
                Predicate.newInListPredicate(longCol,
                        toSupportedDataType(14L, 18L)
                )
        );

        testMerge(Predicate.newComparisonPredicate(floatCol, GREATER_EQUAL, 123.45f),
                Predicate.newComparisonPredicate(floatCol, LESS, 678.90f),
                new Predicate<>(RANGE,
                        floatCol,
                        SupportedDataType.of(123.45f),
                        SupportedDataType.of(678.90f)));

        testMerge(Predicate.newInListPredicate(floatCol, toSupportedDataType(12f, 14f, 16f, 18f)),
                Predicate.newInListPredicate(floatCol, toSupportedDataType(14f, 18f, 20f)),
                Predicate.newInListPredicate(floatCol, toSupportedDataType(14f, 18f))
        );

        testMerge(Predicate.newComparisonPredicate(doubleCol, GREATER_EQUAL, 123.45),
                Predicate.newComparisonPredicate(doubleCol, LESS, 678.90),
                new Predicate<>(RANGE,
                        doubleCol,
                        SupportedDataType.of(123.45),
                        SupportedDataType.of(678.90)));

        testMerge(Predicate.newInListPredicate(doubleCol, toSupportedDataType(12d, 14d, 16d, 18d)),
                Predicate.newInListPredicate(doubleCol, toSupportedDataType(14d, 18d, 20d)),
                Predicate.newInListPredicate(doubleCol, toSupportedDataType(14d, 18d))
        );

        testMerge(Predicate.newComparisonPredicate(bigDecimalCol, GREATER_EQUAL, BigDecimal.valueOf(12345, 2)),
                Predicate.newComparisonPredicate(bigDecimalCol, LESS, BigDecimal.valueOf(67890,2)),
                new Predicate<>(RANGE,
                        bigDecimalCol,
                        SupportedDataType.of(BigDecimal.valueOf(12345, 2)),
                        SupportedDataType.of(BigDecimal.valueOf(67890, 2))
                )
        );

        testMerge(Predicate.newInListPredicate(bigDecimalCol,
                toSupportedDataType(BigDecimal.valueOf(12345, 2), BigDecimal.valueOf(45678, 2))),
                Predicate.newInListPredicate(bigDecimalCol, toSupportedDataType(BigDecimal.valueOf(45678, 2), BigDecimal.valueOf(98765, 2))),
                Predicate.newInListPredicate(bigDecimalCol, toSupportedDataType(BigDecimal.valueOf(45678, 2)))
        );

        testMerge(Predicate.newInListPredicate(bigDecimalCol,
                        toSupportedDataType(
                                BigDecimal.valueOf(12345678910L, 2),
                                BigDecimal.valueOf(34567891011L, 2)
                        )
                ),
                Predicate.newInListPredicate(bigDecimalCol,
                        toSupportedDataType(
                                BigDecimal.valueOf(34567891011L, 2),
                                BigDecimal.valueOf(98765432111L, 2)
                        )
                ),
                Predicate.newInListPredicate(bigDecimalCol, toSupportedDataType(BigDecimal.valueOf(34567891011L, 2)))
        );

        testMerge(Predicate.newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        BigDecimal.valueOf(12345678910L, 2)),
                Predicate.newComparisonPredicate(bigDecimalCol, LESS,
                        BigDecimal.valueOf(67890101112L,2)),
                new Predicate(RANGE,
                        bigDecimalCol,
                        SupportedDataType.of(BigDecimal.valueOf(12345678910L, 2)),
                        SupportedDataType.of(BigDecimal.valueOf(67890101112L, 2))
                )
        );

        testMerge(Predicate.newInListPredicate(bigDecimalCol,
                        toSupportedDataType(
                                new BigDecimal("1234567891011121314.15"),
                                new BigDecimal("3456789101112131415.16")
                        )
                ),
                Predicate.newInListPredicate(bigDecimalCol,
                        toSupportedDataType(
                                new BigDecimal("3456789101112131415.16"),
                                new BigDecimal("9876543212345678910.11")
                        )
                ),
                Predicate.newInListPredicate(bigDecimalCol, toSupportedDataType(new BigDecimal("3456789101112131415.16")))
        );

        testMerge(Predicate.newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        new BigDecimal("1234567891011121314.15")),
                Predicate.newComparisonPredicate(bigDecimalCol, LESS,
                        new BigDecimal("67891011121314151617.18")),
                new Predicate<>(RANGE,
                        bigDecimalCol,
                        SupportedDataType.of(new BigDecimal("1234567891011121314.15")),
                        SupportedDataType.of(new BigDecimal("67891011121314151617.18"))
                )
        );

        testMerge(Predicate.newComparisonPredicate(binaryCol, GREATER_EQUAL,
                        new byte[] { 0, 1, 2, 3, 4, 5, 6 }),
                Predicate.newComparisonPredicate(binaryCol, LESS, new byte[] { 10 }),
                new Predicate<>(RANGE,
                        binaryCol,
                        SupportedDataType.of(ByteBuffer.wrap(new byte[] { 0, 1, 2, 3, 4, 5, 6 })),
                        SupportedDataType.of(ByteBuffer.wrap(new byte[] { 10 }))
                )
        );

        testMerge(Predicate.newComparisonPredicate(stringCol, GREATER_EQUAL, "bar"),
                Predicate.newComparisonPredicate(stringCol, LESS, "foo"),
                new Predicate<>(RANGE,
                        stringCol,
                        SupportedDataType.of("bar"),
                        SupportedDataType.of("foo")
                )
        );

        ByteBuffer valA = ByteBuffer.wrap("a".getBytes(UTF_8));
        ByteBuffer valB = ByteBuffer.wrap("b".getBytes(UTF_8));
        ByteBuffer valC = ByteBuffer.wrap("c".getBytes(UTF_8));
        ByteBuffer valD = ByteBuffer.wrap("d".getBytes(UTF_8));
        ByteBuffer valE = ByteBuffer.wrap("e".getBytes(UTF_8));
        testMerge(Predicate.newInListPredicate(binaryCol, toSupportedDataType(ImmutableList.of(valA, valB, valC, valD))),
                Predicate.newInListPredicate(binaryCol, toSupportedDataType(ImmutableList.of(valB, valD, valE))),
                Predicate.newInListPredicate(binaryCol, toSupportedDataType(ImmutableList.of(valB, valD))));
    }

    @Test
    public void testLessEqual() {
        Assert.assertEquals(Predicate.newComparisonPredicate(byteCol, LESS_EQUAL, 10),
                Predicate.newComparisonPredicate(byteCol, LESS, 11));
        Assert.assertEquals(Predicate.newComparisonPredicate(shortCol, LESS_EQUAL, 10),
                Predicate.newComparisonPredicate(shortCol, LESS, 11));
        Assert.assertEquals(Predicate.newComparisonPredicate(intCol, LESS_EQUAL, 10),
                Predicate.newComparisonPredicate(intCol, LESS, 11));
        Assert.assertEquals(Predicate.newComparisonPredicate(longCol, LESS_EQUAL, 10),
                Predicate.newComparisonPredicate(longCol, LESS, 11));
        Assert.assertEquals(Predicate.newComparisonPredicate(floatCol, LESS_EQUAL, 12.345f),
                Predicate.newComparisonPredicate(floatCol, LESS, Math.nextAfter(12.345f,
                        Float.POSITIVE_INFINITY)));
        Assert.assertEquals(Predicate.newComparisonPredicate(doubleCol, LESS_EQUAL, 12.345),
                Predicate.newComparisonPredicate(doubleCol, LESS, Math.nextAfter(12.345,
                        Float.POSITIVE_INFINITY)));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(bigDecimalCol, LESS_EQUAL,
                        BigDecimal.valueOf(12345,2)),
                Predicate.newComparisonPredicate(bigDecimalCol, LESS,
                        BigDecimal.valueOf(12346,2)));
        Assert.assertEquals(Predicate.newComparisonPredicate(stringCol, LESS_EQUAL, "a"),
                Predicate.newComparisonPredicate(stringCol, LESS, "a\0"));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(binaryCol, LESS_EQUAL, new byte[] { (byte) 10 }),
                Predicate.newComparisonPredicate(binaryCol, LESS, new byte[] { (byte) 10, (byte) 0 }));
        Assert.assertEquals(Predicate.newComparisonPredicate(byteCol, LESS_EQUAL, Byte.MAX_VALUE),
                Predicate.newIsNotNullPredicate(byteCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(shortCol, LESS_EQUAL, Short.MAX_VALUE),
                Predicate.newIsNotNullPredicate(shortCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(intCol, LESS_EQUAL, Integer.MAX_VALUE),
                Predicate.newIsNotNullPredicate(intCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(longCol, LESS_EQUAL, Long.MAX_VALUE),
                Predicate.newIsNotNullPredicate(longCol));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(floatCol, LESS_EQUAL, Float.MAX_VALUE),
                Predicate.newComparisonPredicate(floatCol, LESS, Float.POSITIVE_INFINITY));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(floatCol, LESS_EQUAL, Float.POSITIVE_INFINITY),
                Predicate.newIsNotNullPredicate(floatCol));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(doubleCol, LESS_EQUAL, Double.MAX_VALUE),
                Predicate.newComparisonPredicate(doubleCol, LESS, Double.POSITIVE_INFINITY));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(doubleCol, LESS_EQUAL, Double.POSITIVE_INFINITY),
                Predicate.newIsNotNullPredicate(doubleCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(dateCol, LESS_EQUAL, LocalDate.of(2020,6,1)),
                Predicate.newComparisonPredicate(dateCol, LESS, LocalDate.of(2020,6,2))
        );
        Assert.assertEquals(Predicate.newComparisonPredicate(dateCol, LESS_EQUAL, LocalDate.MAX.toEpochDay()),
                Predicate.newIsNotNullPredicate(dateCol));
    }

    @Test
    public void testGreater() {
        Assert.assertEquals(Predicate.newComparisonPredicate(byteCol, GREATER_EQUAL, (byte) 11),
                Predicate.newComparisonPredicate(byteCol, GREATER, (byte) 10)
        );
        Assert.assertEquals(Predicate.newComparisonPredicate(shortCol, GREATER_EQUAL, 11),
                Predicate.newComparisonPredicate(shortCol, GREATER, 10)
        );
        Assert.assertEquals(Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, 11),
                Predicate.newComparisonPredicate(intCol, GREATER, 10)
        );
        Assert.assertEquals(Predicate.newComparisonPredicate(longCol, GREATER_EQUAL, 11),
                Predicate.newComparisonPredicate(longCol, GREATER, 10)
        );
        Assert.assertEquals(
                Predicate.newComparisonPredicate(floatCol, GREATER_EQUAL,
                        Math.nextAfter(12.345f, Float.MAX_VALUE)),
                Predicate.newComparisonPredicate(floatCol, GREATER, 12.345f)
        );
        Assert.assertEquals(
                Predicate.newComparisonPredicate(doubleCol, GREATER_EQUAL,
                        Math.nextAfter(12.345, Float.MAX_VALUE)),
                Predicate.newComparisonPredicate(doubleCol, GREATER, 12.345)
        );
        Assert.assertEquals(
                Predicate.newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        BigDecimal.valueOf(12346, 2)),
                Predicate.newComparisonPredicate(bigDecimalCol, GREATER,
                        BigDecimal.valueOf(12345, 2)));
        Assert.assertEquals(Predicate.newComparisonPredicate(stringCol, GREATER_EQUAL, "a\0"),
                Predicate.newComparisonPredicate(stringCol, GREATER, "a")
        );
        Assert.assertEquals(
                Predicate.newComparisonPredicate(binaryCol, GREATER_EQUAL,
                        new byte[] { (byte) 10, (byte) 0 }),
                Predicate.newComparisonPredicate(binaryCol, GREATER, new byte[] { (byte) 10 })
        );

        Assert.assertEquals(Predicate.none(byteCol),
                Predicate.newComparisonPredicate(byteCol, GREATER, Byte.MAX_VALUE)
        );
        Assert.assertEquals(Predicate.none(shortCol),
                Predicate.newComparisonPredicate(shortCol, GREATER, Short.MAX_VALUE)
        );
        Assert.assertEquals(Predicate.none(intCol),
                Predicate.newComparisonPredicate(intCol, GREATER, Integer.MAX_VALUE)
        );
        Assert.assertEquals(Predicate.none(longCol),
                Predicate.newComparisonPredicate(longCol, GREATER, Long.MAX_VALUE)
        );
        Assert.assertEquals(
                Predicate.newComparisonPredicate(floatCol, GREATER_EQUAL, Float.POSITIVE_INFINITY),
                Predicate.newComparisonPredicate(floatCol, GREATER, Float.MAX_VALUE)
        );
        Assert.assertEquals(
                Predicate.none(floatCol),
                Predicate.newComparisonPredicate(floatCol, GREATER, Float.POSITIVE_INFINITY)
        );
        Assert.assertEquals(
                Predicate.newComparisonPredicate(doubleCol, GREATER_EQUAL, Double.POSITIVE_INFINITY),
                Predicate.newComparisonPredicate(doubleCol, GREATER, Double.MAX_VALUE)
        );
        Assert.assertEquals(
                Predicate.none(doubleCol),
                Predicate.newComparisonPredicate(doubleCol, GREATER, Double.POSITIVE_INFINITY)
        );
        Assert.assertEquals(Predicate.newComparisonPredicate(dateCol, GREATER_EQUAL,
                LocalDate.of(2020,6,15)),
                Predicate.newComparisonPredicate(dateCol, GREATER, LocalDate.of(2020,6,14))
        );
    }

    @Test
    public void testLess() {
        Assert.assertEquals(Predicate.newComparisonPredicate(byteCol, LESS, Byte.MIN_VALUE),
                Predicate.none(byteCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(shortCol, LESS, Short.MIN_VALUE),
                Predicate.none(shortCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(intCol, LESS, Integer.MIN_VALUE),
                Predicate.none(intCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(longCol, LESS, Long.MIN_VALUE),
                Predicate.none(longCol));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(floatCol, LESS, Float.NEGATIVE_INFINITY),
                Predicate.none(floatCol));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(doubleCol, LESS, Double.NEGATIVE_INFINITY),
                Predicate.none(doubleCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(bigDecimalCol, LESS,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL32_PRECISION, 2)),
                Predicate.none(bigDecimalCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(bigDecimalCol, LESS,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL64_PRECISION, 2)),
                Predicate.none(bigDecimalCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(bigDecimalCol, LESS,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL128_PRECISION, 2)),
                Predicate.none(bigDecimalCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(stringCol, LESS, ""),
                Predicate.none(stringCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(binaryCol, LESS, new byte[] {}),
                Predicate.none(binaryCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(dateCol, LESS,
                LocalDate.MIN.toEpochDay()), Predicate.none(dateCol));
    }

    @Test
    public void testGreaterEqual() {
        Assert.assertEquals(
                Predicate.newComparisonPredicate(byteCol, GREATER_EQUAL, Byte.MIN_VALUE),
                Predicate.newIsNotNullPredicate(byteCol));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(shortCol, GREATER_EQUAL, Short.MIN_VALUE),
                Predicate.newIsNotNullPredicate(shortCol));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, Integer.MIN_VALUE),
                Predicate.newIsNotNullPredicate(intCol));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(longCol, GREATER_EQUAL, Long.MIN_VALUE),
                Predicate.newIsNotNullPredicate(longCol));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(floatCol, GREATER_EQUAL, Float.NEGATIVE_INFINITY),
                Predicate.newIsNotNullPredicate(floatCol));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(doubleCol, GREATER_EQUAL, Double.NEGATIVE_INFINITY),
                Predicate.newIsNotNullPredicate(doubleCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL32_PRECISION, 2)),
                Predicate.newIsNotNullPredicate(bigDecimalCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL64_PRECISION, 2)),
                Predicate.newIsNotNullPredicate(bigDecimalCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL128_PRECISION, 2)),
                Predicate.newIsNotNullPredicate(bigDecimalCol));
        Assert.assertEquals(Predicate.newComparisonPredicate(stringCol, GREATER_EQUAL, ""),
                Predicate.newIsNotNullPredicate(stringCol));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(binaryCol, GREATER_EQUAL, new byte[] {}),
                Predicate.newIsNotNullPredicate(binaryCol));

        Assert.assertEquals(
                Predicate.newComparisonPredicate(byteCol, GREATER_EQUAL, Byte.MAX_VALUE),
                Predicate.newComparisonPredicate(byteCol, EQUAL, Byte.MAX_VALUE));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(shortCol, GREATER_EQUAL, Short.MAX_VALUE),
                Predicate.newComparisonPredicate(shortCol, EQUAL, Short.MAX_VALUE));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(intCol, GREATER_EQUAL, Integer.MAX_VALUE),
                Predicate.newComparisonPredicate(intCol, EQUAL, Integer.MAX_VALUE));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(longCol, GREATER_EQUAL, Long.MAX_VALUE),
                Predicate.newComparisonPredicate(longCol, EQUAL, Long.MAX_VALUE));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(floatCol, GREATER_EQUAL, Float.POSITIVE_INFINITY),
                Predicate.newComparisonPredicate(floatCol, EQUAL, Float.POSITIVE_INFINITY));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(doubleCol, GREATER_EQUAL, Double.POSITIVE_INFINITY),
                Predicate.newComparisonPredicate(doubleCol, EQUAL, Double.POSITIVE_INFINITY));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(dateCol, GREATER_EQUAL,
                        LocalDate.MIN.toEpochDay()),
                Predicate.newIsNotNullPredicate(dateCol));
        Assert.assertEquals(
                Predicate.newComparisonPredicate(dateCol, GREATER_EQUAL, LocalDate.MIN.toEpochDay()),
                Predicate.newIsNotNullPredicate(dateCol));
    }

    @Test
    public void testCreateWithObject() {
        Assert.assertEquals(
                Predicate.newComparisonPredicateFromNative(byteCol, EQUAL, SupportedDataType.of((byte) 10).parseToLong()),
                Predicate.newComparisonPredicate(byteCol, EQUAL, (byte) 10));
        Assert.assertEquals(
                Predicate.newComparisonPredicateFromNative(shortCol, EQUAL, SupportedDataType.of((short) 10).parseToLong()),
                Predicate.newComparisonPredicate(shortCol, EQUAL, (short) 10));
        Assert.assertEquals(
                Predicate.newComparisonPredicateFromNative(intCol, EQUAL, SupportedDataType.of(10).parseToLong()),
                Predicate.newComparisonPredicate(intCol, EQUAL, 10));
        Assert.assertEquals(
                Predicate.newComparisonPredicateFromNative(longCol, EQUAL, SupportedDataType.of(10L).parseToLong()),
                Predicate.newComparisonPredicate(longCol, EQUAL, 10L));
        Assert.assertEquals(
                Predicate.newComparisonPredicateFromNative(floatCol, EQUAL, (long) floatToRawIntBits(12.345f)),
                Predicate.newComparisonPredicate(floatCol, EQUAL, 12.345f));
        Assert.assertEquals(
                Predicate.newComparisonPredicateFromNative(doubleCol, EQUAL, doubleToLongBits(12.345)),
                Predicate.newComparisonPredicate(doubleCol, EQUAL, 12.345));
        Assert.assertEquals(
                Predicate.newComparisonPredicateFromNative(bigDecimalCol, EQUAL,
                        (Object) BigDecimal.valueOf(12345,2)),
                Predicate.newComparisonPredicate(bigDecimalCol, EQUAL,
                        BigDecimal.valueOf(12345,2))
        );
        Assert.assertEquals(
                Predicate.newComparisonPredicateFromNative(stringCol, EQUAL, SupportedDataType.of("a").asSlice(VarcharType.VARCHAR)),
                Predicate.newComparisonPredicate(stringCol, EQUAL, "a")
        );
        Assert.assertEquals(
                Predicate.newComparisonPredicateFromNative(binaryCol, EQUAL, SupportedDataType.of(new byte[] { (byte) 10 }).asSlice(VarbinaryType.VARBINARY)),
                Predicate.newComparisonPredicate(binaryCol, EQUAL, new byte[] { (byte) 10 })
        );
        Assert.assertEquals(Predicate.newComparisonPredicateFromNative(binaryCol, EQUAL, SupportedDataType.of("a").asSlice(VarbinaryType.VARBINARY)),
                Predicate.newComparisonPredicate(binaryCol, EQUAL, Base64.getEncoder().encode("a".getBytes(UTF_8)))
        );
        Assert.assertEquals(Predicate.newComparisonPredicateFromNative(dateCol, EQUAL, SupportedDataType.of(LocalDate.of(2020,6,15)).parseToLong()),
                Predicate
                        .newComparisonPredicate(dateCol, EQUAL, LocalDate.of(2020,6,15))
        );
    }

    @Test
    public void testToString() {
        String actual = Predicate.newComparisonPredicate(boolCol, EQUAL, true).toString();
        Assert.assertEquals(actual,
                "`bool` = true");
        Assert.assertEquals(Predicate.newComparisonPredicate(byteCol, EQUAL, 11).toString(),
                "`byte` = 11");
        Assert.assertEquals(Predicate.newComparisonPredicate(shortCol, EQUAL, 11).toString(),
                "`short` = 11");
        Assert.assertEquals(Predicate.newComparisonPredicate(intCol, EQUAL, -123).toString(),
                "`int` = -123");
        Assert.assertEquals(Predicate.newComparisonPredicate(longCol, EQUAL, 5454).toString(),
                "`long` = 5454");
        Assert.assertEquals(Predicate.newComparisonPredicate(floatCol, EQUAL, 123.456f).toString(),
                "`float` = 123.456");
        Assert.assertEquals(Predicate.newComparisonPredicate(doubleCol, EQUAL, 123.456).toString(),
                "`double` = 123.456");
        Assert.assertEquals(Predicate.newComparisonPredicate(bigDecimalCol, EQUAL,
                        BigDecimal.valueOf(12345, 2)).toString(),
                "`bigDecimal` = 123.45");
        Assert.assertEquals(Predicate.newComparisonPredicate(bigDecimalCol, EQUAL,
                        BigDecimal.valueOf(12345678910L, 2)).toString(),
                "`bigDecimal` = 123456789.10");
        Assert.assertEquals(Predicate.newComparisonPredicate(bigDecimalCol, EQUAL,
                        new BigDecimal("1234567891011121314.15")).toString(),
                "`bigDecimal` = 1234567891011121314.15");
        Assert.assertEquals(Predicate.newComparisonPredicate(stringCol, EQUAL, "my string").toString(),
                "`string` = \"my string\"");
        Assert.assertEquals(Predicate.newComparisonPredicate(
                binaryCol, EQUAL, new byte[]{(byte) 0xAB, (byte) 0x01, (byte) 0xCD}).toString(), "`binary` = 0xAB01CD");
        Assert.assertEquals(intInList(10, 0, -10).toString(),
                "`int` IN (-10, 0, 10)");
        Assert.assertEquals(Predicate.newIsNotNullPredicate(stringCol).toString(),
                "`string` IS NOT NULL");
        Assert.assertEquals(Predicate.newIsNullPredicate(stringCol).toString(),
                "`string` IS NULL");
        Assert.assertEquals(Predicate.newComparisonPredicate(stringCol, EQUAL, "my varchar").toString(),
                "`string` = \"my varchar\"");
        Assert.assertEquals(Predicate.newIsNotNullPredicate(binaryCol).toString(),
                "`binary` IS NOT NULL");
        Assert.assertEquals(Predicate.newIsNullPredicate(binaryCol).toString(),
                "`binary` IS NULL");
        // IS NULL predicate on non-nullable column = NONE predicate
        Assert.assertEquals(Predicate.newIsNullPredicate(intCol).toString(),
                "`int` NONE");

        Assert.assertEquals(Predicate.newInListPredicate(
                boolCol, toSupportedDataType(true)).toString(), "`bool` = true");
        Assert.assertEquals(Predicate.newInListPredicate(
                boolCol, toSupportedDataType(false)).toString(), "`bool` = false");
        Assert.assertEquals(Predicate.newInListPredicate(
                boolCol, toSupportedDataType(false, true, true)).toString(), "`bool` IS NOT NULL");
        Assert.assertEquals(Predicate.newInListPredicate(
                byteCol, toSupportedDataType((byte) 1, (byte) 10, (byte) 100)).toString(), "`byte` IN (1, 10, 100)");
        Assert.assertEquals(Predicate.newInListPredicate(
                shortCol, toSupportedDataType((short) 1, (short) 100, (short) 10)).toString(), "`short` IN (1, 10, 100)");
        Assert.assertEquals(Predicate.newInListPredicate(
                intCol, toSupportedDataType(1, 100, 10)).toString(), "`int` IN (1, 10, 100)");
        Assert.assertEquals(Predicate.newInListPredicate(
                longCol, toSupportedDataType(1L, 100L, 10L)).toString(), "`long` IN (1, 10, 100)");
        Assert.assertEquals(Predicate.newInListPredicate(
                floatCol, toSupportedDataType(123.456f, 78.9f)).toString(), "`float` IN (78.9, 123.456)");
        Assert.assertEquals(Predicate.newInListPredicate(
                doubleCol, toSupportedDataType(123.456d, 78.9d)).toString(), "`double` IN (78.9, 123.456)");
        Assert.assertEquals(Predicate.newInListPredicate(stringCol, toSupportedDataType("my string", "a")).toString(),
                "`string` IN (\"a\", \"my string\")");

        ByteBuffer firstBuffer = ByteBuffer.wrap(new byte[]{(byte) 0xAB, (byte) 0x01, (byte) 0xCD});
        ByteBuffer secondBuffer = ByteBuffer.wrap(new byte[]{(byte) 0x00});
        SortedSet<SupportedDataType<ByteBuffer>> setOfStuff = toSupportedDataType(firstBuffer, secondBuffer);
        int i = secondBuffer.compareTo(firstBuffer);
        Assert.assertEquals(setOfStuff.first().value,secondBuffer);
        Assert.assertEquals(setOfStuff.last().value,firstBuffer);
        Assert.assertEquals(Predicate.newInListPredicate(
                binaryCol, setOfStuff).toString(), "`binary` IN (0x00, 0xAB01CD)");


        Assert.assertEquals(Predicate.newIsNullPredicate(dateCol).toString(), "`date` IS NULL");
        Assert.assertEquals(Predicate.newIsNotNullPredicate(dateCol).toString(),
                "`date` IS NOT NULL");

        Assert.assertEquals(Predicate.newComparisonPredicate(dateCol, EQUAL, LocalDate.of(2020, 6, 16))
                        .toString(),
                "`date` = 2020-06-16");
        SortedSet<SupportedDataType<Long>> intDates = toSupportedDataType(LocalDate.of(2020,6,16).toEpochDay(),
                        LocalDate.of(2019,1,1).toEpochDay(),
                        LocalDate.of(2020,11,10).toEpochDay());
        SortedSet<SupportedDataType<ChronoLocalDate>> supportedDataTypes = toSupportedDataType(
                LocalDate.of(2020,6,16),
                LocalDate.of(2019,1,1),
                LocalDate.of(2020,11,10));
        Assert.assertEquals(Predicate.newInListPredicate(dateCol, supportedDataTypes).toString(),
                "`date` IN (2019-01-01, 2020-06-16, 2020-11-10)");
    }

    @SafeVarargs
    public static <T extends Comparable<T>> SortedSet<SupportedDataType<T>> toSupportedDataType(T... values) {
        return toSupportedDataType(ImmutableList.copyOf(values));
    }

    public static <T extends Comparable<T>> SortedSet<SupportedDataType<T>> toSupportedDataType(List<T> list) {
        return list.stream()
                .sorted()
                .map(SupportedDataType::of)
                .collect(Collectors.toCollection(TreeSet::new));
    }

}