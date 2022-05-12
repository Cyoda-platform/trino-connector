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
import com.cyoda.presto.client.types.DataTypeValue;
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
import com.facebook.presto.common.type.VarbinaryType;
import com.facebook.presto.common.type.VarcharType;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.util.Base64;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.logic.ColumnPredicate.ComparisonOp.*;
import static com.cyoda.presto.client.logic.ColumnPredicate.PredicateType.EQUALITY;
import static com.cyoda.presto.client.logic.ColumnPredicate.PredicateType.RANGE;
import static com.cyoda.presto.client.logic.ColumnPredicateUtils.*;
import static java.lang.Double.doubleToLongBits;
import static java.lang.Float.floatToRawIntBits;
import static java.nio.charset.StandardCharsets.UTF_8;

public class NewComparisonColumnPredicateTest {

    public static final String REQUEST_HANDLER_KEY = "mockRequestHandler";
    public static final String CONNECTOR_ID = "connectorId";

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
    // TODO: Create tests for BigInteger
    private CyodaColumnHandle bigIntegerCol;
    private CyodaColumnHandle dateCol;
    // TODO: Create tests for BigInteger
    private CyodaColumnHandle localDatetimeCol;
    // TODO: Create tests for others, like Year, YearMonth, ...

    private ColumnPredicate<Integer> intRange(Integer lower, Integer upper) {
        Preconditions.checkArgument(lower < upper);
        return new ColumnPredicate<>(RANGE, intCol, DataTypeValue.of(lower), DataTypeValue.of(upper));
    }

    private ColumnPredicate<Long> longRange(long lower, long upper) {
        Preconditions.checkArgument(lower < upper);
        return new ColumnPredicate<>(RANGE, intCol, DataTypeValue.of(lower), DataTypeValue.of(upper));
    }

    private ColumnPredicate<Integer> intInList(Integer... values) {
        SortedSet<DataTypeValue<Integer>> valueSet = toDataTypeValue(values);
        return newInListPredicate(intCol, valueSet);
    }

    private ColumnPredicate<Long> longInList(Long... values) {
        SortedSet<DataTypeValue<Long>> valueSet = toDataTypeValue(values);
        return newInListPredicate(intCol, valueSet);
    }

    private ColumnPredicate<Boolean> boolInList(Boolean... values) {
        SortedSet<DataTypeValue<Boolean>> valueSet = toDataTypeValue(values);
        return newInListPredicate(boolCol, valueSet);
    }

    private ColumnPredicate<String> stringInList(String... values) {
        SortedSet<DataTypeValue<String >> valueSet = toDataTypeValue(values);
        return newInListPredicate(stringCol, valueSet);
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

    private <T extends Comparable<T>> void testMerge(ColumnPredicate<T> a,
                                                     ColumnPredicate<T> b,
                                                     ColumnPredicate<T> expected) {

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
        testMerge(newComparisonPredicate(intCol, EQUAL, 0),
                newComparisonPredicate(intCol, EQUAL, 0),
                newComparisonPredicate(intCol, EQUAL, 0));
        // |
        //  |
        // =
        // None
        testMerge(newComparisonPredicate(intCol, EQUAL, 0),
                newComparisonPredicate(intCol, EQUAL, 1),
                none(intCol));

        // Range + Equality
        //--------------------

        // [-------->
        //      |
        // =
        //      |
        testMerge(newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                newComparisonPredicate(intCol, EQUAL, 10),
                newComparisonPredicate(intCol, EQUAL, 10));

        //    [-------->
        //  |
        // =
        // None
        testMerge(newComparisonPredicate(intCol, GREATER_EQUAL, 10),
                newComparisonPredicate(intCol, EQUAL, 0),
                none(intCol));

        // <--------)
        //      |
        // =
        //      |
        testMerge(newComparisonPredicate(intCol, LESS, 10),
                newComparisonPredicate(intCol, EQUAL, 5),
                newComparisonPredicate(intCol, EQUAL, 5));

        // <--------)
        //            |
        // =
        // None
        testMerge(newComparisonPredicate(intCol, LESS, 0),
                newComparisonPredicate(intCol, EQUAL, 10),
                none(intCol));

        // Unbounded Range + Unbounded Range
        //--------------------

        // [--------> AND
        // [-------->
        // =
        // [-------->

        testMerge(newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                newComparisonPredicate(intCol, GREATER_EQUAL, 0));

        // [--------> AND
        //    [----->
        // =
        //    [----->
        testMerge(newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                newComparisonPredicate(intCol, GREATER_EQUAL, 5));

        // <--------) AND
        // <--------)
        // =
        // <--------)

        testMerge(newComparisonPredicate(intCol, LESS, 0),
                newComparisonPredicate(intCol, LESS, 0),
                newComparisonPredicate(intCol, LESS, 0));

        // <--------) AND
        // <----)
        // =
        // <----)

        testMerge(newComparisonPredicate(intCol, LESS, 0),
                newComparisonPredicate(intCol, LESS, -10),
                newComparisonPredicate(intCol, LESS, -10));

        //    [--------> AND
        // <-------)
        // =
        //    [----)
        testMerge(newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                newComparisonPredicate(intCol, LESS, 10),
                intRange(0, 10));

        //     [-----> AND
        // <----)
        // =
        //     |
        testMerge(newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                newComparisonPredicate(intCol, LESS, 6),
                newComparisonPredicate(intCol, EQUAL, 5));

        //     [-----> AND
        // <---)
        // =
        // None
        testMerge(newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                newComparisonPredicate(intCol, LESS, 5),
                none(intCol));

        //       [-----> AND
        // <---)
        // =
        // None
        testMerge(newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                newComparisonPredicate(intCol, LESS, 3),
                none(intCol));

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
                none(intCol));

        // [--) AND
        //       [---)
        // =
        // None
        testMerge(intRange(0, 3),
                intRange(5, 10),
                none(intCol));

        // Lower Bound + Range
        //--------------------

        // [------------>
        //       [---)
        // =
        //       [---)
        testMerge(newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                intRange(5, 10),
                intRange(5, 10));

        // [------------>
        // [--------)
        // =
        // [--------)
        testMerge(newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                intRange(5, 10),
                intRange(5, 10));

        //      [------------>
        // [--------)
        // =
        //      [---)
        testMerge(newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                intRange(0, 10),
                intRange(5, 10));

        //          [------->
        // [-----)
        // =
        // None
        testMerge(newComparisonPredicate(intCol, GREATER_EQUAL, 10),
                intRange(0, 5),
                none(intCol));

        // Upper Bound + Range
        //--------------------

        // <------------)
        //       [---)
        // =
        //       [---)
        testMerge(newComparisonPredicate(intCol, LESS, 10),
                intRange(3, 8),
                intRange(3, 8));

        // <------------)
        //     [--------)
        // =
        //     [--------)
        testMerge(newComparisonPredicate(intCol, LESS, 10),
                intRange(5, 10),
                intRange(5, 10));


        // <------------)
        //         [--------)
        // =
        //         [----)
        testMerge(newComparisonPredicate(intCol, LESS, 5),
                intRange(0, 10),
                intRange(0, 5));

        // Range + Equality
        //--------------------

        //   [---) AND
        // |
        // =
        // None
        testMerge(intRange(3, 5),
                newComparisonPredicate(intCol, EQUAL, 1),
                none(intCol));

        // [---) AND
        // |
        // =
        // |
        testMerge(intRange(0, 5),
                newComparisonPredicate(intCol, EQUAL, 0),
                newComparisonPredicate(intCol, EQUAL, 0));

        // [---) AND
        //   |
        // =
        //   |
        testMerge(intRange(0, 5),
                newComparisonPredicate(intCol, EQUAL, 3),
                newComparisonPredicate(intCol, EQUAL, 3));

        // [---) AND
        //     |
        // =
        // None
        testMerge(intRange(0, 5),
                newComparisonPredicate(intCol, EQUAL, 5),
                none(intCol));

        // [---) AND
        //       |
        // =
        // None
        testMerge(intRange(0, 5),
                newComparisonPredicate(intCol, EQUAL, 7),
                none(intCol));

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
                none(intCol));

