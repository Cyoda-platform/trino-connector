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
import com.google.common.base.MoreObjects;
import org.springframework.hateoas.PagedModel;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RowNumHandle {

    public final boolean hasRowNumEquals;
    public final boolean hasRowNumRange;
    public final long minRowNum;
    public final long maxRowNum;
    public final long size;
    public final long page;
    public final long offset;

    private RowNumHandle(boolean hasRowNumEquals, boolean hasRowNumRange, long minRowNum, long maxRowNum, long size, long page, long offset) {
        this.hasRowNumEquals = hasRowNumEquals;
        this.hasRowNumRange = hasRowNumRange;
        this.minRowNum = minRowNum;
        this.maxRowNum = maxRowNum;
        this.size = size;
        this.page = page;
        this.offset = offset;
    }


    public static @Nonnull List<RowNumHandle> from(@Nonnull ColumnPredicate<Long> columnPredicate, long page, long size) {
        boolean isEqualsPredicate = columnPredicate.getType() == ColumnPredicate.PredicateType.EQUALITY;
        boolean isRangePredicate = columnPredicate.getType() == ColumnPredicate.PredicateType.RANGE;
        boolean isListPredicate = columnPredicate.getType() == ColumnPredicate.PredicateType.IN_LIST;
        if (isListPredicate) {
            return columnPredicate.getInListValues().stream().map(it -> {
                ColumnPredicate<Long> predicate = new ColumnPredicate<>(
                        ColumnPredicate.PredicateType.EQUALITY, columnPredicate.getColumnName(), it, null);
                return createRowNumHandle(predicate, page, size, true, false);
            }).collect(Collectors.toList());
        }
        return Collections.singletonList(createRowNumHandle(columnPredicate, page, size, isEqualsPredicate, isRangePredicate));
    }

    private static RowNumHandle createRowNumHandle(ColumnPredicate<Long> columnPredicate, long page, long size, boolean isEqualsPredicate, boolean isRangePredicate) {
        long minRownum = columnPredicate.getLower() != null ?
                Optional.ofNullable(columnPredicate.getLower().value)
                        .orElseThrow(()->new IllegalArgumentException("columnPredicate lower value is null")) : 1;
        long maxRownum = columnPredicate.getUpper() != null ? Optional.ofNullable(columnPredicate.getUpper().value)
                .orElseThrow(()->new IllegalArgumentException("columnPredicate lower value is null")) : Long.MAX_VALUE;

        long theSize = isEqualsPredicate ? 1 : Math.min(size,maxRownum-minRownum);
        long thePage;
        long offset;
        if (isEqualsPredicate) {
            thePage = minRownum-1;
            offset = thePage*theSize;
        } else if (isRangePredicate) {
            thePage = (minRownum-1)/theSize + page;
            offset = (minRownum-1)+page*theSize;
        } else {
            thePage = (minRownum-1)/theSize + page;
            offset = (minRownum-1)+page*theSize;
        }
        return new RowNumHandle(isEqualsPredicate, isRangePredicate, minRownum, maxRownum, theSize, thePage, offset);
    }

    public @Nonnull <T> PagedModel.PageMetadata createPageMeta(int page, int pageSize, @Nonnull PagedModel<T> item, @Nonnull PagedModel.PageMetadata apiMeta) {
        PagedModel.PageMetadata metadata;
        if (this.hasRowNumEquals) {
            metadata = new PagedModel.PageMetadata(pageSize, page, 1, 1);
        } else if (this.hasRowNumRange) {
            long totalElements = Math.min(this.maxRowNum - this.minRowNum, apiMeta.getTotalElements());
            metadata = new PagedModel.PageMetadata(this.size, page, totalElements);
        } else {
            metadata = Optional.ofNullable(item.getMetadata()).orElseThrow(()->new IllegalArgumentException("apiMeta has no PageMetadata. API is broken"));
        }
        return metadata;
    }

    public boolean isInRowWindow(long rowNum) {
        if ( this.hasRowNumEquals ) {
            return rowNum == this.minRowNum;
        } else if (this.hasRowNumRange) {
            return rowNum >=this.minRowNum && rowNum < this.maxRowNum;
        } else {
            return true;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("hasRowNumEquals", hasRowNumEquals)
                .add("hasRowNumRange", hasRowNumRange)
                .add("minRownum", minRowNum)
                .add("maxRownum", maxRowNum)
                .add("theSize", size)
                .add("thePage", page)
                .add("offset", offset)
                .toString();
    }

}
