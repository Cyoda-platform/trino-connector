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

package com.cyoda.connector.client.paging;

import com.cyoda.connector.logging.SupplierLogger;
import io.trino.spi.TrinoException;
import org.springframework.hateoas.PagedModel;
import reactor.core.publisher.Flux;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static com.cyoda.connector.CyodaErrorCode.CYODA_API_ERROR;

public class PagingFluxProvider<T> {
    private static final SupplierLogger LOG = SupplierLogger.get(PagingFluxProvider.class);
    private final Function<Integer, PagingHandle<?,T>> pagingHandleGetter;


    public PagingFluxProvider(Function<Integer, PagingHandle<?,T>> pagingHandleGetter) {
        this.pagingHandleGetter = pagingHandleGetter;
    }

    static class PagingData<T> {
        private final AtomicLong currentElementOnPage = new AtomicLong();
        private final long maxPages;
        private final long maxEntries;
        private final long metaPageSize;
        private final long itemLimit;
        private final Iterator<T> contentIter;

        PagingData(long maxPages, long maxEntries, long metaPageSize, long itemLimit, Iterator<T> contentIter) {
            this.maxPages = maxPages;
            this.maxEntries = maxEntries;
            this.metaPageSize = metaPageSize;
            this.itemLimit = itemLimit;
            this.contentIter = contentIter;
        }
    }

    public Flux<T> generate(int startPage) {
        AtomicInteger currentPage = new AtomicInteger(startPage);
        final AtomicLong currentPos = new AtomicLong();
        return Flux.generate(() -> {
            PagingHandle<?,T> pagingHandle = pagingHandleGetter.apply(currentPage.getAndIncrement());
            // We fix the meta which tells us about how many pages / elements on the first call
            // We assume that this is effectively a committed read, which is true if we are
            // reading from Cyoda at a fixed pointInTime.
            // If reading against data that may be deleted/added during calls (i.e. StaticEntities)
            // then it's better to use a huge page size and slurp this in all at once without paging.
            PagedModel.PageMetadata pageMeta = pagingHandle.getPageMeta()
                    .orElseThrow(() -> new TrinoException(CYODA_API_ERROR, "No paging data attached to HATEOAS response. Cannot iterate"));
            long maxPages = pageMeta.getTotalPages();
            long maxEntries = pageMeta.getTotalElements();
            long metaPageSize = pageMeta.getSize();
            PagedModel<T> pagedModel = pagingHandle.getPagedModel().orElse(PagedModel.empty());
            long itemLimit = currentPage.get() == maxPages ? maxEntries - currentPos.get() : metaPageSize;
            Iterator<T> contentIter = pagedModel.getContent().iterator();
            return new PagingData<>(maxPages, maxEntries, metaPageSize, itemLimit, contentIter);
        }, (pageData, sink) -> {
            if (currentPos.get() < pageData.maxEntries) { // This is to safeguard against API sending more than wanted
                if (pageData.contentIter.hasNext() && pageData.currentElementOnPage.get() < pageData.itemLimit) {
                    T item = pageData.contentIter.next();
                    long theCurrentPos = currentPos.incrementAndGet();
                    long theCurrentElementOnPage = pageData.currentElementOnPage.incrementAndGet();

                    LOG.debug("got element %d / %d (Total) with %s", () -> theCurrentElementOnPage, () -> theCurrentPos, () -> item);
                    if (theCurrentPos > pageData.maxEntries) {
                        throw new IllegalStateException("Reading more than expected. Actual=" + theCurrentPos + ", Max=" + pageData.maxEntries);
                    }
                    sink.next(item);
                } else { // Load next page
                    if (currentPage.get() < pageData.maxPages) {
                        PagingHandle<?,T> nextPage = pagingHandleGetter.apply(currentPage.getAndIncrement());
                        pageData.currentElementOnPage.set(0);
                        PagedModel<T> pagedModel = nextPage.getPagedModel().orElse(PagedModel.empty());
                        long itemLimit = currentPage.get() == pageData.maxPages ? pageData.maxEntries - currentPos.get()
                                : pageData.metaPageSize;
                        Iterator<T> iterator = pagedModel.getContent().iterator();
                        pageData = new PagingData<>(
                                pageData.maxPages,
                                pageData.maxEntries,
                                pageData.metaPageSize,
                                itemLimit,
                                iterator
                        );
                    }
                    if (pageData.contentIter.hasNext() && pageData.itemLimit>=1) {
                        sink.next(pageData.contentIter.next());
                        currentPos.incrementAndGet();
                    } else {
                        sink.complete();
                    }
                }
            } else {
                sink.complete();
            }
            return pageData;
        });
    }
}
