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

import com.cyoda.presto.client.types.BigDecimalType;
import com.cyoda.presto.client.types.CompoundDataType;
import com.cyoda.presto.client.types.DataType;
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
import com.facebook.presto.common.type.Type;
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
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.logic.ColumnPredicate.ComparisonOp.*;
import static com.cyoda.presto.client.logic.ColumnPredicate.PredicateType.EQUALITY;
import static com.cyoda.presto.client.logic.ColumnPredicate.PredicateType.RANGE;
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
        return new ColumnPredicate<>(RANGE, intCol, lower, upper);
    }

    private ColumnPredicate<Integer> intInList(Integer... values) {
        SortedSet<Integer> valueSet = toValueSet(values);
        if (valueSet.isEmpty()) {
            return ColumnPredicate.none(intCol);
        }
        return ColumnPredicate.buildInList(intCol, valueSet);
    }

    private ColumnPredicate<Boolean> boolInList(Boolean... values) {
        SortedSet<Boolean> valueSet = toValueSet(values);
        if (valueSet.isEmpty()) {
            return ColumnPredicate.none(boolCol);
        }
        // IN (true, false) predicates can be simplified to IS NOT NULL.
        if (valueSet.size() > 1) {
            return ColumnPredicate.isNotNull(boolCol);
        }
        return ColumnPredicate.buildInList(boolCol, valueSet);
    }

    private ColumnPredicate<String> stringInList(String... values) {
        SortedSet<String> valueSet = toValueSet(values);
        if (valueSet.isEmpty()) {
            return ColumnPredicate.none(stringCol);
        }
        return ColumnPredicate.buildInList(stringCol, valueSet);
    }

    @BeforeMethod
    public void setup() {

        int pos = 0;
        boolCol = newCyodaColumnHandle("bool", BooleanType.BOOLEAN, DataType.BOOLEAN, pos++, true);
        byteCol = newCyodaColumnHandle("byte", TinyintType.TINYINT, DataType.BYTE, pos++, true);
        shortCol = newCyodaColumnHandle("short", SmallintType.SMALLINT, DataType.SHORT, pos++, true);
        intCol = newCyodaColumnHandle("int", IntegerType.INTEGER, DataType.INTEGER, pos++, false);

        longCol = newCyodaColumnHandle("long", DecimalType.createDecimalType(), DataType.LONG, pos++, true);

        floatCol = newCyodaColumnHandle("float", RealType.REAL, DataType.FLOAT, pos++, true);
        doubleCol = newCyodaColumnHandle("double", DoubleType.DOUBLE, DataType.DOUBLE, pos++, true);
        stringCol = newCyodaColumnHandle("string", VarcharType.VARCHAR, DataType.STRING, pos++, true);
        binaryCol = newCyodaColumnHandle("binary", VarbinaryType.VARBINARY, DataType.BYTE_BUFFER, pos++, true);

        bigDecimalCol = newCyodaColumnHandle("bigDecimal", BigDecimalType.BIG_DECIMAL_TYPE, DataType.BIG_DECIMAL, pos++, true);

        bigIntegerCol = newCyodaColumnHandle("bigInt", BigintType.BIGINT, DataType.BIG_INTEGER, pos++, true);
        localDatetimeCol = newCyodaColumnHandle("localDatetime", TimestampType.TIMESTAMP, DataType.LOCAL_DATE_TIME, pos++, true);
        dateCol = newCyodaColumnHandle("date", DateType.DATE, DataType.LOCAL_DATE, pos, true);
    }

    private static CyodaColumnHandle newCyodaColumnHandle(String name, Type type, DataType dataType, int pos, boolean nullable){
        return new CyodaColumnHandle(CONNECTOR_ID, name, type, new CompoundDataType(name, dataType), pos, REQUEST_HANDLER_KEY, nullable);
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
                ColumnPredicate.none(intCol));

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
                ColumnPredicate.none(intCol));

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
                ColumnPredicate.none(intCol));

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
                ColumnPredicate.none(intCol));

        //       [-----> AND
        // <---)
        // =
        // None
        testMerge(newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                newComparisonPredicate(intCol, LESS, 3),
                ColumnPredicate.none(intCol));

        // Range + Range
        //--------------------

        // [--------) AND
        // [--------)
        // =
        // [--------)

        testMerge(intRange(0, 10),
                intRange(0, 10),
                intRange(0, 10));

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
                ColumnPredicate.none(intCol));

        // [--) AND
        //       [---)
        // =
        // None
        testMerge(intRange(0, 3),
                intRange(5, 10),
                ColumnPredicate.none(intCol));

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
                ColumnPredicate.none(intCol));

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
                ColumnPredicate.none(intCol));

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
                ColumnPredicate.none(intCol));

        // [---) AND
        //       |
        // =
        // None
        testMerge(intRange(0, 5),
                newComparisonPredicate(intCol, EQUAL, 7),
                ColumnPredicate.none(intCol));

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
                ColumnPredicate.none(intCol));

        // IN list + NOT NULL
        //--------------------

        testMerge(intInList(10),
                ColumnPredicate.isNotNull(intCol),
                newComparisonPredicate(intCol, EQUAL, 10));

        testMerge(intInList(10, -100),
                ColumnPredicate.isNotNull(intCol),
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
                ColumnPredicate.none(intCol));

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
                ColumnPredicate.none(intCol));

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
                ColumnPredicate.none(intCol));

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
                ColumnPredicate.none(intCol));

        // None
        //--------------------

        // None AND
        // [---->
        // =
        // None
        testMerge(ColumnPredicate.none(intCol),
                newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                ColumnPredicate.none(intCol));

        // None AND
        // <----)
        // =
        // None
        testMerge(ColumnPredicate.none(intCol),
                newComparisonPredicate(intCol, LESS, 0),
                ColumnPredicate.none(intCol));

        // None AND
        // [----)
        // =
        // None
        testMerge(ColumnPredicate.none(intCol),
                intRange(3, 7),
                ColumnPredicate.none(intCol));

        // None AND
        //  |
        // =
        // None
        testMerge(ColumnPredicate.none(intCol),
                newComparisonPredicate(intCol, EQUAL, 5),
                ColumnPredicate.none(intCol));

        // None AND
        // None
        // =
        // None
        testMerge(ColumnPredicate.none(intCol),
                ColumnPredicate.none(intCol),
                ColumnPredicate.<Integer>none(intCol));

        // IS NOT NULL
        //--------------------

        // IS NOT NULL AND
        // NONE
        // =
        // NONE
        testMerge(ColumnPredicate.isNotNull(intCol),
                ColumnPredicate.none(intCol),
                ColumnPredicate.<Integer>none(intCol));

        // IS NOT NULL AND
        // IS NULL
        // =
        // NONE
        testMerge(ColumnPredicate.isNotNull(intCol),
                ColumnPredicate.isNull(intCol),
                ColumnPredicate.<Integer>none(intCol));

        // IS NOT NULL AND
        // IS NOT NULL
        // =
        // IS NOT NULL
        testMerge(ColumnPredicate.<Integer>isNotNull(intCol),
                ColumnPredicate.isNotNull(intCol),
                ColumnPredicate.isNotNull(intCol));

        // IS NOT NULL AND
        // |
        // =
        // |
        testMerge(ColumnPredicate.isNotNull(intCol),
                newComparisonPredicate(intCol, EQUAL, 5),
                newComparisonPredicate(intCol, EQUAL, 5));

        // IS NOT NULL AND
        // [------->
        // =
        // [------->
        testMerge(ColumnPredicate.isNotNull(intCol),
                newComparisonPredicate(intCol, GREATER_EQUAL, 5),
                newComparisonPredicate(intCol, GREATER_EQUAL, 5));

        // IS NOT NULL AND
        // <---------)
        // =
        // <---------)
        testMerge(ColumnPredicate.isNotNull(intCol),
                newComparisonPredicate(intCol, LESS, 5),
                newComparisonPredicate(intCol, LESS, 5));

        // IS NOT NULL AND
        // [-------)
        // =
        // [-------)
        testMerge(ColumnPredicate.isNotNull(intCol),
                intRange(0, 12),
                intRange(0, 12));


        // IS NOT NULL AND
        // |   |   |
        // =
        // |   |   |
        testMerge(ColumnPredicate.isNotNull(intCol),
                intInList(0, 10, 20),
                intInList(0, 10, 20));

        // IS NULL
        //--------------------

        // IS NULL AND
        // NONE
        // =
        // NONE
        testMerge(ColumnPredicate.<Integer>isNull(intCol),
                ColumnPredicate.none(intCol),
                ColumnPredicate.none(intCol));

        // IS NULL AND
        // IS NULL
        // =
        // IS_NULL
        testMerge(ColumnPredicate.<Integer>isNull(intCol),
                ColumnPredicate.isNull(intCol),
                ColumnPredicate.isNull(intCol));

        // IS NULL AND
        // IS NOT NULL
        // =
        // NONE
        testMerge(ColumnPredicate.<Integer>isNull(intCol),
                ColumnPredicate.isNotNull(intCol),
                ColumnPredicate.none(intCol));

        // IS NULL AND
        // |
        // =
        // NONE
        testMerge(ColumnPredicate.isNull(intCol),
                newComparisonPredicate(intCol, EQUAL, 5),
                ColumnPredicate.none(intCol));

        // IS NULL AND
        // [------->
        // =
        // NONE
        testMerge(ColumnPredicate.isNull(intCol),
                newComparisonPredicate(intCol, GREATER_EQUAL, 0),
                ColumnPredicate.none(intCol));

        // IS NULL AND
        // <---------)
        // =
        // NONE
        testMerge(ColumnPredicate.isNull(intCol),
                newComparisonPredicate(intCol, LESS, 5),
                ColumnPredicate.none(intCol));

        // IS NULL AND
        // [-------)
        // =
        // NONE
        testMerge(ColumnPredicate.isNull(intCol),
                intRange(0, 12),
                ColumnPredicate.none(intCol));

        // IS NULL AND
        // |   |   |
        // =
        // NONE
        testMerge(ColumnPredicate.isNull(intCol),
                intInList(0, 10, 20),
                ColumnPredicate.none(intCol));
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
                ColumnPredicate.none(stringCol));

        //        [----->
        //  <-----)
        // =
        // None
        testMerge(newComparisonPredicate(stringCol, GREATER_EQUAL, "b"),
                newComparisonPredicate(stringCol, LESS, "b"),
                ColumnPredicate.none(stringCol));

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
                new ColumnPredicate<>(RANGE, stringCol, "a", "a\0\0")
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
        testMerge(ColumnPredicate.isNotNull(stringCol),
                stringInList("a", "c", "b", ""),
                stringInList("", "a", "b", "c"));
    }

    @Test
    public void testBoolean() {

        // b >= false
        Assert.assertEquals(ColumnPredicate.isNotNull(boolCol),
                newComparisonPredicate(boolCol, GREATER_EQUAL, false));
        // b > false
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, true),
                newComparisonPredicate(boolCol, GREATER, false));
        // b = false
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, false),
                newComparisonPredicate(boolCol, EQUAL, false));
        // b < false
        Assert.assertEquals(ColumnPredicate.none(boolCol),
                newComparisonPredicate(boolCol, LESS, false));
        // b <= false
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, false),
                newComparisonPredicate(boolCol, LESS_EQUAL, false));

        // b >= true
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, true),
                newComparisonPredicate(boolCol, GREATER_EQUAL, true));
        // b > true
        Assert.assertEquals(ColumnPredicate.none(boolCol),
                newComparisonPredicate(boolCol, GREATER, true));
        // b = true
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, true),
                newComparisonPredicate(boolCol, EQUAL, true));
        // b < true
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, false),
                newComparisonPredicate(boolCol, LESS, true));
        // b <= true
        Assert.assertEquals(ColumnPredicate.isNotNull(boolCol),
                newComparisonPredicate(boolCol, LESS_EQUAL, true));

        // b IN ()
        Assert.assertEquals(ColumnPredicate.none(boolCol), boolInList());

        // b IN (true)
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, true),
                boolInList(true, true, true));

        // b IN (false)
        Assert.assertEquals(newComparisonPredicate(boolCol, EQUAL, false),
                boolInList(false));

        // b IN (false, true)
        Assert.assertEquals(ColumnPredicate.isNotNull(boolCol),
                boolInList(false, true, false, true));
    }

    /**
     * Tests basic predicate merges across all types.
     */
    @Test
    public void testAllTypesMerge() {

        testMerge(newComparisonPredicate(boolCol, GREATER_EQUAL, false),
                newComparisonPredicate(boolCol, LESS, true),
                new ColumnPredicate<>(EQUALITY, boolCol, false, null)
        );

        testMerge(newComparisonPredicate(boolCol, GREATER_EQUAL, false),
                newComparisonPredicate(boolCol, LESS_EQUAL, true),
                ColumnPredicate.isNotNull(boolCol));

        testMerge(newComparisonPredicate(byteCol, GREATER_EQUAL, (byte)0),
                newComparisonPredicate(byteCol, LESS, (byte)10),
                new ColumnPredicate<>(RANGE,
                        byteCol,
                        (byte)0,
                        (byte)10
                )
        );

        ColumnPredicate<Byte> result24;
        final SortedSet<Byte> values24 = toValueSet((byte) 14, (byte) 18);
        if (values24.isEmpty()) {
            result24 = ColumnPredicate.none(byteCol);
        } else {
            result24 = ColumnPredicate.buildInList(byteCol, values24);
        }
        ColumnPredicate<Byte> result25;
        final SortedSet<Byte> values25 = toValueSet((byte) 14, (byte) 18, (byte) 20);
        if (values25.isEmpty()) {
            result25 = ColumnPredicate.none(byteCol);
        } else {
            result25 = ColumnPredicate.buildInList(byteCol, values25);
        }
        ColumnPredicate<Byte> result26;
        final SortedSet<Byte> values26 = toValueSet((byte) 12, (byte) 14, (byte) 16, (byte) 18);
        if (values26.isEmpty()) {
            result26 = ColumnPredicate.none(byteCol);
        } else {
            result26 = ColumnPredicate.buildInList(byteCol, values26);
        }
        testMerge(result26,
                result25,
                result24
        );

        testMerge(newComparisonPredicate(shortCol, GREATER_EQUAL, (short) 0),
                newComparisonPredicate(shortCol, LESS, (short) 10),
                new ColumnPredicate<>(RANGE,
                        shortCol,
                        (Short) (short) 0,
                        (Short) (short) 10));

        ColumnPredicate<Short> result21;
        final SortedSet<Short> values21 = toValueSet((short) 14, (short) 18);
        if (values21.isEmpty()) {
            result21 = ColumnPredicate.none(shortCol);
        } else {
            result21 = ColumnPredicate.buildInList(shortCol, values21);
        }
        ColumnPredicate<Short> result22;
        final SortedSet<Short> values22 = toValueSet((short) 14, (short) 18, (short) 20);
        if (values22.isEmpty()) {
            result22 = ColumnPredicate.none(shortCol);
        } else {
            result22 = ColumnPredicate.buildInList(shortCol, values22);
        }
        ColumnPredicate<Short> result23;
        final SortedSet<Short> values23 = toValueSet((short) 12, (short) 14, (short) 16, (short) 18);
        if (values23.isEmpty()) {
            result23 = ColumnPredicate.none(shortCol);
        } else {
            result23 = ColumnPredicate.buildInList(shortCol, values23);
        }
        testMerge(result23,
                result22,
                result21
        );

        testMerge(newComparisonPredicate(longCol, GREATER_EQUAL, 0L),
                newComparisonPredicate(longCol, LESS, 10L),
                new ColumnPredicate<>(RANGE,
                        longCol,
                        0L,
                        10L));

        ColumnPredicate<Long> result18;
        final SortedSet<Long> values18 = toValueSet(14L, 18L);
        if (values18.isEmpty()) {
            result18 = ColumnPredicate.none(longCol);
        } else {
            result18 = ColumnPredicate.buildInList(longCol, values18);
        }
        ColumnPredicate<Long> result19;
        final SortedSet<Long> values19 = toValueSet(14L, 18L, 20L);
        if (values19.isEmpty()) {
            result19 = ColumnPredicate.none(longCol);
        } else {
            result19 = ColumnPredicate.buildInList(longCol, values19);
        }
        ColumnPredicate<Long> result20;
        final SortedSet<Long> values20 = toValueSet(12L, 14L, 16L, 18L);
        if (values20.isEmpty()) {
            result20 = ColumnPredicate.none(longCol);
        } else {
            result20 = ColumnPredicate.buildInList(longCol, values20);
        }
        testMerge(result20,
                result19,
                result18
        );

        testMerge(newComparisonPredicate(floatCol, GREATER_EQUAL, 123.45f),
                newComparisonPredicate(floatCol, LESS, 678.90f),
                new ColumnPredicate<>(RANGE,
                        floatCol,
                        123.45f,
                        678.90f));

        ColumnPredicate<Float> result15;
        final SortedSet<Float> values15 = toValueSet(14f, 18f);
        if (values15.isEmpty()) {
            result15 = ColumnPredicate.none(floatCol);
        } else {
            result15 = ColumnPredicate.buildInList(floatCol, values15);
        }
        ColumnPredicate<Float> result16;
        final SortedSet<Float> values16 = toValueSet(14f, 18f, 20f);
        if (values16.isEmpty()) {
            result16 = ColumnPredicate.none(floatCol);
        } else {
            result16 = ColumnPredicate.buildInList(floatCol, values16);
        }
        ColumnPredicate<Float> result17;
        final SortedSet<Float> values17 = toValueSet(12f, 14f, 16f, 18f);
        if (values17.isEmpty()) {
            result17 = ColumnPredicate.none(floatCol);
        } else {
            result17 = ColumnPredicate.buildInList(floatCol, values17);
        }
        testMerge(result17,
                result16,
                result15
        );

        testMerge(newComparisonPredicate(doubleCol, GREATER_EQUAL, 123.45),
                newComparisonPredicate(doubleCol, LESS, 678.90),
                new ColumnPredicate<>(RANGE,
                        doubleCol,
                        123.45,
                        678.90));

        ColumnPredicate<Double> result12;
        final SortedSet<Double> values12 = toValueSet(14d, 18d);
        if (values12.isEmpty()) {
            result12 = ColumnPredicate.none(doubleCol);
        } else {
            result12 = ColumnPredicate.buildInList(doubleCol, values12);
        }
        ColumnPredicate<Double> result13;
        final SortedSet<Double> values13 = toValueSet(14d, 18d, 20d);
        if (values13.isEmpty()) {
            result13 = ColumnPredicate.none(doubleCol);
        } else {
            result13 = ColumnPredicate.buildInList(doubleCol, values13);
        }
        ColumnPredicate<Double> result14;
        final SortedSet<Double> values14 = toValueSet(12d, 14d, 16d, 18d);
        if (values14.isEmpty()) {
            result14 = ColumnPredicate.none(doubleCol);
        } else {
            result14 = ColumnPredicate.buildInList(doubleCol, values14);
        }
        testMerge(result14,
                result13,
                result12
        );

        testMerge(newComparisonPredicate(bigDecimalCol, GREATER_EQUAL, BigDecimal.valueOf(12345, 2)),
                newComparisonPredicate(bigDecimalCol, LESS, BigDecimal.valueOf(67890, 2)),
                new ColumnPredicate<>(RANGE,
                        bigDecimalCol,
                        BigDecimal.valueOf(12345, 2),
                        BigDecimal.valueOf(67890, 2)
                )
        );

        ColumnPredicate<BigDecimal> result9;
        final SortedSet<BigDecimal> values9 = toValueSet(BigDecimal.valueOf(45678, 2));
        if (values9.isEmpty()) {
            result9 = ColumnPredicate.none(bigDecimalCol);
        } else {
            result9 = ColumnPredicate.buildInList(bigDecimalCol, values9);
        }
        ColumnPredicate<BigDecimal> result10;
        final SortedSet<BigDecimal> values10 = toValueSet(BigDecimal.valueOf(45678, 2), BigDecimal.valueOf(98765, 2));
        if (values10.isEmpty()) {
            result10 = ColumnPredicate.none(bigDecimalCol);
        } else {
            result10 = ColumnPredicate.buildInList(bigDecimalCol, values10);
        }
        ColumnPredicate<BigDecimal> result11;
        final SortedSet<BigDecimal> values11 = toValueSet(BigDecimal.valueOf(12345, 2), BigDecimal.valueOf(45678, 2));
        if (values11.isEmpty()) {
            result11 = ColumnPredicate.none(bigDecimalCol);
        } else {
            result11 = ColumnPredicate.buildInList(bigDecimalCol, values11);
        }
        testMerge(result11,
                result10,
                result9
        );

        ColumnPredicate<BigDecimal> result6;
        final SortedSet<BigDecimal> values6 = toValueSet(BigDecimal.valueOf(34567891011L, 2));
        if (values6.isEmpty()) {
            result6 = ColumnPredicate.none(bigDecimalCol);
        } else {
            result6 = ColumnPredicate.buildInList(bigDecimalCol, values6);
        }
        ColumnPredicate<BigDecimal> result7;
        final SortedSet<BigDecimal> values7 = toValueSet(
                BigDecimal.valueOf(34567891011L, 2),
                BigDecimal.valueOf(98765432111L, 2)
        );
        if (values7.isEmpty()) {
            result7 = ColumnPredicate.none(bigDecimalCol);
        } else {
            result7 = ColumnPredicate.buildInList(bigDecimalCol, values7);
        }
        ColumnPredicate<BigDecimal> result8;
        final SortedSet<BigDecimal> values8 = toValueSet(
                BigDecimal.valueOf(12345678910L, 2),
                BigDecimal.valueOf(34567891011L, 2)
        );
        if (values8.isEmpty()) {
            result8 = ColumnPredicate.none(bigDecimalCol);
        } else {
            result8 = ColumnPredicate.buildInList(bigDecimalCol, values8);
        }
        testMerge(result8,
                result7,
                result6
        );

        testMerge(newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        BigDecimal.valueOf(12345678910L, 2)),
                newComparisonPredicate(bigDecimalCol, LESS,
                        BigDecimal.valueOf(67890101112L, 2)),
                new ColumnPredicate(RANGE,
                        bigDecimalCol,
                        BigDecimal.valueOf(12345678910L, 2),
                        BigDecimal.valueOf(67890101112L, 2)
                )
        );

        ColumnPredicate<BigDecimal> result3;
        final SortedSet<BigDecimal> values3 = toValueSet(new BigDecimal("3456789101112131415.16"));
        if (values3.isEmpty()) {
            result3 = ColumnPredicate.none(bigDecimalCol);
        } else {
            result3 = ColumnPredicate.buildInList(bigDecimalCol, values3);
        }
        ColumnPredicate<BigDecimal> result4;
        final SortedSet<BigDecimal> values4 = toValueSet(
                new BigDecimal("3456789101112131415.16"),
                new BigDecimal("9876543212345678910.11")
        );
        if (values4.isEmpty()) {
            result4 = ColumnPredicate.none(bigDecimalCol);
        } else {
            result4 = ColumnPredicate.buildInList(bigDecimalCol, values4);
        }
        ColumnPredicate<BigDecimal> result5;
        final SortedSet<BigDecimal> values5 = toValueSet(
                new BigDecimal("1234567891011121314.15"),
                new BigDecimal("3456789101112131415.16")
        );
        if (values5.isEmpty()) {
            result5 = ColumnPredicate.none(bigDecimalCol);
        } else {
            result5 = ColumnPredicate.buildInList(bigDecimalCol, values5);
        }
        testMerge(result5,
                result4,
                result3
        );

        testMerge(newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        new BigDecimal("1234567891011121314.15")),
                newComparisonPredicate(bigDecimalCol, LESS,
                        new BigDecimal("67891011121314151617.18")),
                new ColumnPredicate<>(RANGE,
                        bigDecimalCol,
                        new BigDecimal("1234567891011121314.15"),
                        new BigDecimal("67891011121314151617.18")
                )
        );

        //TODO byte[] is not comparable for now
