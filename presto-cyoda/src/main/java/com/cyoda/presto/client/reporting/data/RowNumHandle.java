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
import org.checkerframework.checker.nullness.Opt;
import org.springframework.hateoas.PagedModel;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

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


    public static @Nonnull RowNumHandle from(@Nonnull ColumnPredicate<Long> columnPredicate, long page, long size) {
        boolean isEqualsPredicate = columnPredicate.getType() == ColumnPredicate.PredicateType.EQUALITY;
        boolean isRangePredicate = columnPredicate.getType() == ColumnPredicate.PredicateType.RANGE;

        long minRownum = columnPredicate.getLower() != null ?
                Optional.ofNullable(columnPredicate.getLower().value)
                        .orElseThrow(()->new IllegalArgumentException("columnPredicate lower value is null")) : 1;
        long maxRownum = columnPredicate.getUpper() != null ? Optional.ofNullable(columnPredicate.getUpper().value)
                .orElseThrow(()->new IllegalArgumentException("columnPredicate lower value is null")) : Long.MAX_VALUE;

        long theSize = isEqualsPredicate ? 1 : Math.min(size,maxRownum-minRownum);
        long thePage;
        if ( isEqualsPredicate ) {
            thePage = minRownum-1;
        } else if ( isRangePredicate ) {
            thePage = (minRownum-1)/theSize + page;
        } else {
            thePage = (minRownum-1)/theSize + page;
        }
        long offset = (isEqualsPredicate || isRangePredicate ) ? minRownum-1 : thePage*theSize;
        return new RowNumHandle(isEqualsPredicate, isRangePredicate, minRownum, maxRownum, theSize, thePage, offset);
    }

    public @Nonnull <T> PagedModel.PageMetadata createPageMeta(int page, int pageSize, @Nonnull PagedModel<T> item, @Nonnull PagedModel.PageMetadata apiMeta) {
        PagedModel.PageMetadata metadata;
        if (this.hasRowNumEquals) {
            metadata = new PagedModel.PageMetadata(pageSize, page, 1, 1);
        } else if (this.hasRowNumRange) {
            long totalElements = Math.min(this.maxRowNum - this.minRowNum, apiMeta.getTotalElements());
            long totalPages = Math.max(1,totalElements / this.size);
            metadata = new PagedModel.PageMetadata(this.size, page, totalElements, totalPages);
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
