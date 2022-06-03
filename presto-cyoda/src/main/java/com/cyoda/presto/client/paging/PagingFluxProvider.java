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

package com.cyoda.presto.client.paging;

import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.spi.PrestoException;
import org.springframework.hateoas.PagedModel;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static com.cyoda.presto.CyodaErrorCode.CYODA_API_ERROR;

public class PagingFluxProvider<T> {
    private static final SupplierLogger LOG = SupplierLogger.get(PagingFluxProvider.class);
    private final Function<Integer, PagingHandle<T>> pagingHandleGetter;


    public PagingFluxProvider(Function<Integer, PagingHandle<T>> pagingHandleGetter) {
        this.pagingHandleGetter = pagingHandleGetter;
    }

    public Flux<T> create(int startPage) {
        AtomicInteger currentPage = new AtomicInteger(startPage);
        return Flux.create(sink -> {
            PagingHandle<T> pagingHandle = pagingHandleGetter.apply(currentPage.getAndIncrement());
            AtomicLong currentPos = new AtomicLong();
            AtomicLong currentElementOnPage = new AtomicLong();
            // We fix the meta which tells us about how many pages / elements on the first call
            // We assume that this is effectively a committed read, which is true if we are
            // reading from Cyoda at a fixed pointInTime.
            // If reading against data that may be deleted/added during calls (i.e. StaticEntities)
            // then it's better to use a huge page size and slurp this in all at once without paging.
            PagedModel.PageMetadata pageMeta = pagingHandle.getPageMeta()
                    .orElseThrow(() -> new PrestoException(CYODA_API_ERROR, "No paging data attached to HATEOAS response. Cannot iterate"));
            long maxPages = pageMeta.getTotalPages();
            long maxEntries = pageMeta.getTotalElements();
            long metaPageSize = pageMeta.getSize();
            while(currentPage.get() <= maxPages && currentPos.get() < maxEntries ) {
                PagedModel<T> pagedModel = pagingHandle.getPagedModel().orElse(PagedModel.empty());
                long itemLimit = currentPage.get() == maxPages ? maxEntries-currentPos.get() : metaPageSize;
                pagedModel.getContent().stream().limit(itemLimit).forEach(item -> {
                    long theCurrentPos = currentPos.incrementAndGet();
                    long theCurrentElementOnPage = currentElementOnPage.incrementAndGet();

                    LOG.debug("got element %d / %d (Total) with %s",()->theCurrentElementOnPage, ()->theCurrentPos, () -> item);
                    if ( theCurrentPos > maxEntries ) {
                        throw new IllegalStateException("Reading more than expected. Actual="+ theCurrentPos+ ", Max="+maxEntries);
                    }
                    sink.next(item);
                });
                if ( currentPage.get() < maxPages ) {
                    pagingHandle = pagingHandleGetter.apply(currentPage.getAndIncrement());
                    currentElementOnPage.set(0);
                } else {
                    currentPage.getAndIncrement();
                }
            }
            sink.complete();
        });
    }
}
