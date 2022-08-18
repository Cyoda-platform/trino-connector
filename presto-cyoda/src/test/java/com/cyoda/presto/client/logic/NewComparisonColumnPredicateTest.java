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
import io.airlift.slice.Slices;
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
        if (valueSet.isEmpty()) {
            return ColumnPredicate.none(intCol);
        }
        // IN (true, false) predicates can be simplified to IS NOT NULL.
        if (intCol.getDataType() == DataType.BOOLEAN && valueSet.size() > 1) {
            return ColumnPredicate.isNotNull(intCol);
        }
        return ColumnPredicate.buildInList(intCol, valueSet);
    }

    private ColumnPredicate<Long> longInList(Long... values) {
        SortedSet<DataTypeValue<Long>> valueSet = toDataTypeValue(values);
        if (valueSet.isEmpty()) {
            return ColumnPredicate.none(intCol);
        }
        // IN (true, false) predicates can be simplified to IS NOT NULL.
        if (intCol.getDataType() == DataType.BOOLEAN && valueSet.size() > 1) {
            return ColumnPredicate.isNotNull(intCol);
        }
        return ColumnPredicate.buildInList(intCol, valueSet);
    }

    private ColumnPredicate<Boolean> boolInList(Boolean... values) {
        SortedSet<DataTypeValue<Boolean>> valueSet = toDataTypeValue(values);
        if (valueSet.isEmpty()) {
            return ColumnPredicate.none(boolCol);
        }
        // IN (true, false) predicates can be simplified to IS NOT NULL.
        if (boolCol.getDataType() == DataType.BOOLEAN && valueSet.size() > 1) {
            return ColumnPredicate.isNotNull(boolCol);
        }
        return ColumnPredicate.buildInList(boolCol, valueSet);
    }

    private ColumnPredicate<String> stringInList(String... values) {
        SortedSet<DataTypeValue<String >> valueSet = toDataTypeValue(values);
        if (valueSet.isEmpty()) {
            return ColumnPredicate.none(stringCol);
        }
        // IN (true, false) predicates can be simplified to IS NOT NULL.
        if (stringCol.getDataType() == DataType.BOOLEAN && valueSet.size() > 1) {
            return ColumnPredicate.isNotNull(stringCol);
        }
        return ColumnPredicate.buildInList(stringCol, valueSet);
    }

    @BeforeMethod
    public void setup() {

        int pos = 0;
        boolCol = new CyodaColumnHandle(CONNECTOR_ID,"bool", BooleanType.BOOLEAN, DataType.BOOLEAN,pos++, REQUEST_HANDLER_KEY);
        byteCol = new CyodaColumnHandle(CONNECTOR_ID,"byte", TinyintType.TINYINT,DataType.BYTE,pos++, REQUEST_HANDLER_KEY);
        shortCol = new CyodaColumnHandle(CONNECTOR_ID,"short", SmallintType.SMALLINT,DataType.SHORT, pos++, REQUEST_HANDLER_KEY);
        intCol = new CyodaColumnHandle(CONNECTOR_ID,"int", IntegerType.INTEGER, DataType.INTEGER, pos++, REQUEST_HANDLER_KEY);

        longCol = new CyodaColumnHandle(CONNECTOR_ID,"long", DecimalType.createDecimalType(), DataType.LONG, pos++, REQUEST_HANDLER_KEY);

        floatCol = new CyodaColumnHandle(CONNECTOR_ID,"float", RealType.REAL, DataType.FLOAT,pos++, REQUEST_HANDLER_KEY);
        doubleCol = new CyodaColumnHandle(CONNECTOR_ID,"double", DoubleType.DOUBLE, DataType.DOUBLE,pos++, REQUEST_HANDLER_KEY);
        stringCol = new CyodaColumnHandle(CONNECTOR_ID,"string", VarcharType.VARCHAR, DataType.STRING,pos++, REQUEST_HANDLER_KEY);
        binaryCol = new CyodaColumnHandle(CONNECTOR_ID,"binary", VarbinaryType.VARBINARY, DataType.BYTE_BUFFER,pos++, REQUEST_HANDLER_KEY);

        bigDecimalCol = new CyodaColumnHandle(CONNECTOR_ID,"bigDecimal", BigDecimalType.BIG_DECIMAL_TYPE, DataType.BIG_DECIMAL,pos++, REQUEST_HANDLER_KEY);

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
                new ColumnPredicate<>(EQUALITY, boolCol, DataTypeValue.of(false), null)
        );

        testMerge(newComparisonPredicate(boolCol, GREATER_EQUAL, false),
                newComparisonPredicate(boolCol, LESS_EQUAL, true),
                ColumnPredicate.isNotNull(boolCol));

        testMerge(newComparisonPredicate(byteCol, GREATER_EQUAL, 0),
                newComparisonPredicate(byteCol, LESS, 10),
                new ColumnPredicate<>(RANGE,
                        byteCol,
                        DataTypeValue.of(0),
                        DataTypeValue.of(10)
                )
        );

        ColumnPredicate<Byte> result24;
        final SortedSet<DataTypeValue<Byte>> values24 = toDataTypeValue((byte) 14, (byte) 18);
        if (values24.isEmpty()) {
            result24 = ColumnPredicate.none(byteCol);
        } else if (byteCol.getDataType() == DataType.BOOLEAN && values24.size() > 1) {
            result24 = ColumnPredicate.isNotNull(byteCol);
        } else {
            result24 = ColumnPredicate.buildInList(byteCol, values24);
        }
        ColumnPredicate<Byte> result25;
        final SortedSet<DataTypeValue<Byte>> values25 = toDataTypeValue((byte) 14, (byte) 18, (byte) 20);
        if (values25.isEmpty()) {
            result25 = ColumnPredicate.none(byteCol);
        } else if (byteCol.getDataType() == DataType.BOOLEAN && values25.size() > 1) {
            result25 = ColumnPredicate.isNotNull(byteCol);
        } else {
            result25 = ColumnPredicate.buildInList(byteCol, values25);
        }
        ColumnPredicate<Byte> result26;
        final SortedSet<DataTypeValue<Byte>> values26 = toDataTypeValue((byte) 12, (byte) 14, (byte) 16, (byte) 18);
        if (values26.isEmpty()) {
            result26 = ColumnPredicate.none(byteCol);
        } else if (byteCol.getDataType() == DataType.BOOLEAN && values26.size() > 1) {
            result26 = ColumnPredicate.isNotNull(byteCol);
        } else {
            result26 = ColumnPredicate.buildInList(byteCol, values26);
        }
        testMerge(result26,
                result25,
                result24
        );

        testMerge(newComparisonPredicate(shortCol, GREATER_EQUAL, (short)0),
                newComparisonPredicate(shortCol, LESS, (short)10),
                new ColumnPredicate<>(RANGE,
                        shortCol,
                        DataTypeValue.of((short) 0),
                        DataTypeValue.of((short) 10)));

        ColumnPredicate<Short> result21;
        final SortedSet<DataTypeValue<Short>> values21 = toDataTypeValue((short) 14, (short) 18);
        if (values21.isEmpty()) {
            result21 = ColumnPredicate.none(shortCol);
        } else if (shortCol.getDataType() == DataType.BOOLEAN && values21.size() > 1) {
            result21 = ColumnPredicate.isNotNull(shortCol);
        } else {
            result21 = ColumnPredicate.buildInList(shortCol, values21);
        }
        ColumnPredicate<Short> result22;
        final SortedSet<DataTypeValue<Short>> values22 = toDataTypeValue((short) 14, (short) 18, (short) 20);
        if (values22.isEmpty()) {
            result22 = ColumnPredicate.none(shortCol);
        } else if (shortCol.getDataType() == DataType.BOOLEAN && values22.size() > 1) {
            result22 = ColumnPredicate.isNotNull(shortCol);
        } else {
            result22 = ColumnPredicate.buildInList(shortCol, values22);
        }
        ColumnPredicate<Short> result23;
        final SortedSet<DataTypeValue<Short>> values23 = toDataTypeValue((short) 12, (short) 14, (short) 16, (short) 18);
        if (values23.isEmpty()) {
            result23 = ColumnPredicate.none(shortCol);
        } else if (shortCol.getDataType() == DataType.BOOLEAN && values23.size() > 1) {
            result23 = ColumnPredicate.isNotNull(shortCol);
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
                        DataTypeValue.of(0L),
                        DataTypeValue.of(10L)));

        ColumnPredicate<Long> result18;
        final SortedSet<DataTypeValue<Long>> values18 = toDataTypeValue(14L, 18L);
        if (values18.isEmpty()) {
            result18 = ColumnPredicate.none(longCol);
        } else if (longCol.getDataType() == DataType.BOOLEAN && values18.size() > 1) {
            result18 = ColumnPredicate.isNotNull(longCol);
        } else {
            result18 = ColumnPredicate.buildInList(longCol, values18);
        }
        ColumnPredicate<Long> result19;
        final SortedSet<DataTypeValue<Long>> values19 = toDataTypeValue(14L, 18L, 20L);
        if (values19.isEmpty()) {
            result19 = ColumnPredicate.none(longCol);
        } else if (longCol.getDataType() == DataType.BOOLEAN && values19.size() > 1) {
            result19 = ColumnPredicate.isNotNull(longCol);
        } else {
            result19 = ColumnPredicate.buildInList(longCol, values19);
        }
        ColumnPredicate<Long> result20;
        final SortedSet<DataTypeValue<Long>> values20 = toDataTypeValue(12L, 14L, 16L, 18L);
        if (values20.isEmpty()) {
            result20 = ColumnPredicate.none(longCol);
        } else if (longCol.getDataType() == DataType.BOOLEAN && values20.size() > 1) {
            result20 = ColumnPredicate.isNotNull(longCol);
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
                        DataTypeValue.of(123.45f),
                        DataTypeValue.of(678.90f)));

        ColumnPredicate<Float> result15;
        final SortedSet<DataTypeValue<Float>> values15 = toDataTypeValue(14f, 18f);
        if (values15.isEmpty()) {
            result15 = ColumnPredicate.none(floatCol);
        } else if (floatCol.getDataType() == DataType.BOOLEAN && values15.size() > 1) {
            result15 = ColumnPredicate.isNotNull(floatCol);
        } else {
            result15 = ColumnPredicate.buildInList(floatCol, values15);
        }
        ColumnPredicate<Float> result16;
        final SortedSet<DataTypeValue<Float>> values16 = toDataTypeValue(14f, 18f, 20f);
        if (values16.isEmpty()) {
            result16 = ColumnPredicate.none(floatCol);
        } else if (floatCol.getDataType() == DataType.BOOLEAN && values16.size() > 1) {
            result16 = ColumnPredicate.isNotNull(floatCol);
        } else {
            result16 = ColumnPredicate.buildInList(floatCol, values16);
        }
        ColumnPredicate<Float> result17;
        final SortedSet<DataTypeValue<Float>> values17 = toDataTypeValue(12f, 14f, 16f, 18f);
        if (values17.isEmpty()) {
            result17 = ColumnPredicate.none(floatCol);
        } else if (floatCol.getDataType() == DataType.BOOLEAN && values17.size() > 1) {
            result17 = ColumnPredicate.isNotNull(floatCol);
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
                        DataTypeValue.of(123.45),
                        DataTypeValue.of(678.90)));

        ColumnPredicate<Double> result12;
        final SortedSet<DataTypeValue<Double>> values12 = toDataTypeValue(14d, 18d);
        if (values12.isEmpty()) {
            result12 = ColumnPredicate.none(doubleCol);
        } else if (doubleCol.getDataType() == DataType.BOOLEAN && values12.size() > 1) {
            result12 = ColumnPredicate.isNotNull(doubleCol);
        } else {
            result12 = ColumnPredicate.buildInList(doubleCol, values12);
        }
        ColumnPredicate<Double> result13;
        final SortedSet<DataTypeValue<Double>> values13 = toDataTypeValue(14d, 18d, 20d);
        if (values13.isEmpty()) {
            result13 = ColumnPredicate.none(doubleCol);
        } else if (doubleCol.getDataType() == DataType.BOOLEAN && values13.size() > 1) {
            result13 = ColumnPredicate.isNotNull(doubleCol);
        } else {
            result13 = ColumnPredicate.buildInList(doubleCol, values13);
        }
        ColumnPredicate<Double> result14;
        final SortedSet<DataTypeValue<Double>> values14 = toDataTypeValue(12d, 14d, 16d, 18d);
        if (values14.isEmpty()) {
            result14 = ColumnPredicate.none(doubleCol);
        } else if (doubleCol.getDataType() == DataType.BOOLEAN && values14.size() > 1) {
            result14 = ColumnPredicate.isNotNull(doubleCol);
        } else {
            result14 = ColumnPredicate.buildInList(doubleCol, values14);
        }
        testMerge(result14,
                result13,
                result12
        );

        testMerge(newComparisonPredicate(bigDecimalCol, GREATER_EQUAL, BigDecimal.valueOf(12345, 2)),
                newComparisonPredicate(bigDecimalCol, LESS, BigDecimal.valueOf(67890,2)),
                new ColumnPredicate<>(RANGE,
                        bigDecimalCol,
                        DataTypeValue.of(BigDecimal.valueOf(12345, 2)),
                        DataTypeValue.of(BigDecimal.valueOf(67890, 2))
                )
        );

        ColumnPredicate<BigDecimal> result9;
        final SortedSet<DataTypeValue<BigDecimal>> values9 = toDataTypeValue(BigDecimal.valueOf(45678, 2));
        if (values9.isEmpty()) {
            result9 = ColumnPredicate.none(bigDecimalCol);
        } else if (bigDecimalCol.getDataType() == DataType.BOOLEAN && values9.size() > 1) {
            result9 = ColumnPredicate.isNotNull(bigDecimalCol);
        } else {
            result9 = ColumnPredicate.buildInList(bigDecimalCol, values9);
        }
        ColumnPredicate<BigDecimal> result10;
        final SortedSet<DataTypeValue<BigDecimal>> values10 = toDataTypeValue(BigDecimal.valueOf(45678, 2), BigDecimal.valueOf(98765, 2));
        if (values10.isEmpty()) {
            result10 = ColumnPredicate.none(bigDecimalCol);
        } else if (bigDecimalCol.getDataType() == DataType.BOOLEAN && values10.size() > 1) {
            result10 = ColumnPredicate.isNotNull(bigDecimalCol);
        } else {
            result10 = ColumnPredicate.buildInList(bigDecimalCol, values10);
        }
        ColumnPredicate<BigDecimal> result11;
        final SortedSet<DataTypeValue<BigDecimal>> values11 = toDataTypeValue(BigDecimal.valueOf(12345, 2), BigDecimal.valueOf(45678, 2));
        if (values11.isEmpty()) {
            result11 = ColumnPredicate.none(bigDecimalCol);
        } else if (bigDecimalCol.getDataType() == DataType.BOOLEAN && values11.size() > 1) {
            result11 = ColumnPredicate.isNotNull(bigDecimalCol);
        } else {
            result11 = ColumnPredicate.buildInList(bigDecimalCol, values11);
        }
        testMerge(result11,
                result10,
                result9
        );

        ColumnPredicate<BigDecimal> result6;
        final SortedSet<DataTypeValue<BigDecimal>> values6 = toDataTypeValue(BigDecimal.valueOf(34567891011L, 2));
        if (values6.isEmpty()) {
            result6 = ColumnPredicate.none(bigDecimalCol);
        } else if (bigDecimalCol.getDataType() == DataType.BOOLEAN && values6.size() > 1) {
            result6 = ColumnPredicate.isNotNull(bigDecimalCol);
        } else {
            result6 = ColumnPredicate.buildInList(bigDecimalCol, values6);
        }
        ColumnPredicate<BigDecimal> result7;
        final SortedSet<DataTypeValue<BigDecimal>> values7 = toDataTypeValue(
                BigDecimal.valueOf(34567891011L, 2),
                BigDecimal.valueOf(98765432111L, 2)
        );
        if (values7.isEmpty()) {
            result7 = ColumnPredicate.none(bigDecimalCol);
        } else if (bigDecimalCol.getDataType() == DataType.BOOLEAN && values7.size() > 1) {
            result7 = ColumnPredicate.isNotNull(bigDecimalCol);
        } else {
            result7 = ColumnPredicate.buildInList(bigDecimalCol, values7);
        }
        ColumnPredicate<BigDecimal> result8;
        final SortedSet<DataTypeValue<BigDecimal>> values8 = toDataTypeValue(
                BigDecimal.valueOf(12345678910L, 2),
                BigDecimal.valueOf(34567891011L, 2)
        );
        if (values8.isEmpty()) {
            result8 = ColumnPredicate.none(bigDecimalCol);
        } else if (bigDecimalCol.getDataType() == DataType.BOOLEAN && values8.size() > 1) {
            result8 = ColumnPredicate.isNotNull(bigDecimalCol);
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
                        BigDecimal.valueOf(67890101112L,2)),
                new ColumnPredicate(RANGE,
                        bigDecimalCol,
                        DataTypeValue.of(BigDecimal.valueOf(12345678910L, 2)),
                        DataTypeValue.of(BigDecimal.valueOf(67890101112L, 2))
                )
        );

        ColumnPredicate<BigDecimal> result3;
        final SortedSet<DataTypeValue<BigDecimal>> values3 = toDataTypeValue(new BigDecimal("3456789101112131415.16"));
        if (values3.isEmpty()) {
            result3 = ColumnPredicate.none(bigDecimalCol);
        } else if (bigDecimalCol.getDataType() == DataType.BOOLEAN && values3.size() > 1) {
            result3 = ColumnPredicate.isNotNull(bigDecimalCol);
        } else {
            result3 = ColumnPredicate.buildInList(bigDecimalCol, values3);
        }
        ColumnPredicate<BigDecimal> result4;
        final SortedSet<DataTypeValue<BigDecimal>> values4 = toDataTypeValue(
                new BigDecimal("3456789101112131415.16"),
                new BigDecimal("9876543212345678910.11")
        );
        if (values4.isEmpty()) {
            result4 = ColumnPredicate.none(bigDecimalCol);
        } else if (bigDecimalCol.getDataType() == DataType.BOOLEAN && values4.size() > 1) {
            result4 = ColumnPredicate.isNotNull(bigDecimalCol);
        } else {
            result4 = ColumnPredicate.buildInList(bigDecimalCol, values4);
        }
        ColumnPredicate<BigDecimal> result5;
        final SortedSet<DataTypeValue<BigDecimal>> values5 = toDataTypeValue(
                new BigDecimal("1234567891011121314.15"),
                new BigDecimal("3456789101112131415.16")
        );
        if (values5.isEmpty()) {
            result5 = ColumnPredicate.none(bigDecimalCol);
        } else if (bigDecimalCol.getDataType() == DataType.BOOLEAN && values5.size() > 1) {
            result5 = ColumnPredicate.isNotNull(bigDecimalCol);
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
        ColumnPredicate<ByteBuffer> result;
        final SortedSet<DataTypeValue<ByteBuffer>> values = toDataTypeValue(ImmutableList.of(valB, valD));
        if (values.isEmpty()) {
            result = ColumnPredicate.none(binaryCol);
        } else if (binaryCol.getDataType() == DataType.BOOLEAN && values.size() > 1) {
            result = ColumnPredicate.isNotNull(binaryCol);
        } else {
            result = ColumnPredicate.buildInList(binaryCol, values);
        }
        ColumnPredicate<ByteBuffer> result1;
        final SortedSet<DataTypeValue<ByteBuffer>> values1 = toDataTypeValue(ImmutableList.of(valB, valD, valE));
        if (values1.isEmpty()) {
            result1 = ColumnPredicate.none(binaryCol);
        } else if (binaryCol.getDataType() == DataType.BOOLEAN && values1.size() > 1) {
            result1 = ColumnPredicate.isNotNull(binaryCol);
        } else {
            result1 = ColumnPredicate.buildInList(binaryCol, values1);
        }
        ColumnPredicate<ByteBuffer> result2;
        final SortedSet<DataTypeValue<ByteBuffer>> values2 = toDataTypeValue(ImmutableList.of(valA, valB, valC, valD));
        if (values2.isEmpty()) {
            result2 = ColumnPredicate.none(binaryCol);
        } else if (binaryCol.getDataType() == DataType.BOOLEAN && values2.size() > 1) {
            result2 = ColumnPredicate.isNotNull(binaryCol);
        } else {
            result2 = ColumnPredicate.buildInList(binaryCol, values2);
        }
        testMerge(result2,
                result1,
                result);
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
        Assert.assertEquals(newComparisonPredicate(dateCol, LESS_EQUAL, LocalDate.of(2020,6,1)),
                newComparisonPredicate(dateCol, LESS, LocalDate.of(2020,6,2))
        );
        Assert.assertEquals(newComparisonPredicate(dateCol, LESS_EQUAL, LocalDate.MAX.toEpochDay()),
                ColumnPredicate.isNotNull(dateCol));
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
                LocalDate.of(2020,6,15)),
                newComparisonPredicate(dateCol, GREATER, LocalDate.of(2020,6,14))
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
        Assert.assertEquals(newComparisonPredicate(binaryCol, LESS, new byte[] {}),
                ColumnPredicate.none(binaryCol));
        Assert.assertEquals(newComparisonPredicate(dateCol, LESS,
                LocalDate.MIN.toEpochDay()), ColumnPredicate.none(dateCol));
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
        Assert.assertEquals(
                newComparisonPredicate(binaryCol, GREATER_EQUAL, new byte[] {}),
                ColumnPredicate.isNotNull(binaryCol));

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
                ColumnPredicate.isNotNull(dateCol));
        Assert.assertEquals(
                newComparisonPredicate(dateCol, GREATER_EQUAL, LocalDate.MIN.toEpochDay()),
                ColumnPredicate.isNotNull(dateCol));
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
                        Slices.utf8Slice(BigDecimal.valueOf(12345,2).toString())),
                newComparisonPredicate(bigDecimalCol, EQUAL, BigDecimal.valueOf(12345,2))
        );
        Assert.assertEquals(
                newComparisonPredicateFromNative(stringCol, EQUAL, DataTypeValue.of("a").asSlice(VarcharType.VARCHAR)),
                newComparisonPredicate(stringCol, EQUAL, "a")
        );
        Assert.assertEquals(
                newComparisonPredicateFromNative(binaryCol, EQUAL, DataTypeValue.of(new byte[] { (byte) 10 }).asSlice(VarbinaryType.VARBINARY)),
                newComparisonPredicate(binaryCol, EQUAL, new byte[] { (byte) 10 })
        );
        Assert.assertEquals(newComparisonPredicateFromNative(binaryCol, EQUAL, DataTypeValue.of("a".getBytes(UTF_8)).asSlice(VarbinaryType.VARBINARY)),
                newComparisonPredicate(binaryCol, EQUAL, "a".getBytes(UTF_8))
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
        final SortedSet<DataTypeValue<Boolean>> values9 = toDataTypeValue(true);
        if (values9.isEmpty()) {
            result11 = ColumnPredicate.none(boolCol);
        } else if (boolCol.getDataType() == DataType.BOOLEAN && values9.size() > 1) {
            result11 = ColumnPredicate.isNotNull(boolCol);
        } else {
            result11 = ColumnPredicate.buildInList(boolCol, values9);
        }
        Assert.assertEquals(result11.toString(), "`bool` = true");
        ColumnPredicate<Boolean> result10;
        final SortedSet<DataTypeValue<Boolean>> values8 = toDataTypeValue(false);
        if (values8.isEmpty()) {
            result10 = ColumnPredicate.none(boolCol);
        } else if (boolCol.getDataType() == DataType.BOOLEAN && values8.size() > 1) {
            result10 = ColumnPredicate.isNotNull(boolCol);
        } else {
            result10 = ColumnPredicate.buildInList(boolCol, values8);
        }
        Assert.assertEquals(result10.toString(), "`bool` = false");
        ColumnPredicate<Boolean> result9;
        final SortedSet<DataTypeValue<Boolean>> values7 = toDataTypeValue(false, true, true);
        if (values7.isEmpty()) {
            result9 = ColumnPredicate.none(boolCol);
        } else if (boolCol.getDataType() == DataType.BOOLEAN && values7.size() > 1) {
            result9 = ColumnPredicate.isNotNull(boolCol);
        } else {
            result9 = ColumnPredicate.buildInList(boolCol, values7);
        }
        Assert.assertEquals(result9.toString(), "`bool` IS NOT NULL");
        ColumnPredicate<Byte> result8;
        final SortedSet<DataTypeValue<Byte>> values6 = toDataTypeValue((byte) 1, (byte) 10, (byte) 100);
        if (values6.isEmpty()) {
            result8 = ColumnPredicate.none(byteCol);
        } else if (byteCol.getDataType() == DataType.BOOLEAN && values6.size() > 1) {
            result8 = ColumnPredicate.isNotNull(byteCol);
        } else {
            result8 = ColumnPredicate.buildInList(byteCol, values6);
        }
        Assert.assertEquals(result8.toString(), "`byte` IN (1, 10, 100)");
        ColumnPredicate<Short> result7;
        final SortedSet<DataTypeValue<Short>> values5 = toDataTypeValue((short) 1, (short) 100, (short) 10);
        if (values5.isEmpty()) {
            result7 = ColumnPredicate.none(shortCol);
        } else if (shortCol.getDataType() == DataType.BOOLEAN && values5.size() > 1) {
            result7 = ColumnPredicate.isNotNull(shortCol);
        } else {
            result7 = ColumnPredicate.buildInList(shortCol, values5);
        }
        Assert.assertEquals(result7.toString(), "`short` IN (1, 10, 100)");
        ColumnPredicate<Integer> result6;
        final SortedSet<DataTypeValue<Integer>> values4 = toDataTypeValue(1, 100, 10);
        if (values4.isEmpty()) {
            result6 = ColumnPredicate.none(intCol);
        } else if (intCol.getDataType() == DataType.BOOLEAN && values4.size() > 1) {
            result6 = ColumnPredicate.isNotNull(intCol);
        } else {
            result6 = ColumnPredicate.buildInList(intCol, values4);
        }
        Assert.assertEquals(result6.toString(), "`int` IN (1, 10, 100)");
        ColumnPredicate<Long> result5;
        final SortedSet<DataTypeValue<Long>> values3 = toDataTypeValue(1L, 100L, 10L);
        if (values3.isEmpty()) {
            result5 = ColumnPredicate.none(longCol);
        } else if (longCol.getDataType() == DataType.BOOLEAN && values3.size() > 1) {
            result5 = ColumnPredicate.isNotNull(longCol);
        } else {
            result5 = ColumnPredicate.buildInList(longCol, values3);
        }
        Assert.assertEquals(result5.toString(), "`long` IN (1, 10, 100)");
        ColumnPredicate<Float> result4;
        final SortedSet<DataTypeValue<Float>> values2 = toDataTypeValue(123.456f, 78.9f);
        if (values2.isEmpty()) {
            result4 = ColumnPredicate.none(floatCol);
        } else if (floatCol.getDataType() == DataType.BOOLEAN && values2.size() > 1) {
            result4 = ColumnPredicate.isNotNull(floatCol);
        } else {
            result4 = ColumnPredicate.buildInList(floatCol, values2);
        }
        Assert.assertEquals(result4.toString(), "`float` IN (78.9, 123.456)");
        ColumnPredicate<Double> result3;
        final SortedSet<DataTypeValue<Double>> values1 = toDataTypeValue(123.456d, 78.9d);
        if (values1.isEmpty()) {
            result3 = ColumnPredicate.none(doubleCol);
        } else if (doubleCol.getDataType() == DataType.BOOLEAN && values1.size() > 1) {
            result3 = ColumnPredicate.isNotNull(doubleCol);
        } else {
            result3 = ColumnPredicate.buildInList(doubleCol, values1);
        }
        Assert.assertEquals(result3.toString(), "`double` IN (78.9, 123.456)");
        ColumnPredicate<String> result2;
        final SortedSet<DataTypeValue<String>> values = toDataTypeValue("my string", "a");
        if (values.isEmpty()) {
            result2 = ColumnPredicate.none(stringCol);
        } else if (stringCol.getDataType() == DataType.BOOLEAN && values.size() > 1) {
            result2 = ColumnPredicate.isNotNull(stringCol);
        } else {
            result2 = ColumnPredicate.buildInList(stringCol, values);
        }
        Assert.assertEquals(result2.toString(),
                "`string` IN (\"a\", \"my string\")");

        ByteBuffer firstBuffer = ByteBuffer.wrap(new byte[]{(byte) 0xAB, (byte) 0x01, (byte) 0xCD});
        ByteBuffer secondBuffer = ByteBuffer.wrap(new byte[]{(byte) 0x00});
        SortedSet<DataTypeValue<ByteBuffer>> setOfStuff = toDataTypeValue(firstBuffer, secondBuffer);
        Assert.assertEquals(setOfStuff.first().value,secondBuffer);
        Assert.assertEquals(setOfStuff.last().value,firstBuffer);
        ColumnPredicate<ByteBuffer> result1;
        if (setOfStuff.isEmpty()) {
            result1 = ColumnPredicate.none(binaryCol);
        } else if (binaryCol.getDataType() == DataType.BOOLEAN && setOfStuff.size() > 1) {
            result1 = ColumnPredicate.isNotNull(binaryCol);
        } else {
            result1 = ColumnPredicate.buildInList(binaryCol, setOfStuff);
        }
        Assert.assertEquals(result1.toString(), "`binary` IN (0x00, 0xAB01CD)");


        Assert.assertEquals(ColumnPredicate.isNull(dateCol).toString(), "`date` IS NULL");
        Assert.assertEquals(ColumnPredicate.isNotNull(dateCol).toString(),
                "`date` IS NOT NULL");

        Assert.assertEquals(newComparisonPredicate(dateCol, EQUAL, LocalDate.of(2020, 6, 16))
                        .toString(),
                "`date` = 2020-06-16");
        SortedSet<DataTypeValue<ChronoLocalDate>> dataTypeValues = toDataTypeValue(
                LocalDate.of(2020,6,16),
                LocalDate.of(2019,1,1),
                LocalDate.of(2020,11,10));
        ColumnPredicate<ChronoLocalDate> result;
        if (dataTypeValues.isEmpty()) {
            result = ColumnPredicate.none(dateCol);
        } else if (dateCol.getDataType() == DataType.BOOLEAN && dataTypeValues.size() > 1) {
            result = ColumnPredicate.isNotNull(dateCol);
        } else {
            result = ColumnPredicate.buildInList(dateCol, dataTypeValues);
        }
        Assert.assertEquals(result.toString(),
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