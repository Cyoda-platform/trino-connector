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

import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.PagingApiRequestHandler;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.spi.PrestoException;
import org.springframework.hateoas.PagedModel;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import static com.cyoda.presto.CyodaErrorCode.CYODA_API_ERROR;

public class PagedIterator<T> implements Iterable<T> {

    private static final SupplierLogger LOG = SupplierLogger.get(PagedIterator.class);
    private final Function<Integer, PagingHandle<T>> pagingHandleSupplier;

    public PagedIterator(AuthContext authContext, PagingApiRequestHandler<T> requestHandler, int pageSize,
                         List<CyodaColumnHandle> projectedColumns, CompoundPredicateNode predicates) {
        pagingHandleSupplier = page -> new PagingHandle<T>(authContext, requestHandler, page, pageSize, projectedColumns, predicates);

    }

     // TODO: This will only work if you call hasNext() at each step.
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int currentPage = 0;
            // Do the API call to load the first page.
            PagingHandle<T> pagingHandle = pagingHandleSupplier.apply(currentPage++);

            PagedModel<T> pagedModel = pagingHandle.getPagedModel().orElse(PagedModel.empty());

            // We fix the meta which tells us about how many pages / elements on the first call
            // We assume that this is effectively a committed read, which is true if we are
            // reading from Cyoda at a fixed pointInTime.
            // If reading against data that may be deleted/added during calls (i.e. StaticEntities)
            // then it's better to use a huge page size and slurp this in all at once without paging.
            final PagedModel.PageMetadata pageMeta = pagingHandle.getPageMeta()
                    .orElseThrow(() -> new PrestoException(CYODA_API_ERROR,"No paging data attached to HATEOAS response. Cannot iterate"));
            Iterator<T> iterator = pagedModel.iterator();

            long currentPos = 0;
            long currentElementOnPage = 0;
            final long maxPages = pageMeta.getTotalPages();
            final long maxEntries = pageMeta.getTotalElements();
            final long pageSize = pageMeta.getSize();

            @Override
            public boolean hasNext() {
                boolean hasNext = iterator.hasNext();

                if ( !hasNext && currentPage >= maxPages)
                    return false;

                // We might get more elements that what the page size stipulates.
                // Because of buggy HATEOAS endpoints.
                // So limit the calls strictly to the page size.
                if ( !hasNext || currentElementOnPage > pageSize-1 ) {
                    // Do the API call to load the next page.
                    pagingHandle = pagingHandleSupplier.apply(currentPage++);
                    currentElementOnPage = 0;
                    pagedModel = pagingHandle.getPagedModel().orElse(PagedModel.empty());
                    iterator = pagedModel.iterator();
                    return iterator.hasNext();
                }
                return hasNext;
            }

            @Override
            public T next() {
                currentPos++;
                currentElementOnPage++;
                T next = iterator.next();

                LOG.debug("got %s",() -> next);
                if ( currentPos > maxEntries ) {
                    LOG.error("Reading more than expected!");
                }

                return next;
            }
        };
    }

}
