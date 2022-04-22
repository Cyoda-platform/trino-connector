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

import com.cyoda.presto.client.CyodaApiRequestHandler;
import org.springframework.hateoas.PagedModel;

import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class PagingHandle<T> {

    private final Optional<PagedModel.PageMetadata> pageMeta;
    private final Optional<PagedModel<T>> pagedModel;


    public PagingHandle(CyodaApiRequestHandler<T> requestHandler, int pageNum, int pageSize, String query) {
        this.pagedModel = requestHandler.retrievePage(pageNum,pageSize,query);
        this.pageMeta = pagedModel.map(PagedModel::getMetadata);
    }

    public Optional<PagedModel.PageMetadata> getPageMeta() {
        return pageMeta;
    }

    public Optional<PagedModel<T>> getPagedModel() {
        return pagedModel;
    }
}
