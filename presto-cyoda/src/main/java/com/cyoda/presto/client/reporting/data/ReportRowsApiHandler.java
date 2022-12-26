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

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.logic.ColumnPredicate;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.paging.PagingFluxProvider;
import com.cyoda.presto.client.paging.PagingHandle;
import com.cyoda.presto.client.reporting.BaseReportsApiHandler;
import com.cyoda.presto.client.reporting.PredicateTraversal;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportMetadataProvider;
import com.cyoda.presto.client.reporting.stats.RowPageRequestStats;
import com.cyoda.presto.client.reporting.stats.RowRequestStats;
import com.cyoda.presto.client.reporting.stats.RowRequestStatsHandler;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.cyoda.service.api.beans.ReportRow;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.trino.spi.StandardErrorCode;
import io.trino.spi.TrinoException;
import io.trino.spi.type.TypeManager;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.server.core.TypeReferences;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_GROUP_JSON_BASE64_VARIABLE;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ID_COLUMN;

public class ReportRowsApiHandler extends BaseReportsApiHandler<RowsRequestKey, RowHandle>{

    protected static final SupplierLogger LOG = SupplierLogger.get(ReportRowsApiHandler.class);

    static final String REPORT_ROWS_TEMPLATE = "/{" + ROW_REPORT_ID_COLUMN + "" +
            "}/group_rows/{" +
            ROW_GROUP_JSON_BASE64_VARIABLE + "}";

    private final CyodaColumnHandle rowNumberColumn;
    private final RowRequestStatsHandler statsHandler;