        // IN list + NOT NULL
        //--------------------

        testMerge(intInList(10),
                newIsNotNullPredicate(intCol),
                newComparisonPredicate(intCol, EQUAL, 10));

        testMerge(intInList(10, -100),
                newIsNotNullPredicate(intCol),
                intInList(-100, 10));

        // IN list + Equality
        //--------------------

        // | | |
        //   |
        // =
        //   |
        testMerge(intInList(0, 10, 20),
                newComparisonPredicate(intCol, EQUAL, 10),
                newComparisonPredicate(intCol, EQUAL, 10));

        // | | |
        //       |
        // =
        // none
        testMerge(intInList(0, 10, 20),
                newComparisonPredicate(intCol, EQUAL, 30),
                none(intCol));

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
                none(intCol));

        // | | | |
        //    [------>
        // =
        //   | |
        testMerge(intInList(0, 10, 20, 30),
                newComparisonPredicate(intCol, GREATER_EQUAL, 15),
                intInList(20, 30));

        // | | |
        //    [------>
        // =
        //     |
        testMerge(intInList(0, 10, 20),
                newComparisonPredicate(intCol, GREATER_EQUAL, 15),
                newComparisonPredicate(intCol, EQUAL, 20));

        // | |
        //    [------>
        // =
        // none
        testMerge(intInList(0, 10),
                newComparisonPredicate(intCol, GREATER_EQUAL, 15),
                none(intCol));

        // | | | |
        // <--)
        // =
        // | |
        testMerge(intInList(0, 10, 20, 30),
                newComparisonPredicate(intCol, LESS, 15),
                intInList(0, 10));

        // |  | |
        // <--)
        // =
        // |
        testMerge(intInList(0, 10, 20),
                newComparisonPredicate(intCol, LESS, 10),
                newComparisonPredicate(intCol, EQUAL, 0));

        //      | |
        // <--)
        // =
        // none
        testMerge(intInList(10, 20),
                newComparisonPredicate(intCol, LESS, 5),
                none(intCol));

        // None
        //--------------------

        // None AND
        // [---->
        // =
        // None
        testMerge(none(intCol),
                newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                none(intCol));

        // None AND
        // <----)
        // =
        // None
        testMerge(none(intCol),
                newComparisonPredicate(intCol, LESS, 0),
                none(intCol));

        // None AND
        // [----)
        // =
        // None
        testMerge(none(intCol),
                intRange(3, 7),
                none(intCol));

        // None AND
        //  |
        // =
        // None
        testMerge(none(intCol),
                newComparisonPredicate(intCol, EQUAL, 5),
                none(intCol));

        // None AND
        // None
        // =
        // None
        testMerge(none(intCol),
                none(intCol),
                ColumnPredicateUtils.<Integer>none(intCol));

        // IS NOT NULL
        //--------------------

        // IS NOT NULL AND
        // NONE
        // =
        // NONE
        testMerge(newIsNotNullPredicate(intCol),
                none(intCol),
                ColumnPredicateUtils.<Integer>none(intCol));

        // IS NOT NULL AND
        // IS NULL
        // =
        // NONE
        testMerge(newIsNotNullPredicate(intCol),
                newIsNullPredicate(intCol),
                ColumnPredicateUtils.<Integer>none(intCol));

        // IS NOT NULL AND
        // IS NOT NULL
        // =
        // IS NOT NULL
        testMerge(ColumnPredicateUtils.<Integer>newIsNotNullPredicate(intCol),
                newIsNotNullPredicate(intCol),
                newIsNotNullPredicate(intCol));

        // IS NOT NULL AND
        // |
        // =
        // |
        testMerge(newIsNotNullPredicate(intCol),
                newComparisonPredicate(intCol, EQUAL, 5),
                newComparisonPredicate(intCol, EQUAL, 5));

        // IS NOT NULL AND
        // [------->
        // =
        // [------->
        testMerge(newIsNotNullPredicate(intCol),
                newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                newComparisonPredicate(intCol, GREATER_EQUAL, 5));

        // IS NOT NULL AND
        // <---------)
        // =
        // <---------)
        testMerge(newIsNotNullPredicate(intCol),
                newComparisonPredicate(intCol, LESS, 5),
                newComparisonPredicate(intCol, LESS, 5));

        // IS NOT NULL AND
        // [-------)
        // =
        // [-------)
        testMerge(newIsNotNullPredicate(intCol),
                intRange(0, 12),
                intRange(0, 12));


        // IS NOT NULL AND
        // |   |   |
        // =
        // |   |   |
        testMerge(newIsNotNullPredicate(intCol),
                intInList(0, 10, 20),
                intInList(0, 10, 20));

        // IS NULL
        //--------------------

        // IS NULL AND
        // NONE
        // =
        // NONE
        testMerge(ColumnPredicateUtils.<Integer>newIsNullPredicate(intCol),
                none(intCol),
                none(intCol));

        // IS NULL AND
        // IS NULL
        // =
        // IS_NULL
        testMerge(ColumnPredicateUtils.<Integer>newIsNullPredicate(intCol),
                newIsNullPredicate(intCol),
                newIsNullPredicate(intCol));

        // IS NULL AND
        // IS NOT NULL
        // =
        // NONE
        testMerge(ColumnPredicateUtils.<Integer>newIsNullPredicate(intCol),
                newIsNotNullPredicate(intCol),
                none(intCol));

        // IS NULL AND
        // |
        // =
        // NONE
        testMerge(newIsNullPredicate(intCol),
                newComparisonPredicate(intCol, EQUAL, 5),
                none(intCol));

        // IS NULL AND
        // [------->
        // =
        // NONE
        testMerge(newIsNullPredicate(intCol),
                newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                none(intCol));

        // IS NULL AND
        // <---------)
        // =
        // NONE
        testMerge(newIsNullPredicate(intCol),
                newComparisonPredicate(intCol, LESS, 5),
                none(intCol));

        // IS NULL AND
        // [-------)
        // =
        // NONE
        testMerge(newIsNullPredicate(intCol),
                intRange(0, 12),
                none(intCol));

        // IS NULL AND
        // |   |   |
        // =
        // NONE
        testMerge(newIsNullPredicate(intCol),
                intInList(0, 10, 20),
                none(intCol));
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
        testMerge(newComparisonPredicate(stringCol, GREATER_EQUAL, "b\0"),
                newComparisonPredicate(stringCol, LESS, "b"),
                none(stringCol));

        //        [----->
        //  <-----)
        // =
        // None
        testMerge(newComparisonPredicate(stringCol, GREATER_EQUAL, "b"),
                newComparisonPredicate(stringCol, LESS, "b"),
                none(stringCol));

        //       [----->
        //  <----)
        // =
        //       |
        testMerge(newComparisonPredicate(stringCol, GREATER_EQUAL, "b"),
                newComparisonPredicate(stringCol, LESS, "b\0"),
                newComparisonPredicate(stringCol, EQUAL, "b"));

        //     [----->
        //  <-----)
        // =
        //     [--)
        testMerge(newComparisonPredicate(stringCol, GREATER_EQUAL, "a"),
                newComparisonPredicate(stringCol, LESS, "a\0\0"),
                new ColumnPredicate<>(RANGE, stringCol, DataTypeValue.of("a"), DataTypeValue.of("a\0\0"))
        );

        //     [----->
        //   | | | |
        // =
        //     | | |
        testMerge(newComparisonPredicate(stringCol, GREATER_EQUAL, "a"),
                stringInList("a", "c", "b", ""),
                stringInList("a", "b", "c"));

        //   IS NOT NULL
        //   | | | |
        // =
        //   | | | |
        testMerge(newIsNotNullPredicate(stringCol),
                stringInList("a", "c", "b", ""),
                stringInList("", "a", "b", "c"));
    }

    @Test
    public void testBoolean() {

        // b >= false
        Assert.assertEquals(newIsNotNullPredicate(boolCol),
                newComparisonPredicate(boolCol, GREATER_EQUAL, false));
        // b > false
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, true),
                newComparisonPredicate(boolCol, GREATER, false));
        // b = false
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, false),
                newComparisonPredicate(boolCol, EQUAL, false));
        // b < false
        Assert.assertEquals(none(boolCol),
                newComparisonPredicate(boolCol, LESS, false));
        // b <= false
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, false),
                newComparisonPredicate(boolCol, LESS_EQUAL, false));

        // b >= true
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, true),
                newComparisonPredicate(boolCol, GREATER_EQUAL, true));
        // b > true
        Assert.assertEquals(none(boolCol),
                newComparisonPredicate(boolCol, GREATER, true));
        // b = true
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, true),
                newComparisonPredicate(boolCol, EQUAL, true));
        // b < true
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, false),
                newComparisonPredicate(boolCol, LESS, true));
        // b <= true
        Assert.assertEquals(newIsNotNullPredicate(boolCol),
                newComparisonPredicate(boolCol, LESS_EQUAL, true));

        // b IN ()
        Assert.assertEquals(none(boolCol), boolInList());

        // b IN (true)
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, true),
                boolInList(true, true, true));

        // b IN (false)
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, false),
                boolInList(false));

        // b IN (false, true)
        Assert.assertEquals(newIsNotNullPredicate(boolCol),
                boolInList(false, true, false, true));
    }

    /**
     * Tests basic predicate merges across all types.
     */
    @Test
    public void testAllTypesMerge() {

        testMerge(newComparisonPredicate(boolCol, GREATER_EQUAL, false),
                newComparisonPredicate(boolCol, LESS, true),
                new ColumnPredicate<>(EQUALITY, boolCol, DataTypeValue.of(false), null)
        );

        testMerge(newComparisonPredicate(boolCol, GREATER_EQUAL, false),
                newComparisonPredicate(boolCol, LESS_EQUAL, true),
                newIsNotNullPredicate(boolCol));

        testMerge(newComparisonPredicate(byteCol, GREATER_EQUAL, 0),
                newComparisonPredicate(byteCol, LESS, 10),
                new ColumnPredicate<>(RANGE,
                        byteCol,
                        DataTypeValue.of(0),
                        DataTypeValue.of(10)
                )
        );

        testMerge(newInListPredicate(byteCol,
                        toDataTypeValue((byte) 12, (byte) 14, (byte) 16, (byte) 18)
                ),
                newInListPredicate(byteCol,
                        toDataTypeValue((byte) 14, (byte) 18, (byte) 20)
                ),
                newInListPredicate(byteCol,
                        toDataTypeValue((byte) 14, (byte) 18)
                )
        );

        testMerge(newComparisonPredicate(shortCol, GREATER_EQUAL, (short)0),
                newComparisonPredicate(shortCol, LESS, (short)10),
                new ColumnPredicate<>(RANGE,
                        shortCol,
                        DataTypeValue.of((short) 0),
                        DataTypeValue.of((short) 10)));

        testMerge(newInListPredicate(shortCol,
                        toDataTypeValue((short) 12, (short) 14, (short) 16, (short) 18)
                ),
                newInListPredicate(shortCol,
                        toDataTypeValue((short) 14, (short) 18, (short) 20)
                ),
                newInListPredicate(shortCol,
                        toDataTypeValue((short) 14, (short) 18)
                )
        );

        testMerge(newComparisonPredicate(longCol, GREATER_EQUAL, 0L),
                newComparisonPredicate(longCol, LESS, 10L),
                new ColumnPredicate<>(RANGE,
                        longCol,
                        DataTypeValue.of(0L),
                        DataTypeValue.of(10L)));

        testMerge(newInListPredicate(longCol,
                        toDataTypeValue(12L, 14L, 16L, 18L)
                ),
                newInListPredicate(longCol,
                        toDataTypeValue(14L, 18L, 20L)
                ),
                newInListPredicate(longCol,
                        toDataTypeValue(14L, 18L)
                )
        );

        testMerge(newComparisonPredicate(floatCol, GREATER_EQUAL, 123.45f),
                newComparisonPredicate(floatCol, LESS, 678.90f),
                new ColumnPredicate<>(RANGE,
                        floatCol,
                        DataTypeValue.of(123.45f),
                        DataTypeValue.of(678.90f)));

        testMerge(newInListPredicate(floatCol, toDataTypeValue(12f, 14f, 16f, 18f)),
                newInListPredicate(floatCol, toDataTypeValue(14f, 18f, 20f)),
                newInListPredicate(floatCol, toDataTypeValue(14f, 18f))
        );

        testMerge(newComparisonPredicate(doubleCol, GREATER_EQUAL, 123.45),
                newComparisonPredicate(doubleCol, LESS, 678.90),
                new ColumnPredicate<>(RANGE,
                        doubleCol,
                        DataTypeValue.of(123.45),
                        DataTypeValue.of(678.90)));

        testMerge(newInListPredicate(doubleCol, toDataTypeValue(12d, 14d, 16d, 18d)),
                newInListPredicate(doubleCol, toDataTypeValue(14d, 18d, 20d)),
                newInListPredicate(doubleCol, toDataTypeValue(14d, 18d))
        );

        testMerge(newComparisonPredicate(bigDecimalCol, GREATER_EQUAL, BigDecimal.valueOf(12345, 2)),
                newComparisonPredicate(bigDecimalCol, LESS, BigDecimal.valueOf(67890,2)),
                new ColumnPredicate<>(RANGE,
                        bigDecimalCol,
                        DataTypeValue.of(BigDecimal.valueOf(12345, 2)),
                        DataTypeValue.of(BigDecimal.valueOf(67890, 2))
                )
        );

        testMerge(newInListPredicate(bigDecimalCol,
                toDataTypeValue(BigDecimal.valueOf(12345, 2), BigDecimal.valueOf(45678, 2))),
                newInListPredicate(bigDecimalCol, toDataTypeValue(BigDecimal.valueOf(45678, 2), BigDecimal.valueOf(98765, 2))),
                newInListPredicate(bigDecimalCol, toDataTypeValue(BigDecimal.valueOf(45678, 2)))
        );

        testMerge(newInListPredicate(bigDecimalCol,
                        toDataTypeValue(
                                BigDecimal.valueOf(12345678910L, 2),
                                BigDecimal.valueOf(34567891011L, 2)
                        )
                ),
                newInListPredicate(bigDecimalCol,
                        toDataTypeValue(
                                BigDecimal.valueOf(34567891011L, 2),
                                BigDecimal.valueOf(98765432111L, 2)
                        )
                ),
                newInListPredicate(bigDecimalCol, toDataTypeValue(BigDecimal.valueOf(34567891011L, 2)))
        );

        testMerge(newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        BigDecimal.valueOf(12345678910L, 2)),
                newComparisonPredicate(bigDecimalCol, LESS,
                        BigDecimal.valueOf(67890101112L,2)),
                new ColumnPredicate(RANGE,
                        bigDecimalCol,
                        DataTypeValue.of(BigDecimal.valueOf(12345678910L, 2)),
                        DataTypeValue.of(BigDecimal.valueOf(67890101112L, 2))
                )
        );

        testMerge(newInListPredicate(bigDecimalCol,
                        toDataTypeValue(
                                new BigDecimal("1234567891011121314.15"),
                                new BigDecimal("3456789101112131415.16")
                        )
                ),
                newInListPredicate(bigDecimalCol,
                        toDataTypeValue(
                                new BigDecimal("3456789101112131415.16"),
                                new BigDecimal("9876543212345678910.11")
                        )
                ),
                newInListPredicate(bigDecimalCol, toDataTypeValue(new BigDecimal("3456789101112131415.16")))
        );

        testMerge(newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        new BigDecimal("1234567891011121314.15")),
                newComparisonPredicate(bigDecimalCol, LESS,
                        new BigDecimal("67891011121314151617.18")),
                new ColumnPredicate<>(RANGE,
                        bigDecimalCol,
                        DataTypeValue.of(new BigDecimal("1234567891011121314.15")),
                        DataTypeValue.of(new BigDecimal("67891011121314151617.18"))
                )
        );

        testMerge(newComparisonPredicate(binaryCol, GREATER_EQUAL,
                        new byte[] { 0, 1, 2, 3, 4, 5, 6 }),
                newComparisonPredicate(binaryCol, LESS, new byte[] { 10 }),
                new ColumnPredicate<>(RANGE,
                        binaryCol,
                        DataTypeValue.of(ByteBuffer.wrap(new byte[] { 0, 1, 2, 3, 4, 5, 6 })),
                        DataTypeValue.of(ByteBuffer.wrap(new byte[] { 10 }))
                )
        );

        testMerge(newComparisonPredicate(stringCol, GREATER_EQUAL, "bar"),
                newComparisonPredicate(stringCol, LESS, "foo"),
                new ColumnPredicate<>(RANGE,
                        stringCol,
                        DataTypeValue.of("bar"),
                        DataTypeValue.of("foo")
                )
        );

        ByteBuffer valA = ByteBuffer.wrap("a".getBytes(UTF_8));
        ByteBuffer valB = ByteBuffer.wrap("b".getBytes(UTF_8));
        ByteBuffer valC = ByteBuffer.wrap("c".getBytes(UTF_8));
        ByteBuffer valD = ByteBuffer.wrap("d".getBytes(UTF_8));
        ByteBuffer valE = ByteBuffer.wrap("e".getBytes(UTF_8));
        testMerge(newInListPredicate(binaryCol, toDataTypeValue(ImmutableList.of(valA, valB, valC, valD))),
                newInListPredicate(binaryCol, toDataTypeValue(ImmutableList.of(valB, valD, valE))),
                newInListPredicate(binaryCol, toDataTypeValue(ImmutableList.of(valB, valD))));
    }

    @Test
    public void testLessEqual() {
        Assert.assertEquals(newComparisonPredicate(byteCol, LESS_EQUAL, 10),
                newComparisonPredicate(byteCol, LESS, 11));
        Assert.assertEquals(newComparisonPredicate(shortCol, LESS_EQUAL, 10),
                newComparisonPredicate(shortCol, LESS, 11));
        Assert.assertEquals(newComparisonPredicate(intCol, LESS_EQUAL, 10),
                newComparisonPredicate(intCol, LESS, 11));
        Assert.assertEquals(newComparisonPredicate(longCol, LESS_EQUAL, 10),
                newComparisonPredicate(longCol, LESS, 11));
        Assert.assertEquals(newComparisonPredicate(floatCol, LESS_EQUAL, 12.345f),
                newComparisonPredicate(floatCol, LESS, Math.nextAfter(12.345f,
                        Float.POSITIVE_INFINITY)));
        Assert.assertEquals(newComparisonPredicate(doubleCol, LESS_EQUAL, 12.345),
                newComparisonPredicate(doubleCol, LESS, Math.nextAfter(12.345,
                        Float.POSITIVE_INFINITY)));
        Assert.assertEquals(
                newComparisonPredicate(bigDecimalCol, LESS_EQUAL,
                        BigDecimal.valueOf(12345,2)),
                newComparisonPredicate(bigDecimalCol, LESS,
                        BigDecimal.valueOf(12346,2)));
        Assert.assertEquals(newComparisonPredicate(stringCol, LESS_EQUAL, "a"),
                newComparisonPredicate(stringCol, LESS, "a\0"));
        Assert.assertEquals(
                newComparisonPredicate(binaryCol, LESS_EQUAL, new byte[] { (byte) 10 }),
                newComparisonPredicate(binaryCol, LESS, new byte[] { (byte) 10, (byte) 0 }));
        Assert.assertEquals(newComparisonPredicate(byteCol, LESS_EQUAL, Byte.MAX_VALUE),
                newIsNotNullPredicate(byteCol));
        Assert.assertEquals(newComparisonPredicate(shortCol, LESS_EQUAL, Short.MAX_VALUE),
                newIsNotNullPredicate(shortCol));
        Assert.assertEquals(newComparisonPredicate(intCol, LESS_EQUAL, Integer.MAX_VALUE),
                newIsNotNullPredicate(intCol));
        Assert.assertEquals(newComparisonPredicate(longCol, LESS_EQUAL, Long.MAX_VALUE),
                newIsNotNullPredicate(longCol));
        Assert.assertEquals(
                newComparisonPredicate(floatCol, LESS_EQUAL, Float.MAX_VALUE),
                newComparisonPredicate(floatCol, LESS, Float.POSITIVE_INFINITY));
        Assert.assertEquals(
                newComparisonPredicate(floatCol, LESS_EQUAL, Float.POSITIVE_INFINITY),
                newIsNotNullPredicate(floatCol));
        Assert.assertEquals(
                newComparisonPredicate(doubleCol, LESS_EQUAL, Double.MAX_VALUE),
                newComparisonPredicate(doubleCol, LESS, Double.POSITIVE_INFINITY));
        Assert.assertEquals(
                newComparisonPredicate(doubleCol, LESS_EQUAL, Double.POSITIVE_INFINITY),
                newIsNotNullPredicate(doubleCol));
        Assert.assertEquals(newComparisonPredicate(dateCol, LESS_EQUAL, LocalDate.of(2020,6,1)),
                newComparisonPredicate(dateCol, LESS, LocalDate.of(2020,6,2))
        );
        Assert.assertEquals(newComparisonPredicate(dateCol, LESS_EQUAL, LocalDate.MAX.toEpochDay()),
                newIsNotNullPredicate(dateCol));
    }

    @Test
    public void testGreater() {
        Assert.assertEquals(newComparisonPredicate(byteCol, GREATER_EQUAL, (byte) 11),
                newComparisonPredicate(byteCol, GREATER, (byte) 10)
        );
        Assert.assertEquals(newComparisonPredicate(shortCol, GREATER_EQUAL, 11),
                newComparisonPredicate(shortCol, GREATER, 10)
        );
        Assert.assertEquals(newComparisonPredicate(intCol, GREATER_EQUAL, 11),
                newComparisonPredicate(intCol, GREATER, 10)
        );
        Assert.assertEquals(newComparisonPredicate(longCol, GREATER_EQUAL, 11),
                newComparisonPredicate(longCol, GREATER, 10)
        );
        Assert.assertEquals(
                newComparisonPredicate(floatCol, GREATER_EQUAL,
                        Math.nextAfter(12.345f, Float.MAX_VALUE)),
                newComparisonPredicate(floatCol, GREATER, 12.345f)
        );
        Assert.assertEquals(
                newComparisonPredicate(doubleCol, GREATER_EQUAL,
                        Math.nextAfter(12.345, Float.MAX_VALUE)),
                newComparisonPredicate(doubleCol, GREATER, 12.345)
        );
        Assert.assertEquals(
                newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        BigDecimal.valueOf(12346, 2)),
                newComparisonPredicate(bigDecimalCol, GREATER,
                        BigDecimal.valueOf(12345, 2)));
        Assert.assertEquals(newComparisonPredicate(stringCol, GREATER_EQUAL, "a\0"),
                newComparisonPredicate(stringCol, GREATER, "a")
        );
        Assert.assertEquals(
                newComparisonPredicate(binaryCol, GREATER_EQUAL,
                        new byte[] { (byte) 10, (byte) 0 }),
                newComparisonPredicate(binaryCol, GREATER, new byte[] { (byte) 10 })
        );

        Assert.assertEquals(none(byteCol),
                newComparisonPredicate(byteCol, GREATER, Byte.MAX_VALUE)
        );
        Assert.assertEquals(none(shortCol),
                newComparisonPredicate(shortCol, GREATER, Short.MAX_VALUE)
        );
        Assert.assertEquals(none(intCol),
                newComparisonPredicate(intCol, GREATER, Integer.MAX_VALUE)
        );
        Assert.assertEquals(none(longCol),
                newComparisonPredicate(longCol, GREATER, Long.MAX_VALUE)
        );
        Assert.assertEquals(
                newComparisonPredicate(floatCol, GREATER_EQUAL, Float.POSITIVE_INFINITY),
                newComparisonPredicate(floatCol, GREATER, Float.MAX_VALUE)
        );
        Assert.assertEquals(
                none(floatCol),
                newComparisonPredicate(floatCol, GREATER, Float.POSITIVE_INFINITY)
        );
        Assert.assertEquals(
                newComparisonPredicate(doubleCol, GREATER_EQUAL, Double.POSITIVE_INFINITY),
                newComparisonPredicate(doubleCol, GREATER, Double.MAX_VALUE)
        );
        Assert.assertEquals(
                none(doubleCol),
                newComparisonPredicate(doubleCol, GREATER, Double.POSITIVE_INFINITY)
        );
        Assert.assertEquals(newComparisonPredicate(dateCol, GREATER_EQUAL,
                LocalDate.of(2020,6,15)),
                newComparisonPredicate(dateCol, GREATER, LocalDate.of(2020,6,14))
        );
    }

    @Test
    public void testLess() {
        Assert.assertEquals(newComparisonPredicate(byteCol, LESS, Byte.MIN_VALUE),
                none(byteCol));
        Assert.assertEquals(newComparisonPredicate(shortCol, LESS, Short.MIN_VALUE),
                none(shortCol));
        Assert.assertEquals(newComparisonPredicate(intCol, LESS, Integer.MIN_VALUE),
                none(intCol));
        Assert.assertEquals(newComparisonPredicate(longCol, LESS, Long.MIN_VALUE),
                none(longCol));
        Assert.assertEquals(
                newComparisonPredicate(floatCol, LESS, Float.NEGATIVE_INFINITY),
                none(floatCol));
        Assert.assertEquals(
                newComparisonPredicate(doubleCol, LESS, Double.NEGATIVE_INFINITY),
                none(doubleCol));
        Assert.assertEquals(newComparisonPredicate(bigDecimalCol, LESS,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL32_PRECISION, 2)),
                none(bigDecimalCol));
        Assert.assertEquals(newComparisonPredicate(bigDecimalCol, LESS,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL64_PRECISION, 2)),
                none(bigDecimalCol));
        Assert.assertEquals(newComparisonPredicate(bigDecimalCol, LESS,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL128_PRECISION, 2)),
                none(bigDecimalCol));
        Assert.assertEquals(newComparisonPredicate(stringCol, LESS, ""),
                none(stringCol));
        Assert.assertEquals(newComparisonPredicate(binaryCol, LESS, new byte[] {}),
                none(binaryCol));
        Assert.assertEquals(newComparisonPredicate(dateCol, LESS,
                LocalDate.MIN.toEpochDay()), none(dateCol));
    }

    @Test
    public void testGreaterEqual() {
        Assert.assertEquals(
                newComparisonPredicate(byteCol, GREATER_EQUAL, Byte.MIN_VALUE),
                newIsNotNullPredicate(byteCol));
        Assert.assertEquals(
                newComparisonPredicate(shortCol, GREATER_EQUAL, Short.MIN_VALUE),
                newIsNotNullPredicate(shortCol));
        Assert.assertEquals(
                newComparisonPredicate(intCol, GREATER_EQUAL, Integer.MIN_VALUE),
                newIsNotNullPredicate(intCol));
        Assert.assertEquals(
                newComparisonPredicate(longCol, GREATER_EQUAL, Long.MIN_VALUE),
                newIsNotNullPredicate(longCol));
        Assert.assertEquals(
                newComparisonPredicate(floatCol, GREATER_EQUAL, Float.NEGATIVE_INFINITY),
                newIsNotNullPredicate(floatCol));
        Assert.assertEquals(
                newComparisonPredicate(doubleCol, GREATER_EQUAL, Double.NEGATIVE_INFINITY),
                newIsNotNullPredicate(doubleCol));
        Assert.assertEquals(newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL32_PRECISION, 2)),
                newIsNotNullPredicate(bigDecimalCol));
        Assert.assertEquals(newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL64_PRECISION, 2)),
                newIsNotNullPredicate(bigDecimalCol));
        Assert.assertEquals(newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL128_PRECISION, 2)),
                newIsNotNullPredicate(bigDecimalCol));
        Assert.assertEquals(newComparisonPredicate(stringCol, GREATER_EQUAL, ""),
                newIsNotNullPredicate(stringCol));
        Assert.assertEquals(
                newComparisonPredicate(binaryCol, GREATER_EQUAL, new byte[] {}),
                newIsNotNullPredicate(binaryCol));

        Assert.assertEquals(
                newComparisonPredicate(byteCol, GREATER_EQUAL, Byte.MAX_VALUE),
                newComparisonPredicate(byteCol, EQUAL, Byte.MAX_VALUE));
        Assert.assertEquals(
                newComparisonPredicate(shortCol, GREATER_EQUAL, Short.MAX_VALUE),
                newComparisonPredicate(shortCol, EQUAL, Short.MAX_VALUE));
        Assert.assertEquals(
                newComparisonPredicate(intCol, GREATER_EQUAL, Integer.MAX_VALUE),
                newComparisonPredicate(intCol, EQUAL, Integer.MAX_VALUE));
        Assert.assertEquals(
                newComparisonPredicate(longCol, GREATER_EQUAL, Long.MAX_VALUE),
                newComparisonPredicate(longCol, EQUAL, Long.MAX_VALUE));
        Assert.assertEquals(
                newComparisonPredicate(floatCol, GREATER_EQUAL, Float.POSITIVE_INFINITY),
                newComparisonPredicate(floatCol, EQUAL, Float.POSITIVE_INFINITY));
        Assert.assertEquals(
                newComparisonPredicate(doubleCol, GREATER_EQUAL, Double.POSITIVE_INFINITY),
                newComparisonPredicate(doubleCol, EQUAL, Double.POSITIVE_INFINITY));
        Assert.assertEquals(
                newComparisonPredicate(dateCol, GREATER_EQUAL,
                        LocalDate.MIN.toEpochDay()),
                newIsNotNullPredicate(dateCol));
        Assert.assertEquals(
                newComparisonPredicate(dateCol, GREATER_EQUAL, LocalDate.MIN.toEpochDay()),
                newIsNotNullPredicate(dateCol));
    }

    @Test
    public void testCreateWithObject() {
        Assert.assertEquals(
                newComparisonPredicateFromNative(byteCol, EQUAL, DataTypeValue.of((byte) 10).parseToLong()),
                newComparisonPredicate(byteCol, EQUAL, (byte) 10));
        Assert.assertEquals(
                newComparisonPredicateFromNative(shortCol, EQUAL, DataTypeValue.of((short) 10).parseToLong()),
                newComparisonPredicate(shortCol, EQUAL, (short) 10));
        Assert.assertEquals(
                newComparisonPredicateFromNative(intCol, EQUAL, DataTypeValue.of(10).parseToLong()),
                newComparisonPredicate(intCol, EQUAL, 10));
        Assert.assertEquals(
                newComparisonPredicateFromNative(longCol, EQUAL, DataTypeValue.of(10L).parseToLong()),
                newComparisonPredicate(longCol, EQUAL, 10L));
        Assert.assertEquals(
                newComparisonPredicateFromNative(floatCol, EQUAL, (long) floatToRawIntBits(12.345f)),
                newComparisonPredicate(floatCol, EQUAL, 12.345f));
        Assert.assertEquals(
                newComparisonPredicateFromNative(doubleCol, EQUAL, doubleToLongBits(12.345)),
                newComparisonPredicate(doubleCol, EQUAL, 12.345));
        Assert.assertEquals(
                newComparisonPredicateFromNative(bigDecimalCol, EQUAL,
                        (Object) BigDecimal.valueOf(12345,2)),
                newComparisonPredicate(bigDecimalCol, EQUAL,
                        BigDecimal.valueOf(12345,2))
        );
        Assert.assertEquals(
                newComparisonPredicateFromNative(stringCol, EQUAL, DataTypeValue.of("a").asSlice(VarcharType.VARCHAR)),
                newComparisonPredicate(stringCol, EQUAL, "a")
        );
        Assert.assertEquals(
                newComparisonPredicateFromNative(binaryCol, EQUAL, DataTypeValue.of(new byte[] { (byte) 10 }).asSlice(VarbinaryType.VARBINARY)),
                newComparisonPredicate(binaryCol, EQUAL, new byte[] { (byte) 10 })
        );
        Assert.assertEquals(newComparisonPredicateFromNative(binaryCol, EQUAL, DataTypeValue.of("a").asSlice(VarbinaryType.VARBINARY)),
                newComparisonPredicate(binaryCol, EQUAL, Base64.getEncoder().encode("a".getBytes(UTF_8)))
        );
        Assert.assertEquals(newComparisonPredicateFromNative(dateCol, EQUAL, DataTypeValue.of(LocalDate.of(2020,6,15)).parseToLong()),
                newComparisonPredicate(dateCol, EQUAL, LocalDate.of(2020,6,15))
        );
    }

    @Test
    public void testToString() {
        String actual = newComparisonPredicate(boolCol, EQUAL, true).toString();
        Assert.assertEquals(actual,
                "`bool` = true");
        Assert.assertEquals(newComparisonPredicate(byteCol, EQUAL, 11).toString(),
                "`byte` = 11");
        Assert.assertEquals(newComparisonPredicate(shortCol, EQUAL, 11).toString(),
                "`short` = 11");
        Assert.assertEquals(newComparisonPredicate(intCol, EQUAL, -123).toString(),
                "`int` = -123");
        Assert.assertEquals(newComparisonPredicate(longCol, EQUAL, 5454).toString(),
                "`long` = 5454");
        Assert.assertEquals(newComparisonPredicate(floatCol, EQUAL, 123.456f).toString(),
                "`float` = 123.456");
        Assert.assertEquals(newComparisonPredicate(doubleCol, EQUAL, 123.456).toString(),
                "`double` = 123.456");
        Assert.assertEquals(newComparisonPredicate(bigDecimalCol, EQUAL,
                        BigDecimal.valueOf(12345, 2)).toString(),
                "`bigDecimal` = 123.45");
        Assert.assertEquals(newComparisonPredicate(bigDecimalCol, EQUAL,
                        BigDecimal.valueOf(12345678910L, 2)).toString(),
                "`bigDecimal` = 123456789.10");
        Assert.assertEquals(newComparisonPredicate(bigDecimalCol, EQUAL,
                        new BigDecimal("1234567891011121314.15")).toString(),
                "`bigDecimal` = 1234567891011121314.15");
        Assert.assertEquals(newComparisonPredicate(stringCol, EQUAL, "my string").toString(),
                "`string` = \"my string\"");
        Assert.assertEquals(newComparisonPredicate(
                binaryCol, EQUAL, new byte[]{(byte) 0xAB, (byte) 0x01, (byte) 0xCD}).toString(), "`binary` = 0xAB01CD");
        Assert.assertEquals(intInList(10, 0, -10).toString(),
                "`int` IN (-10, 0, 10)");
        Assert.assertEquals(newIsNotNullPredicate(stringCol).toString(),
                "`string` IS NOT NULL");
        Assert.assertEquals(newIsNullPredicate(stringCol).toString(),
                "`string` IS NULL");
        Assert.assertEquals(newComparisonPredicate(stringCol, EQUAL, "my varchar").toString(),
                "`string` = \"my varchar\"");
        Assert.assertEquals(newIsNotNullPredicate(binaryCol).toString(),
                "`binary` IS NOT NULL");
        Assert.assertEquals(newIsNullPredicate(binaryCol).toString(),
                "`binary` IS NULL");
        // IS NULL predicate on non-nullable column = NONE predicate
        Assert.assertEquals(newIsNullPredicate(intCol).toString(),
                "`int` NONE");

        Assert.assertEquals(newInListPredicate(
                boolCol, toDataTypeValue(true)).toString(), "`bool` = true");
        Assert.assertEquals(newInListPredicate(
                boolCol, toDataTypeValue(false)).toString(), "`bool` = false");
        Assert.assertEquals(newInListPredicate(
                boolCol, toDataTypeValue(false, true, true)).toString(), "`bool` IS NOT NULL");
        Assert.assertEquals(newInListPredicate(
                byteCol, toDataTypeValue((byte) 1, (byte) 10, (byte) 100)).toString(), "`byte` IN (1, 10, 100)");
        Assert.assertEquals(newInListPredicate(
                shortCol, toDataTypeValue((short) 1, (short) 100, (short) 10)).toString(), "`short` IN (1, 10, 100)");
        Assert.assertEquals(newInListPredicate(
                intCol, toDataTypeValue(1, 100, 10)).toString(), "`int` IN (1, 10, 100)");
        Assert.assertEquals(newInListPredicate(
                longCol, toDataTypeValue(1L, 100L, 10L)).toString(), "`long` IN (1, 10, 100)");
        Assert.assertEquals(newInListPredicate(
                floatCol, toDataTypeValue(123.456f, 78.9f)).toString(), "`float` IN (78.9, 123.456)");
        Assert.assertEquals(newInListPredicate(
                doubleCol, toDataTypeValue(123.456d, 78.9d)).toString(), "`double` IN (78.9, 123.456)");
        Assert.assertEquals(newInListPredicate(stringCol, toDataTypeValue("my string", "a")).toString(),
                "`string` IN (\"a\", \"my string\")");

        ByteBuffer firstBuffer = ByteBuffer.wrap(new byte[]{(byte) 0xAB, (byte) 0x01, (byte) 0xCD});
        ByteBuffer secondBuffer = ByteBuffer.wrap(new byte[]{(byte) 0x00});
        SortedSet<DataTypeValue<ByteBuffer>> setOfStuff = toDataTypeValue(firstBuffer, secondBuffer);
        Assert.assertEquals(setOfStuff.first().value,secondBuffer);
        Assert.assertEquals(setOfStuff.last().value,firstBuffer);
        Assert.assertEquals(newInListPredicate(
                binaryCol, setOfStuff).toString(), "`binary` IN (0x00, 0xAB01CD)");


        Assert.assertEquals(newIsNullPredicate(dateCol).toString(), "`date` IS NULL");
        Assert.assertEquals(newIsNotNullPredicate(dateCol).toString(),
                "`date` IS NOT NULL");

        Assert.assertEquals(newComparisonPredicate(dateCol, EQUAL, LocalDate.of(2020, 6, 16))
                        .toString(),
                "`date` = 2020-06-16");
        SortedSet<DataTypeValue<ChronoLocalDate>> dataTypeValues = toDataTypeValue(
                LocalDate.of(2020,6,16),
                LocalDate.of(2019,1,1),
                LocalDate.of(2020,11,10));
        Assert.assertEquals(newInListPredicate(dateCol, dataTypeValues).toString(),
                "`date` IN (2019-01-01, 2020-06-16, 2020-11-10)");
    }

    @SafeVarargs
    public static <T extends Comparable<T>> SortedSet<DataTypeValue<T>> toDataTypeValue(T... values) {
        return toDataTypeValue(ImmutableList.copyOf(values));
    }

    public static <T extends Comparable<T>> SortedSet<DataTypeValue<T>> toDataTypeValue(List<T> list) {
        return list.stream()
                .sorted()
                .map(DataTypeValue::of)
                .collect(Collectors.toCollection(TreeSet::new));
    }

}