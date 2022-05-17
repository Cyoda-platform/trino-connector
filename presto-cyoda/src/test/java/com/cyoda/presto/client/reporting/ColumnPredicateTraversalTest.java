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

package com.cyoda.presto.client.reporting;

import com.cyoda.presto.client.logic.ColumnPredicate;
import com.cyoda.presto.client.logic.ColumnPredicateUtils;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.common.type.VarcharType;
import io.airlift.slice.Slices;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.Set;

import static com.cyoda.presto.client.logic.CompoundPredicateNode.builder;
import static com.cyoda.presto.client.logic.Connective.AND;
import static com.cyoda.presto.client.logic.Connective.OR;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class ColumnPredicateTraversalTest {

    @Test
    public void testOf() {

        assertNotNull(PredicateTraversal.of(CompoundPredicateNode.empty()));

        try {
            //noinspection ConstantConditions
            assertNotNull(PredicateTraversal.of(null));
            fail("should not get here");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage(),"Need to put a message in the Exception");
            assertFalse(e.getMessage().isEmpty(),"Need to put something!");
        }

    }

    @Test
    public void testAssembleFilterings_no_values() {

        CyodaColumnHandle mockHandle1 = mock(CyodaColumnHandle.class);
        when(mockHandle1.getColumnName()).thenReturn("myColumnName");
        when(mockHandle1.getDataType()).thenReturn(DataType.STRING);
        when(mockHandle1.getColumnType()).thenReturn(VarcharType.VARCHAR);

        Optional<Set<String>> selectionSet = PredicateTraversal.of(CompoundPredicateNode.empty()).assembleFilterings(mockHandle1.getColumnName());
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),0);
    }

    @Test
    public void testAssembleFilterings_one_equals_value() {

        CyodaColumnHandle mockHandle1 = mock(CyodaColumnHandle.class);
        when(mockHandle1.getColumnName()).thenReturn("myColumnName");
        when(mockHandle1.getDataType()).thenReturn(DataType.STRING);
        when(mockHandle1.getColumnType()).thenReturn(VarcharType.VARCHAR);

        ColumnPredicate<String> hello = ColumnPredicateUtils.newEqualsPredicate(mockHandle1, Slices.utf8Slice("hello"),String.class);
        Optional<Set<String>> selectionSet = PredicateTraversal.of(
                builder(AND).addLeaf(hello).build()
        ).assembleFilterings(mockHandle1.getColumnName());
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),1);
        String value = selectionSet.get().iterator().next();
        assertEquals(value,"hello");
    }


    @Test
    public void testAssembleFilterings_list_value() {

        CyodaColumnHandle mockHandle1 = mock(CyodaColumnHandle.class);
        when(mockHandle1.getColumnName()).thenReturn("myColumnName");
        when(mockHandle1.getDataType()).thenReturn(DataType.STRING);
        when(mockHandle1.getColumnType()).thenReturn(VarcharType.VARCHAR);

        ColumnPredicate<String> hello = ColumnPredicateUtils.newEqualsPredicate(mockHandle1, Slices.utf8Slice("hello"),String.class);
        Optional<Set<String>> selectionSet = PredicateTraversal.of(
                builder(AND).addLeaf(hello).build()
        ).assembleFilterings(mockHandle1.getColumnName());
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),1);
        String value = selectionSet.get().iterator().next();
        assertEquals(value,"hello");
    }

    @Test
    public void testAssembleFilterings_two_different_values() {

        CyodaColumnHandle mockHandle1 = mock(CyodaColumnHandle.class);
        when(mockHandle1.getColumnName()).thenReturn("myColumnName");
        when(mockHandle1.getDataType()).thenReturn(DataType.STRING);
        when(mockHandle1.getColumnType()).thenReturn(VarcharType.VARCHAR);

        ColumnPredicate<String> hello = ColumnPredicateUtils.newEqualsPredicate(mockHandle1, Slices.utf8Slice("hello"),String.class);
        ColumnPredicate<String> goodbye = ColumnPredicateUtils.newEqualsPredicate(mockHandle1, Slices.utf8Slice("goodbye"),String.class);
        Optional<Set<String>> selectionSet = PredicateTraversal.of(builder(AND)
                        .addMember(builder(AND).addLeaf(hello).build())
                        .addMember(builder(AND).addLeaf(goodbye).build())
                        .build()
        ).assembleFilterings(mockHandle1.getColumnName());
        // Cannot select something that is "hello" AND "goodbye"
        assertFalse(selectionSet.isPresent());
    }

    @Test
    public void testAssembleFilterings_two_same_values() {

        CyodaColumnHandle mockHandle1 = mock(CyodaColumnHandle.class);
        when(mockHandle1.getColumnName()).thenReturn("myColumnName");
        when(mockHandle1.getDataType()).thenReturn(DataType.STRING);
        when(mockHandle1.getColumnType()).thenReturn(VarcharType.VARCHAR);

        ColumnPredicate<String> hello = ColumnPredicateUtils.newEqualsPredicate(mockHandle1, Slices.utf8Slice("hello"),String.class);
        ColumnPredicate<String> sameAsHello = ColumnPredicateUtils.newEqualsPredicate(mockHandle1, Slices.utf8Slice("hello"),String.class);
        Optional<Set<String>> selectionSet = PredicateTraversal.of(builder(AND)
                        .addMember(builder(AND).addLeaf(hello).build())
                        .addMember(builder(AND).addLeaf(sameAsHello).build())
                        .build()
        ).assembleFilterings(mockHandle1.getColumnName());
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),1);
        String value = selectionSet.get().iterator().next();
        assertEquals(value,"hello");
    }

    @Test
    public void testAssembleFilterings_not_my_column() {

        CyodaColumnHandle myMockHandle = mock(CyodaColumnHandle.class);
        when(myMockHandle.getColumnName()).thenReturn("myColumnName");
        when(myMockHandle.getDataType()).thenReturn(DataType.STRING);
        when(myMockHandle.getColumnType()).thenReturn(VarcharType.VARCHAR);

        CyodaColumnHandle theOtherMockHandle = mock(CyodaColumnHandle.class);
        when(theOtherMockHandle.getColumnName()).thenReturn("theOtherColumnName");
        when(theOtherMockHandle.getDataType()).thenReturn(DataType.STRING);
        when(theOtherMockHandle.getColumnType()).thenReturn(VarcharType.VARCHAR);

        ColumnPredicate<String> hello = ColumnPredicateUtils.newEqualsPredicate(theOtherMockHandle, Slices.utf8Slice("hello"),String.class);
        ColumnPredicate<String> sameAsHello = ColumnPredicateUtils.newEqualsPredicate(theOtherMockHandle, Slices.utf8Slice("hello"),String.class);
        Optional<Set<String>> selectionSet = PredicateTraversal.of(builder(AND)
                .addMember(builder(AND).addLeaf(hello).build())
                .addMember(builder(AND).addLeaf(sameAsHello).build())
                .build()
        ).assembleFilterings(myMockHandle.getColumnName());
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),0);
    }

    @Test
    public void testAssembleFilterings_complex_tree_with_and_at_top() {

        CyodaColumnHandle myMockHandle = mock(CyodaColumnHandle.class);
        when(myMockHandle.getColumnName()).thenReturn("myColumnName");
        when(myMockHandle.getDataType()).thenReturn(DataType.STRING);
        when(myMockHandle.getColumnType()).thenReturn(VarcharType.VARCHAR);

        CyodaColumnHandle theOtherMockHandle = mock(CyodaColumnHandle.class);
        when(theOtherMockHandle.getColumnName()).thenReturn("theOtherColumnName");
        when(theOtherMockHandle.getDataType()).thenReturn(DataType.STRING);
        when(theOtherMockHandle.getColumnType()).thenReturn(VarcharType.VARCHAR);

        ColumnPredicate<String> myHello = ColumnPredicateUtils.newEqualsPredicate(myMockHandle, Slices.utf8Slice("hello"),String.class);
        ColumnPredicate<String> myGoodbye = ColumnPredicateUtils.newEqualsPredicate(myMockHandle, Slices.utf8Slice("goodBye"),String.class);

        ColumnPredicate<String> notMyHello = ColumnPredicateUtils.newEqualsPredicate(theOtherMockHandle, Slices.utf8Slice("hello"),String.class);
        ColumnPredicate<String> notMyGoodbye = ColumnPredicateUtils.newEqualsPredicate(theOtherMockHandle, Slices.utf8Slice("goodBye"),String.class);
        Optional<Set<String>> selectionSet = PredicateTraversal.of(builder(AND)
                        .addMember(builder(AND).addLeaf(notMyHello).build())
                                .addMember(builder(AND).addLeaf(notMyGoodbye).build())
                                .addMember(builder(AND)
                                        .addMember(CompoundPredicateNode.empty(null))
                                        .addMember(builder(OR)
                                                        .addLeaf(myHello)
                                                        .addLeaf(myGoodbye)
                                                        .build())
                                        .build()
                        ).build()
        ).assembleFilterings(myMockHandle.getColumnName());
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),2);
    }

    @Test
    public void testAssembleFilterings_complex_and_tree() {

        CyodaColumnHandle myMockHandle = mock(CyodaColumnHandle.class);
        when(myMockHandle.getColumnName()).thenReturn("myColumnName");
        when(myMockHandle.getDataType()).thenReturn(DataType.STRING);
        when(myMockHandle.getColumnType()).thenReturn(VarcharType.VARCHAR);

        CyodaColumnHandle theOtherMockHandle = mock(CyodaColumnHandle.class);
        when(theOtherMockHandle.getColumnName()).thenReturn("theOtherColumnName");
        when(theOtherMockHandle.getDataType()).thenReturn(DataType.STRING);
        when(theOtherMockHandle.getColumnType()).thenReturn(VarcharType.VARCHAR);

        ColumnPredicate<String> myHello = ColumnPredicateUtils.newEqualsPredicate(myMockHandle, Slices.utf8Slice("hello"),String.class);

        ColumnPredicate<String> notMyGoodbye = ColumnPredicateUtils.newEqualsPredicate(theOtherMockHandle, Slices.utf8Slice("goodBye"),String.class);
        Optional<Set<String>> selectionSet = PredicateTraversal.of(builder(AND)
                        .addMember(builder(AND).addLeaf(myHello).build())
                                .addMember(builder(AND).addLeaf(notMyGoodbye).build())
                                .addMember(builder(AND)
                                        .addMember(CompoundPredicateNode.empty(null))
                                        .addMember(builder(AND).addLeaf(myHello).build())
                                        .build()
                        ).build()
        ).assembleFilterings(myMockHandle.getColumnName());
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),1);
    }

    @Test
    public void testAssembleFilterings_complex_and_tree_mutually_exclusive() {

        CyodaColumnHandle myMockHandle = mock(CyodaColumnHandle.class);
        when(myMockHandle.getColumnName()).thenReturn("myColumnName");
        when(myMockHandle.getDataType()).thenReturn(DataType.STRING);
        when(myMockHandle.getColumnType()).thenReturn(VarcharType.VARCHAR);

        CyodaColumnHandle theOtherMockHandle = mock(CyodaColumnHandle.class);
        when(theOtherMockHandle.getColumnName()).thenReturn("theOtherColumnName");
        when(theOtherMockHandle.getDataType()).thenReturn(DataType.STRING);
        when(theOtherMockHandle.getColumnType()).thenReturn(VarcharType.VARCHAR);

        ColumnPredicate<String> myHello = ColumnPredicateUtils.newEqualsPredicate(myMockHandle, Slices.utf8Slice("hello"),String.class);
        ColumnPredicate<String> myGoodbye = ColumnPredicateUtils.newEqualsPredicate(myMockHandle, Slices.utf8Slice("goodBye"),String.class);

        ColumnPredicate<String> notMyGoodbye = ColumnPredicateUtils.newEqualsPredicate(theOtherMockHandle, Slices.utf8Slice("goodBye"),String.class);
        Optional<Set<String>> selectionSet = PredicateTraversal.of(builder(AND)
                        .addMember(builder(AND).addLeaf(myHello).build())
                        .addMember(builder(AND).addLeaf(notMyGoodbye).build())
                        .addMember(builder(AND)
                                        .addMember(CompoundPredicateNode.empty(null))
                                        .addMember(builder(AND)
                                                .addLeaf(myGoodbye)
                                                .build())
                                        .build()
                        ).build()
        ).assembleFilterings(myMockHandle.getColumnName());
        assertFalse(selectionSet.isPresent());
    }

    @Test
    public void testAssembleFilterings_complex_or_tree() {

        CyodaColumnHandle myMockHandle = mock(CyodaColumnHandle.class);
        when(myMockHandle.getColumnName()).thenReturn("myColumnName");
        when(myMockHandle.getDataType()).thenReturn(DataType.STRING);
        when(myMockHandle.getColumnType()).thenReturn(VarcharType.VARCHAR);

        CyodaColumnHandle theOtherMockHandle = mock(CyodaColumnHandle.class);
        when(theOtherMockHandle.getColumnName()).thenReturn("theOtherColumnName");
        when(theOtherMockHandle.getDataType()).thenReturn(DataType.STRING);
        when(theOtherMockHandle.getColumnType()).thenReturn(VarcharType.VARCHAR);

        ColumnPredicate<String> myHello = ColumnPredicateUtils.newEqualsPredicate(myMockHandle, Slices.utf8Slice("hello"),String.class);

        ColumnPredicate<String> notMyGoodbye = ColumnPredicateUtils.newEqualsPredicate(theOtherMockHandle, Slices.utf8Slice("goodBye"),String.class);
        Optional<Set<String>> selectionSet = PredicateTraversal.of(builder(AND)
                        .addMember(builder(AND).addLeaf(myHello).build())
                        .addMember(builder(AND).addLeaf(notMyGoodbye).build())
                        .addMember(builder(OR)
                                        .addMember(CompoundPredicateNode.empty(null))
                                        .addMember(builder(OR)
                                                .addLeaf(myHello)
                                                .build())
                                        .build()
                        ).build()
        ).assembleFilterings(myMockHandle.getColumnName());
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),1);
    }

    @Test
    public void testAssembleFilterings_complex_tree_top_or() {

        CyodaColumnHandle myMockHandle = mock(CyodaColumnHandle.class);
        when(myMockHandle.getColumnName()).thenReturn("myColumnName");
        when(myMockHandle.getDataType()).thenReturn(DataType.STRING);
        when(myMockHandle.getColumnType()).thenReturn(VarcharType.VARCHAR);

        CyodaColumnHandle theOtherMockHandle = mock(CyodaColumnHandle.class);
        when(theOtherMockHandle.getColumnName()).thenReturn("theOtherColumnName");
        when(theOtherMockHandle.getDataType()).thenReturn(DataType.STRING);
        when(theOtherMockHandle.getColumnType()).thenReturn(VarcharType.VARCHAR);

        ColumnPredicate<String> myHello = ColumnPredicateUtils.newEqualsPredicate(myMockHandle, Slices.utf8Slice("hello"),String.class);
        ColumnPredicate<String> myGoodbye = ColumnPredicateUtils.newEqualsPredicate(myMockHandle, Slices.utf8Slice("goodBye"),String.class);

        ColumnPredicate<String> notMyHello = ColumnPredicateUtils.newEqualsPredicate(theOtherMockHandle, Slices.utf8Slice("hello"),String.class);
        ColumnPredicate<String> notMyGoodbye = ColumnPredicateUtils.newEqualsPredicate(theOtherMockHandle, Slices.utf8Slice("goodBye"),String.class);
        Optional<Set<String>> selectionSet = PredicateTraversal.of(builder(AND)
                        .addMember(builder(AND).addLeaf(notMyHello).build())
                        .addMember(builder(AND).addLeaf(notMyGoodbye).build())
                        .addMember(builder(OR)
                                        .addMember(CompoundPredicateNode.empty(null))
                                        .addMember(builder(OR)
                                                .addLeaf(myHello)
                                                .addLeaf(myGoodbye)
                                                .build())
                                        .build()
                        ).build()
        ).assembleFilterings(myMockHandle.getColumnName());
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),2);
    }
}