    @Inject
    public ReportRowsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                RestTemplateCustomizer restTemplateCustomizer, StaticReportMetadataProvider staticMetaProvider,
                                RowRequestStatsHandler statsHandler) {
        super(connectorId, config, typeManager, restTemplateCustomizer, LOG);
        this.rowNumberColumn = staticMetaProvider.getReportRows().getRowNumberColumn();
        this.statsHandler = statsHandler;
    }


    private Optional<PagedModel<RowHandle>> retrievePage(
            RowsRequestKey requestKey, int page,
            int pageSize,
            CompoundPredicateNode predicates,
            SizeListener listener,
            RowRequestStats requestStats
    ) {

        int size = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;

        PredicateTraversal<Long> longPredicateTraversal = PredicateTraversal.of(predicates, Long.class);

        UriTemplate uriTemplate = setupUriTemplate();

        Set<ColumnPredicate<Long>> rowNumPredicate = longPredicateTraversal.parseFor(this.rowNumberColumn);

        Preconditions.checkArgument(!rowNumPredicate.isEmpty(), "Bug in Traversal");

        return rowNumPredicate.stream().flatMap(it -> RowNumHandle.from(it, page, size).stream())
                .map(it -> exchange(
                        page,
                        size,
                        listener,
                        requestKey,
                        uriTemplate,
                        it))
                .map(exchangeResult -> {
                    requestStats.addPageRequest(exchangeResult.pageRequestStats);
                    return exchangeResult.pagedModel;
                })
                .reduce(Optional.of(PagedModel.empty()), (result, rowHandles) ->
                        Optional.of(merge(result.get(), rowHandles.orElse(null)))
                );
    }

    private @Nonnull PagedModel<RowHandle> merge(@Nonnull PagedModel<RowHandle> result, @Nullable PagedModel<RowHandle> response) {
        if (response == null) return result;
        ImmutableList.Builder<RowHandle> builder = ImmutableList.builder();
        builder.addAll(result.getContent());
        builder.addAll(response.getContent());
        Collection<RowHandle> content = builder.build();

        PagedModel.PageMetadata resultMetadata = result.getMetadata();
        PagedModel.PageMetadata responseMetadata = Optional.ofNullable(response.getMetadata()).orElseThrow(
                () -> new IllegalArgumentException("No pageMeta attached to response. Cannot continue")
        );

        long totalElements = Optional.ofNullable(resultMetadata).map(PagedModel.PageMetadata::getTotalElements).orElse(0L)
                + responseMetadata.getTotalElements();
        PagedModel.PageMetadata meta = new PagedModel.PageMetadata(
                Optional.ofNullable(resultMetadata).map(PagedModel.PageMetadata::getSize).orElse(responseMetadata.getSize()),
                Optional.ofNullable(resultMetadata).map(PagedModel.PageMetadata::getNumber).orElse(responseMetadata.getNumber()),
                totalElements);
        return PagedModel.of(content, meta);
    }

    private ExchangeResult exchange(int page, int pageSize, SizeListener listener,
                                                     RowsRequestKey requestKey,
                                                     UriTemplate uriTemplate,
                                                     RowNumHandle rowNumHandle) {
        long startTime = System.currentTimeMillis();
        ImmutableMap.Builder<String, Object> expansionBuilder = ImmutableMap.<String, Object>builder()
                .put(PAGE_REQUEST_PARAMETER, rowNumHandle.page)
                .put(SIZE_REQUEST_PARAMETER, rowNumHandle.size);


        expansionBuilder.put(ROW_REPORT_ID_COLUMN, requestKey.reportId());
        expansionBuilder.put(ROW_GROUP_JSON_BASE64_VARIABLE, requestKey.groupJsonBase64());

        URI templatedUri = uriTemplate.expand(expansionBuilder.build());

        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(restTemplateCustomizer.getRestTemplateWithTechAuth());

        TypeReferences.PagedModelType<ReportRow> typeReference =
                new TypeReferences.PagedModelType<ReportRow>() {
                };

        try {
            final PagedModel<ReportRow> fieldsViews = traverson
                    .follow()
                    .toObject(typeReference);
            publishSize(listener, fieldsViews);
            return new ExchangeResult(
                    Optional.ofNullable(fieldsViews)
                    .map(item -> {
                        AtomicLong rowNum = new AtomicLong(rowNumHandle.offset);
                        List<RowHandle> handles = item.getContent().stream()
                                .map(reportRow ->
                                        new RowHandle(requestKey.reportId(),
                                                requestKey.groupingVersion(),
                                                requestKey.groupJsonBase64(),
                                                reportRow, rowNum.incrementAndGet()))
                                .filter(reportRow -> rowNumHandle.isInRowWindow(reportRow.rowNum()))
                                .limit(rowNumHandle.size) // This to ringfence buggy API that sends one than the page size.
                                .collect(Collectors.toList());
                        PagedModel.PageMetadata apiMeta = Optional.ofNullable(fieldsViews.getMetadata()).orElseThrow(() -> new IllegalStateException("No meta attached"));
                        PagedModel.PageMetadata metadata = rowNumHandle.createPageMeta(page, pageSize, item, apiMeta);
                        return PagedModel.of(handles, metadata);
                    }),
                    new RowPageRequestStats(page, pageSize, rowNumHandle.page, rowNumHandle.size, System.currentTimeMillis() - startTime)
            );
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, "retrieveCollection", e, templatedUri);
        }
    }


    private UriTemplate setupUriTemplate() {

        URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(REPORT_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new TrinoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, e);
        }
        final ImmutableList.Builder<TemplateVariable> builder = ImmutableList.builder();
        builder.add(
                TemplateVariable.requestParameter(PAGE_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued(SIZE_REQUEST_PARAMETER)
        );

        TemplateVariables vars = new TemplateVariables(builder.build());
        return UriTemplate.of(uri.toASCIIString() + REPORT_ROWS_TEMPLATE)
                .with(vars);
    }




    public Flux<RowHandle> asFlux(RowsRequestKey requestKey, CompoundPredicateNode predicates, SizeListener listener) {

        int pageSize = getPageSize();
        logCreation(pageSize, predicates, log);
        RowRequestStats requestStats = statsHandler.registerCall(requestKey, predicates);
        Function<Integer, PagingHandle<?, RowHandle>> pagingHandleGetter = page ->
                new PagingHandle<>(retrievePage(requestKey, page, pageSize, predicates, listener, requestStats));
        return new PagingFluxProvider<>(pagingHandleGetter)
                .generate(0)
                .doOnComplete(()-> LOG.info("Fulfilled rows request to cyoda:\n" + requestStats.toString()));
    }

    private record ExchangeResult(Optional<PagedModel<RowHandle>> pagedModel, RowPageRequestStats pageRequestStats){};
}