//        testMerge(newComparisonPredicate(binaryCol, GREATER_EQUAL,
//                        new byte[] { 0, 1, 2, 3, 4, 5, 6 }),
//                newComparisonPredicate(binaryCol, LESS, new byte[] { 10 }),
//                new ColumnPredicate<>(RANGE,
//                        binaryCol,
//                        DataTypeValue.of(ByteBuffer.wrap(new byte[] { 0, 1, 2, 3, 4, 5, 6 })),
//                        DataTypeValue.of(ByteBuffer.wrap(new byte[] { 10 }))
//                )
//        );

        testMerge(newComparisonPredicate(stringCol, GREATER_EQUAL, "bar"),
                newComparisonPredicate(stringCol, LESS, "foo"),
                new ColumnPredicate<>(RANGE,
                        stringCol,
                        "bar",
                        "foo"
                )
        );

        ByteBuffer valA = ByteBuffer.wrap("a".getBytes(UTF_8));
        ByteBuffer valB = ByteBuffer.wrap("b".getBytes(UTF_8));
        ByteBuffer valC = ByteBuffer.wrap("c".getBytes(UTF_8));
        ByteBuffer valD = ByteBuffer.wrap("d".getBytes(UTF_8));
        ByteBuffer valE = ByteBuffer.wrap("e".getBytes(UTF_8));
        ColumnPredicate<ByteBuffer> result;
        final SortedSet<ByteBuffer> values = toValueSet(ImmutableList.of(valB, valD));
        if (values.isEmpty()) {
            result = ColumnPredicate.none(binaryCol);
        } else {
            result = ColumnPredicate.buildInList(binaryCol, values);
        }
        ColumnPredicate<ByteBuffer> result1;
        final SortedSet<ByteBuffer> values1 = toValueSet(ImmutableList.of(valB, valD, valE));
        if (values1.isEmpty()) {
            result1 = ColumnPredicate.none(binaryCol);
        } else {
            result1 = ColumnPredicate.buildInList(binaryCol, values1);
        }
        ColumnPredicate<ByteBuffer> result2;
        final SortedSet<ByteBuffer> values2 = toValueSet(ImmutableList.of(valA, valB, valC, valD));
        if (values2.isEmpty()) {
            result2 = ColumnPredicate.none(binaryCol);
        } else {
            result2 = ColumnPredicate.buildInList(binaryCol, values2);
        }
        testMerge(result2,
                result1,
                result);
    }

    @Test
    public void testLessEqual() {
        Assert.assertEquals(newComparisonPredicate(byteCol, LESS_EQUAL, (byte)10),
                newComparisonPredicate(byteCol, LESS, (byte)11));
        Assert.assertEquals(newComparisonPredicate(shortCol, LESS_EQUAL, (short)10),
                newComparisonPredicate(shortCol, LESS, (short)11));
        Assert.assertEquals(newComparisonPredicate(intCol, LESS_EQUAL, 10),
                newComparisonPredicate(intCol, LESS, 11));
        Assert.assertEquals(newComparisonPredicate(longCol, LESS_EQUAL, 10L),
                newComparisonPredicate(longCol, LESS, 11L));
        Assert.assertEquals(newComparisonPredicate(floatCol, LESS_EQUAL, 12.345f),
                newComparisonPredicate(floatCol, LESS, Math.nextAfter(12.345f,
                        Float.POSITIVE_INFINITY)));
        Assert.assertEquals(newComparisonPredicate(doubleCol, LESS_EQUAL, 12.345),
                newComparisonPredicate(doubleCol, LESS, Math.nextAfter(12.345,
                        Float.POSITIVE_INFINITY)));
        Assert.assertEquals(
                newComparisonPredicate(bigDecimalCol, LESS_EQUAL,
                        BigDecimal.valueOf(12345, 2)),
                newComparisonPredicate(bigDecimalCol, LESS,
                        BigDecimal.valueOf(12346, 2)));
        Assert.assertEquals(newComparisonPredicate(stringCol, LESS_EQUAL, "a"),
                newComparisonPredicate(stringCol, LESS, "a\0"));
        //TODO byte[] is not comparable for now
//        Assert.assertEquals(
//                newComparisonPredicate(binaryCol, LESS_EQUAL, new byte[] { (byte) 10 }),
//                newComparisonPredicate(binaryCol, LESS, new byte[] { (byte) 10, (byte) 0 }));
        Assert.assertEquals(newComparisonPredicate(byteCol, LESS_EQUAL, Byte.MAX_VALUE),
                ColumnPredicate.isNotNull(byteCol));
        Assert.assertEquals(newComparisonPredicate(shortCol, LESS_EQUAL, Short.MAX_VALUE),
                ColumnPredicate.isNotNull(shortCol));
        Assert.assertEquals(newComparisonPredicate(intCol, LESS_EQUAL, Integer.MAX_VALUE),
                ColumnPredicate.isNotNull(intCol));
        Assert.assertEquals(newComparisonPredicate(longCol, LESS_EQUAL, Long.MAX_VALUE),
                ColumnPredicate.isNotNull(longCol));
        Assert.assertEquals(
                newComparisonPredicate(floatCol, LESS_EQUAL, Float.MAX_VALUE),
                newComparisonPredicate(floatCol, LESS, Float.POSITIVE_INFINITY));
        Assert.assertEquals(
                newComparisonPredicate(floatCol, LESS_EQUAL, Float.POSITIVE_INFINITY),
                ColumnPredicate.isNotNull(floatCol));
        Assert.assertEquals(
                newComparisonPredicate(doubleCol, LESS_EQUAL, Double.MAX_VALUE),
                newComparisonPredicate(doubleCol, LESS, Double.POSITIVE_INFINITY));
        Assert.assertEquals(
                newComparisonPredicate(doubleCol, LESS_EQUAL, Double.POSITIVE_INFINITY),
                ColumnPredicate.isNotNull(doubleCol));
        Assert.assertEquals(newComparisonPredicate(dateCol, LESS_EQUAL, LocalDate.of(2020, 6, 1)),
                newComparisonPredicate(dateCol, LESS, LocalDate.of(2020, 6, 2))
        );
        Assert.assertEquals(newComparisonPredicate(dateCol, LESS_EQUAL, LocalDate.MAX),
                ColumnPredicate.isNotNull(dateCol));
    }

    @Test
    public void testGreater() {
        Assert.assertEquals(newComparisonPredicate(byteCol, GREATER_EQUAL, (byte) 11),
                newComparisonPredicate(byteCol, GREATER, (byte) 10)
        );
        Assert.assertEquals(newComparisonPredicate(shortCol, GREATER_EQUAL, (short)11),
                newComparisonPredicate(shortCol, GREATER, (short)10)
        );
        Assert.assertEquals(newComparisonPredicate(intCol, GREATER_EQUAL, 11),
                newComparisonPredicate(intCol, GREATER, 10)
        );
        Assert.assertEquals(newComparisonPredicate(longCol, GREATER_EQUAL, 11L),
                newComparisonPredicate(longCol, GREATER, 10L)
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
        //TODO byte[] is not comparable for now
//        Assert.assertEquals(
//                newComparisonPredicate(binaryCol, GREATER_EQUAL,
//                        new byte[] { (byte) 10, (byte) 0 }),
//                newComparisonPredicate(binaryCol, GREATER, new byte[] { (byte) 10 })
//        );

        Assert.assertEquals(ColumnPredicate.none(byteCol),
                newComparisonPredicate(byteCol, GREATER, Byte.MAX_VALUE)
        );
        Assert.assertEquals(ColumnPredicate.none(shortCol),
                newComparisonPredicate(shortCol, GREATER, Short.MAX_VALUE)
        );
        Assert.assertEquals(ColumnPredicate.none(intCol),
                newComparisonPredicate(intCol, GREATER, Integer.MAX_VALUE)
        );
        Assert.assertEquals(ColumnPredicate.none(longCol),
                newComparisonPredicate(longCol, GREATER, Long.MAX_VALUE)
        );
        Assert.assertEquals(
                newComparisonPredicate(floatCol, GREATER_EQUAL, Float.POSITIVE_INFINITY),
                newComparisonPredicate(floatCol, GREATER, Float.MAX_VALUE)
        );
        Assert.assertEquals(
                ColumnPredicate.none(floatCol),
                newComparisonPredicate(floatCol, GREATER, Float.POSITIVE_INFINITY)
        );
        Assert.assertEquals(
                newComparisonPredicate(doubleCol, GREATER_EQUAL, Double.POSITIVE_INFINITY),
                newComparisonPredicate(doubleCol, GREATER, Double.MAX_VALUE)
        );
        Assert.assertEquals(
                ColumnPredicate.none(doubleCol),
                newComparisonPredicate(doubleCol, GREATER, Double.POSITIVE_INFINITY)
        );
        Assert.assertEquals(newComparisonPredicate(dateCol, GREATER_EQUAL,
                        LocalDate.of(2020, 6, 15)),
                newComparisonPredicate(dateCol, GREATER, LocalDate.of(2020, 6, 14))
        );
    }

    @Test
    public void testLess() {
        Assert.assertEquals(newComparisonPredicate(byteCol, LESS, Byte.MIN_VALUE),
                ColumnPredicate.none(byteCol));
        Assert.assertEquals(newComparisonPredicate(shortCol, LESS, Short.MIN_VALUE),
                ColumnPredicate.none(shortCol));
        Assert.assertEquals(newComparisonPredicate(intCol, LESS, Integer.MIN_VALUE),
                ColumnPredicate.none(intCol));
        Assert.assertEquals(newComparisonPredicate(longCol, LESS, Long.MIN_VALUE),
                ColumnPredicate.none(longCol));
        Assert.assertEquals(
                newComparisonPredicate(floatCol, LESS, Float.NEGATIVE_INFINITY),
                ColumnPredicate.none(floatCol));
        Assert.assertEquals(
                newComparisonPredicate(doubleCol, LESS, Double.NEGATIVE_INFINITY),
                ColumnPredicate.none(doubleCol));
        Assert.assertEquals(newComparisonPredicate(bigDecimalCol, LESS,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL32_PRECISION, 2)),
                ColumnPredicate.none(bigDecimalCol));
        Assert.assertEquals(newComparisonPredicate(bigDecimalCol, LESS,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL64_PRECISION, 2)),
                ColumnPredicate.none(bigDecimalCol));
        Assert.assertEquals(newComparisonPredicate(bigDecimalCol, LESS,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL128_PRECISION, 2)),
                ColumnPredicate.none(bigDecimalCol));
        Assert.assertEquals(newComparisonPredicate(stringCol, LESS, ""),
                ColumnPredicate.none(stringCol));
        //TODO byte[] is not comparable for now
