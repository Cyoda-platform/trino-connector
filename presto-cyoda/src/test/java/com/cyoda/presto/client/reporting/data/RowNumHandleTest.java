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

package com.cyoda.presto.client.reporting.data;

import com.cyoda.presto.client.logic.ColumnPredicate;
import com.cyoda.presto.client.types.DataTypeValue;
import com.cyoda.presto.handles.CyodaColumnHandle;
import org.springframework.hateoas.PagedModel;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class RowNumHandleTest {

    @Test
    public void testEmptyRange() {
        long page = 0;
        int size = 10;

        CyodaColumnHandle column = mock(CyodaColumnHandle.class);
        when(column.getColumnName()).thenReturn("happyColumnName");
        ColumnPredicate<Long> columnPredicate = new ColumnPredicate<>(ColumnPredicate.PredicateType.ALL, column, null, null);

        RowNumHandle rowNumHandle = RowNumHandle.from(columnPredicate,page,size);
        assertFalse(rowNumHandle.hasRowNumEquals);
        assertFalse(rowNumHandle.hasRowNumRange);
        assertEquals(rowNumHandle.minRowNum,1);
        assertEquals(rowNumHandle.maxRowNum,Long.MAX_VALUE);
        assertEquals(rowNumHandle.size,size);
        assertEquals(rowNumHandle.page,0);
        assertEquals(rowNumHandle.offset,0);

        assertTrue(rowNumHandle.isInRowWindow(1));
        assertTrue(rowNumHandle.isInRowWindow(0));
        assertTrue(rowNumHandle.isInRowWindow(2));

        PagedModel<?> item = mock(PagedModel.class);
        PagedModel.PageMetadata mockMeta = mock(PagedModel.PageMetadata.class);
        when(item.getMetadata()).thenReturn(mockMeta);
        PagedModel.PageMetadata apiMeta = mock(PagedModel.PageMetadata.class);
        PagedModel.PageMetadata pageMeta = rowNumHandle.createPageMeta(1, 1, item, apiMeta);
        assertEquals(pageMeta,mockMeta);

        // Always need meta from API
        when(item.getMetadata()).thenReturn(null);
        assertThrows(IllegalArgumentException.class,()->rowNumHandle.createPageMeta(1, 1, item, apiMeta));
    }

    @Test
    public void testSingleValueEquals() {
        long rowNum = 10L;
        long page = 0;
        int size = 1000;

        CyodaColumnHandle column = mock(CyodaColumnHandle.class);
        when(column.getColumnName()).thenReturn("happyColumnName");
        ColumnPredicate<Long> columnPredicate = new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY,
                column, DataTypeValue.of(rowNum), null);

        RowNumHandle rowNumHandle = RowNumHandle.from(columnPredicate,page,size);
        assertTrue(rowNumHandle.hasRowNumEquals);
        assertFalse(rowNumHandle.hasRowNumRange);
        assertEquals(rowNumHandle.minRowNum,rowNum);
        assertEquals(rowNumHandle.maxRowNum,Long.MAX_VALUE);
        assertEquals(rowNumHandle.size,1);
        assertEquals(rowNumHandle.page,rowNum-1);
        assertEquals(rowNumHandle.offset,rowNum-1);

        assertFalse(rowNumHandle.isInRowWindow(1));
        assertTrue(rowNumHandle.isInRowWindow(10));
        assertFalse(rowNumHandle.isInRowWindow(11));

        PagedModel<?> item = mock(PagedModel.class);
        PagedModel.PageMetadata mockMeta = mock(PagedModel.PageMetadata.class);
        when(item.getMetadata()).thenReturn(null);
        PagedModel.PageMetadata apiMeta = mock(PagedModel.PageMetadata.class);
        PagedModel.PageMetadata pageMeta = rowNumHandle.createPageMeta(0, 1000, item, apiMeta);
        assertEquals(pageMeta.getTotalPages(),1);
        assertEquals(pageMeta.getTotalElements(),1);
        assertEquals(pageMeta.getSize(),1000);
        assertEquals(pageMeta.getNumber(),0);
    }

    @Test
    public void testRangeBiggerThanPageSize() {
        long lower = 10L;
        long upper = 200L;
        long page = 0;
        int size = 100;

        CyodaColumnHandle column = mock(CyodaColumnHandle.class);
        when(column.getColumnName()).thenReturn("happyColumnName");
        ColumnPredicate<Long> columnPredicate = new ColumnPredicate<>(ColumnPredicate.PredicateType.RANGE,
                column, DataTypeValue.of(lower), DataTypeValue.of(upper));

        RowNumHandle rowNumHandle = RowNumHandle.from(columnPredicate,page,size);
        assertFalse(rowNumHandle.hasRowNumEquals);
        assertTrue(rowNumHandle.hasRowNumRange);
        assertEquals(rowNumHandle.minRowNum,lower);
        assertEquals(rowNumHandle.maxRowNum,upper);
        assertEquals(rowNumHandle.size,size);
        assertEquals(rowNumHandle.page,0);
        assertEquals(rowNumHandle.offset,lower-1);

        assertFalse(rowNumHandle.isInRowWindow(1));
        assertTrue(rowNumHandle.isInRowWindow(lower));
        assertTrue(rowNumHandle.isInRowWindow(lower+1));
        assertTrue(rowNumHandle.isInRowWindow(upper-1));
        assertFalse(rowNumHandle.isInRowWindow(upper)); // When hitting the page limit, it's upper-1
        assertFalse(rowNumHandle.isInRowWindow(upper+1));


        PagedModel<?> item = mock(PagedModel.class);
        when(item.getMetadata()).thenReturn(null);
        PagedModel.PageMetadata apiMeta = mock(PagedModel.PageMetadata.class);
        long totalElementsInSearch = 100_100;
        when(apiMeta.getTotalElements()).thenReturn(totalElementsInSearch);
        PagedModel.PageMetadata pageMeta = rowNumHandle.createPageMeta(0, 1000, item, apiMeta);
        assertEquals(pageMeta.getTotalPages(),1);
        assertEquals(pageMeta.getTotalElements(),upper-lower); // Since the rowNum is a range predicate, search cannot return more than the window
        assertEquals(pageMeta.getSize(),size);
        assertEquals(pageMeta.getNumber(),0);
    }
}