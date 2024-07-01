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

package com.cyoda.connector.client.reporting;

import com.cyoda.connector.client.logic.ColumnPredicate;
import com.cyoda.connector.client.logic.CompoundPredicateNode;
import com.cyoda.connector.client.logic.converters.impl.StringPrestoValueConverter;
import com.cyoda.connector.client.types.DataType;
import com.cyoda.connector.handles.CyodaColumnHandle;
import io.trino.spi.type.VarcharType;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import static com.cyoda.connector.client.logic.CompoundPredicateNode.builder;
import static com.cyoda.connector.client.logic.Connective.AND;
import static com.cyoda.connector.client.logic.Connective.OR;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

public class ColumnPredicateTraversalTest {

    private CyodaColumnHandle myHandle = new CyodaColumnHandle("myColumnName",
            VarcharType.VARCHAR, DataType.STRING, 0);
    private CyodaColumnHandle theOtherHandle = new CyodaColumnHandle("theOtherColumnName",
            VarcharType.VARCHAR, DataType.STRING, 1);
    private static StringPrestoValueConverter stringConverter = new StringPrestoValueConverter();

    @Test
    public void testOf() {

        assertNotNull(PredicateTraversal.of(CompoundPredicateNode.empty(),null));

        try {
            //noinspection ConstantConditions
            assertNotNull(PredicateTraversal.of(null,null));
            fail("should not get here");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage(),"Need to put a message in the Exception");
            assertFalse(e.getMessage().isEmpty(),"Need to put something!");
        }

    }

    @Test
    public void testAssembleFilterings_no_values() {
        
        Optional<SortedSet<String>> selectionSet = PredicateTraversal.of(CompoundPredicateNode.empty(),String.class)
                .assembleEqualsPredicateValuesFromAnd(myHandle);
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),0);
    }

    private static ColumnPredicate<String> newEqualsPredicate(CyodaColumnHandle column, Slice slice){
        return stringConverter.newComparisonPredicateFromNative(column, ColumnPredicate.ComparisonOp.EQUAL, slice);
    } 
    @Test
    public void testAssembleFilterings_one_equals_value() {

        ColumnPredicate<String> hello = newEqualsPredicate(myHandle, Slices.utf8Slice("hello"));
        Optional<SortedSet<String>> selectionSet = PredicateTraversal.of(
                builder(AND).addLeaf(hello).build(),
                String.class
        ).assembleEqualsPredicateValuesFromAnd(myHandle);
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),1);
        String value = selectionSet.get().iterator().next();
        assertEquals(value,"hello");
    }


    @Test
    public void testAssembleFilterings_list_value() {

        ColumnPredicate<String> hello = newEqualsPredicate(myHandle, Slices.utf8Slice("hello"));
        Optional<SortedSet<String>> selectionSet = PredicateTraversal.of(
                builder(AND).addLeaf(hello).build(),
                String.class
        ).assembleEqualsPredicateValuesFromAnd(myHandle);
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),1);
        String value = selectionSet.get().iterator().next();
        assertEquals(value,"hello");
    }

    @Test
    public void testAssembleFilterings_two_different_values() {

        ColumnPredicate<String> hello = newEqualsPredicate(myHandle, Slices.utf8Slice("hello"));
        ColumnPredicate<String> goodbye = newEqualsPredicate(myHandle, Slices.utf8Slice("goodbye"));
        Optional<SortedSet<String>> selectionSet = PredicateTraversal.of(builder(AND)
                        .addMember(builder(AND).addLeaf(hello).build())
                        .addMember(builder(AND).addLeaf(goodbye).build())
                        .build(),
                String.class
        ).assembleEqualsPredicateValuesFromAnd(myHandle);
        // Cannot select something that is "hello" AND "goodbye"
        assertFalse(selectionSet.isPresent());
    }

    @Test
    public void testAssembleFilterings_two_same_values() {

        ColumnPredicate<String> hello = newEqualsPredicate(myHandle, Slices.utf8Slice("hello"));
        ColumnPredicate<String> sameAsHello = newEqualsPredicate(myHandle, Slices.utf8Slice("hello"));
        CompoundPredicateNode node = builder(AND)
                .addMember(builder(AND).addLeaf(hello).build())
                .addMember(builder(AND).addLeaf(sameAsHello).build())
                .build();
        int nodeCount = node.countNodes();
        assertEquals(nodeCount,2);
        Optional<SortedSet<String>> selectionSet = PredicateTraversal.of(node,
                String.class
        ).assembleEqualsPredicateValuesFromAnd(myHandle);
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),1);
        String value = selectionSet.get().iterator().next();
        assertEquals(value,"hello");
    }

    @Test
    public void testAssembleFilterings_not_my_column() {

        ColumnPredicate<String> hello = newEqualsPredicate(theOtherHandle, Slices.utf8Slice("hello"));
        ColumnPredicate<String> sameAsHello = newEqualsPredicate(theOtherHandle, Slices.utf8Slice("hello"));
        CompoundPredicateNode node = builder(AND)
                .addMember(builder(AND).addLeaf(hello).build())
                .addMember(builder(AND).addLeaf(sameAsHello).build())
                .build();
        int nodeCount = node.countNodes();
        assertEquals(nodeCount,2);
        Optional<SortedSet<String>> selectionSet = PredicateTraversal.of(node,
                String.class
        ).assembleEqualsPredicateValuesFromAnd(myHandle);
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),0);
    }

    @Test
    public void testAssembleFilterings_complex_tree_with_and_at_top() {

        ColumnPredicate<String> myHello = newEqualsPredicate(myHandle, Slices.utf8Slice("hello"));
        ColumnPredicate<String> myGoodbye = newEqualsPredicate(myHandle, Slices.utf8Slice("goodBye"));

        ColumnPredicate<String> notMyHello = newEqualsPredicate(theOtherHandle, Slices.utf8Slice("hello"));
        ColumnPredicate<String> notMyGoodbye = newEqualsPredicate(theOtherHandle, Slices.utf8Slice("goodBye"));
        CompoundPredicateNode node = builder(AND)
                .addMember(builder(AND).addLeaf(notMyHello).build())
                .addMember(builder(AND).addLeaf(notMyGoodbye).build())
                .addMember(builder(AND)
                        .addMember(CompoundPredicateNode.empty(null))
                        .addMember(builder(OR)
                                .addLeaf(myHello)
                                .addLeaf(myGoodbye)
                                .build())
                        .build()
                ).build();
        int nodeCount = node.countNodes();
        assertEquals(nodeCount,4);
        assertThrows(IllegalArgumentException.class,()->PredicateTraversal.of(node,String.class).assembleEqualsPredicateValuesFromAnd(myHandle));
        Set<ColumnPredicate<String>> columnPredicates = PredicateTraversal.of(node, String.class).parseFor(myHandle);
        assertFalse(columnPredicates.isEmpty());
        assertEquals(columnPredicates.size(),2);
    }

    @Test
    public void testAssembleFilterings_complex_and_tree() {

        ColumnPredicate<String> myHello = newEqualsPredicate(myHandle, Slices.utf8Slice("hello"));

        ColumnPredicate<String> notMyGoodbye = newEqualsPredicate(theOtherHandle, Slices.utf8Slice("goodBye"));
        CompoundPredicateNode node = builder(AND)
                .addMember(builder(AND).addLeaf(myHello).build())
                .addMember(builder(AND).addLeaf(notMyGoodbye).build())
                .addMember(builder(AND)
                        .addMember(CompoundPredicateNode.empty(null))
                        .addMember(builder(AND).addLeaf(myHello).build())
                        .build()
                ).build();
        int nodeCount = node.countNodes();
        assertEquals(nodeCount,3);
        Optional<SortedSet<String>> selectionSet = PredicateTraversal.of(node, String.class)
                .assembleEqualsPredicateValuesFromAnd(myHandle);
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),1);
    }

    @Test
    public void testAssembleFilterings_complex_and_tree_mutually_exclusive() {

        ColumnPredicate<String> myHello = newEqualsPredicate(myHandle, Slices.utf8Slice("hello"));
        ColumnPredicate<String> myGoodbye = newEqualsPredicate(myHandle, Slices.utf8Slice("goodBye"));

        ColumnPredicate<String> notMyGoodbye = newEqualsPredicate(theOtherHandle, Slices.utf8Slice("goodBye"));
        CompoundPredicateNode node = builder(AND)
                .addMember(builder(AND).addLeaf(myHello).build())
                .addMember(builder(AND).addLeaf(notMyGoodbye).build())
                .addMember(builder(AND)
                        .addMember(CompoundPredicateNode.empty(null))
                        .addMember(builder(AND)
                                .addLeaf(myGoodbye)
                                .build())
                        .build()
                ).build();
        int nodeCount = node.countNodes();
        assertEquals(nodeCount,3);
        Optional<SortedSet<String>> selectionSet = PredicateTraversal.of(node, String.class)
                .assembleEqualsPredicateValuesFromAnd(myHandle);
        assertFalse(selectionSet.isPresent());
    }

    @Test
    public void testAssembleFilterings_complex_or_tree() {

        ColumnPredicate<String> myHello = newEqualsPredicate(myHandle, Slices.utf8Slice("hello"));

        ColumnPredicate<String> notMyGoodbye = newEqualsPredicate(theOtherHandle, Slices.utf8Slice("goodBye"));
        CompoundPredicateNode node = builder(AND)
                .addMember(builder(AND).addLeaf(myHello).build())
                .addMember(builder(AND).addLeaf(notMyGoodbye).build())
                .addMember(builder(OR)
                        .addMember(CompoundPredicateNode.empty(null))
                        .addMember(builder(OR)
                                .addLeaf(myHello)
                                .build())
                        .build()
                ).build();
        int nodeCount = node.countNodes();
        assertEquals(nodeCount,3);
        Optional<SortedSet<String>> selectionSet = PredicateTraversal.of(node,String.class).assembleEqualsPredicateValuesFromAnd(myHandle);
        assertTrue(selectionSet.isPresent());
        assertEquals(selectionSet.get().size(),1);
    }

    @Test
    public void testAssembleFilterings_complex_tree_top_or() {

        ColumnPredicate<String> myHello = newEqualsPredicate(myHandle, Slices.utf8Slice("hello"));
        ColumnPredicate<String> myGoodbye = newEqualsPredicate(myHandle, Slices.utf8Slice("goodBye"));

        ColumnPredicate<String> notMyHello = newEqualsPredicate(theOtherHandle, Slices.utf8Slice("hello"));
        ColumnPredicate<String> notMyGoodbye = newEqualsPredicate(theOtherHandle, Slices.utf8Slice("goodBye"));
        CompoundPredicateNode node = builder(AND)
                .addMember(builder(AND).addLeaf(notMyHello).build())
                .addMember(builder(AND).addLeaf(notMyGoodbye).build())
                .addMember(builder(OR)
                        .addMember(CompoundPredicateNode.empty(null))
                        .addMember(builder(OR)
                                .addLeaf(myHello)
                                .addLeaf(myGoodbye)
                                .build())
                        .build()
                ).build();
        int nodeCount = node.countNodes();
        assertEquals(nodeCount,4);
        assertThrows(IllegalArgumentException.class,()->PredicateTraversal.of(node,String.class).assembleEqualsPredicateValuesFromAnd(myHandle));
        Set<ColumnPredicate<String>> columnPredicates = PredicateTraversal.of(node, String.class).parseFor(myHandle);
        assertFalse(columnPredicates.isEmpty());
        assertEquals(columnPredicates.size(),2);
    }
}