//        Assert.assertEquals(newComparisonPredicate(binaryCol, LESS, new byte[] {}),
//                ColumnPredicate.none(binaryCol));
        Assert.assertEquals(newComparisonPredicate(dateCol, LESS, LocalDate.MIN), ColumnPredicate.none(dateCol));
    }

    @Test
    public void testGreaterEqual() {
        Assert.assertEquals(
                newComparisonPredicate(byteCol, GREATER_EQUAL, Byte.MIN_VALUE),
                ColumnPredicate.isNotNull(byteCol));
        Assert.assertEquals(
                newComparisonPredicate(shortCol, GREATER_EQUAL, Short.MIN_VALUE),
                ColumnPredicate.isNotNull(shortCol));
        Assert.assertEquals(
                newComparisonPredicate(intCol, GREATER_EQUAL, Integer.MIN_VALUE),
                ColumnPredicate.isNotNull(intCol));
        Assert.assertEquals(
                newComparisonPredicate(longCol, GREATER_EQUAL, Long.MIN_VALUE),
                ColumnPredicate.isNotNull(longCol));
        Assert.assertEquals(
                newComparisonPredicate(floatCol, GREATER_EQUAL, Float.NEGATIVE_INFINITY),
                ColumnPredicate.isNotNull(floatCol));
        Assert.assertEquals(
                newComparisonPredicate(doubleCol, GREATER_EQUAL, Double.NEGATIVE_INFINITY),
                ColumnPredicate.isNotNull(doubleCol));
        Assert.assertEquals(newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL32_PRECISION, 2)),
                ColumnPredicate.isNotNull(bigDecimalCol));
        Assert.assertEquals(newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL64_PRECISION, 2)),
                ColumnPredicate.isNotNull(bigDecimalCol));
        Assert.assertEquals(newComparisonPredicate(bigDecimalCol, GREATER_EQUAL,
                        DecimalUtil.minValue(DecimalUtil.MAX_DECIMAL128_PRECISION, 2)),
                ColumnPredicate.isNotNull(bigDecimalCol));
        Assert.assertEquals(newComparisonPredicate(stringCol, GREATER_EQUAL, ""),
                ColumnPredicate.isNotNull(stringCol));
//TODO byte[] is not comparable for now
//        Assert.assertEquals(
//                newComparisonPredicate(binaryCol, GREATER_EQUAL, new byte[] {}),
//                ColumnPredicate.isNotNull(binaryCol));

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
                newComparisonPredicate(dateCol, GREATER_EQUAL, LocalDate.MIN),
                ColumnPredicate.isNotNull(dateCol));
    }


    @Test
    public void testToString() {
        String actual = newComparisonPredicate(boolCol, EQUAL, true).toString();
        Assert.assertEquals(actual,
                "`bool` = true");
        Assert.assertEquals(newComparisonPredicate(byteCol, EQUAL, (byte)11).toString(),
                "`byte` = 11");
        Assert.assertEquals(newComparisonPredicate(shortCol, EQUAL, (short)11).toString(),
                "`short` = 11");
        Assert.assertEquals(newComparisonPredicate(intCol, EQUAL, -123).toString(),
                "`int` = -123");
        Assert.assertEquals(newComparisonPredicate(longCol, EQUAL, 5454L).toString(),
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
        Assert.assertEquals(intInList(10, 0, -10).toString(),
                "`int` IN (-10, 0, 10)");
        Assert.assertEquals(ColumnPredicate.isNotNull(stringCol).toString(),
                "`string` IS NOT NULL");
        Assert.assertEquals(ColumnPredicate.isNull(stringCol).toString(),
                "`string` IS NULL");
        Assert.assertEquals(newComparisonPredicate(stringCol, EQUAL, "my varchar").toString(),
                "`string` = \"my varchar\"");
        Assert.assertEquals(ColumnPredicate.isNotNull(binaryCol).toString(),
                "`binary` IS NOT NULL");
        Assert.assertEquals(ColumnPredicate.isNull(binaryCol).toString(),
                "`binary` IS NULL");
        // IS NULL predicate on non-nullable column = NONE predicate
        Assert.assertEquals(ColumnPredicate.isNull(intCol).toString(),
                "`int` NONE");

        ColumnPredicate<Boolean> result11;
        final SortedSet<Boolean> values9 = toValueSet(true);
        if (values9.isEmpty()) {
            result11 = ColumnPredicate.none(boolCol);
        } else {
            result11 = ColumnPredicate.buildInList(boolCol, values9);
        }
        Assert.assertEquals(result11.toString(), "`bool` = true");
        ColumnPredicate<Boolean> result10;
        final SortedSet<Boolean> values8 = toValueSet(false);
        if (values8.isEmpty()) {
            result10 = ColumnPredicate.none(boolCol);
        } else {
            result10 = ColumnPredicate.buildInList(boolCol, values8);
        }
        Assert.assertEquals(result10.toString(), "`bool` = false");
        ColumnPredicate<Byte> result8;
        final SortedSet<Byte> values6 = toValueSet((byte) 1, (byte) 10, (byte) 100);
        if (values6.isEmpty()) {
            result8 = ColumnPredicate.none(byteCol);
        } else {
            result8 = ColumnPredicate.buildInList(byteCol, values6);
        }
        Assert.assertEquals(result8.toString(), "`byte` IN (1, 10, 100)");
        ColumnPredicate<Short> result7;
        final SortedSet<Short> values5 = toValueSet((short) 1, (short) 100, (short) 10);
        if (values5.isEmpty()) {
            result7 = ColumnPredicate.none(shortCol);
        } else {
            result7 = ColumnPredicate.buildInList(shortCol, values5);
        }
        Assert.assertEquals(result7.toString(), "`short` IN (1, 10, 100)");
        ColumnPredicate<Integer> result6;
        final SortedSet<Integer> values4 = toValueSet(1, 100, 10);
        if (values4.isEmpty()) {
            result6 = ColumnPredicate.none(intCol);
        } else {
            result6 = ColumnPredicate.buildInList(intCol, values4);
        }
        Assert.assertEquals(result6.toString(), "`int` IN (1, 10, 100)");
        ColumnPredicate<Long> result5;
        final SortedSet<Long> values3 = toValueSet(1L, 100L, 10L);
        if (values3.isEmpty()) {
            result5 = ColumnPredicate.none(longCol);
        } else {
            result5 = ColumnPredicate.buildInList(longCol, values3);
        }
        Assert.assertEquals(result5.toString(), "`long` IN (1, 10, 100)");
        ColumnPredicate<Float> result4;
        final SortedSet<Float> values2 = toValueSet(123.456f, 78.9f);
        if (values2.isEmpty()) {
            result4 = ColumnPredicate.none(floatCol);
        } else {
            result4 = ColumnPredicate.buildInList(floatCol, values2);
        }
        Assert.assertEquals(result4.toString(), "`float` IN (78.9, 123.456)");
        ColumnPredicate<Double> result3;
        final SortedSet<Double> values1 = toValueSet(123.456d, 78.9d);
        if (values1.isEmpty()) {
            result3 = ColumnPredicate.none(doubleCol);
        } else {
            result3 = ColumnPredicate.buildInList(doubleCol, values1);
        }
        Assert.assertEquals(result3.toString(), "`double` IN (78.9, 123.456)");
        ColumnPredicate<String> result2;
        final SortedSet<String> values = toValueSet("my string", "a");
        if (values.isEmpty()) {
            result2 = ColumnPredicate.none(stringCol);
        } else {
            result2 = ColumnPredicate.buildInList(stringCol, values);
        }
        Assert.assertEquals(result2.toString(),
                "`string` IN (\"a\", \"my string\")");

        ByteBuffer firstBuffer = ByteBuffer.wrap(new byte[]{(byte) 0xAB, (byte) 0x01, (byte) 0xCD});
        ByteBuffer secondBuffer = ByteBuffer.wrap(new byte[]{(byte) 0x00});
        SortedSet<ByteBuffer> setOfStuff = toValueSet(firstBuffer, secondBuffer);
        //TODO well, java does not sort ByteBuffer as we expected, so what do we do about it?
        //     ...and why it is in the "testToString" method
//        Assert.assertEquals(setOfStuff.first(), secondBuffer);
//        Assert.assertEquals(setOfStuff.last(), firstBuffer);
//        ColumnPredicate<ByteBuffer> result1;
//        if (setOfStuff.isEmpty()) {
//            result1 = ColumnPredicate.none(binaryCol);
//        } else {
//            result1 = ColumnPredicate.buildInList(binaryCol, setOfStuff);
//        }
//        Assert.assertEquals(result1.toString(), "`binary` IN (0x00, 0xAB01CD)");


        Assert.assertEquals(ColumnPredicate.isNull(dateCol).toString(), "`date` IS NULL");
        Assert.assertEquals(ColumnPredicate.isNotNull(dateCol).toString(),
                "`date` IS NOT NULL");

        Assert.assertEquals(newComparisonPredicate(dateCol, EQUAL, LocalDate.of(2020, 6, 16))
                        .toString(),
                "`date` = 2020-06-16");
        SortedSet<ChronoLocalDate> dataTypeValues = toValueSet(
                LocalDate.of(2020, 6, 16),
                LocalDate.of(2019, 1, 1),
                LocalDate.of(2020, 11, 10));
        ColumnPredicate<ChronoLocalDate> result;
        if (dataTypeValues.isEmpty()) {
            result = ColumnPredicate.none(dateCol);
        } else {
            result = ColumnPredicate.buildInList(dateCol, dataTypeValues);
        }
        Assert.assertEquals(result.toString(),
                "`date` IN (2019-01-01, 2020-06-16, 2020-11-10)");
    }

    @SafeVarargs
    public static <T extends Comparable<T>> SortedSet<T> toValueSet(T... values) {
        return toValueSet(ImmutableList.copyOf(values));
    }

    public static <T extends Comparable<T>> SortedSet<T> toValueSet(List<T> list) {
        return list.stream()
                .sorted()
                .collect(Collectors.toCollection(TreeSet::new));
    }
    
    private static <T extends Comparable<T>> ColumnPredicate<T> newComparisonPredicate(CyodaColumnHandle columnHandle,
                                                                 ColumnPredicate.ComparisonOp op, T value){
        return columnHandle.getConverter().newComparisonPredicateFromJava(columnHandle, op, value);
    }

}