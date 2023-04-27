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

package com.cyoda.presto.client.reporting.groups;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.reporting.CachedPagingReportsApiHandler;
import com.cyoda.presto.client.reporting.stats.ContentIdLoadingCache;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.presto.logging.SupplierLogger;
import com.cyoda.service.api.beans.GroupHeader;
import com.cyoda.service.interactors.WrappedEntityModel;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
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

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.GROUPING_VERSION_COLUMN;

/**
 * This is not intended to be exposed as a table, but used internally to fill the real table.
 */
public class ReportGroupsApiHandler extends CachedPagingReportsApiHandler<GroupsRequestKey, GroupingHandle> {

    protected static final SupplierLogger LOG = SupplierLogger.get(ReportGroupsApiHandler.class);

    public static final String REPORT_GROUPS_TEMPLATE = "/{" + HISTORY_REPORT_ID_COLUMN + "" +
            "}/{" +
            GROUPING_VERSION_COLUMN + "}/groups";

    public static final String IN_PREDICATES = " in predicates";
    public static final String NO_RESULT_FOUND_FOR_COLUMN = "no result found for column ";
//    private final CyodaColumnHandle groupingVersionColumn;
//    private final CyodaColumnHandle reportNameColumn;
//    private final CyodaColumnHandle reportIdColumn;

    @Inject
    public ReportGroupsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                  RestTemplateCustomizer restTemplateCustomizer,
                                  AuthService authService,
                                  CyodaApiRequestStatsMonitor requestStatsMonitor,
                                  CyodaCacheMonitor cacheMonitor) {
        super(connectorId, config, typeManager, restTemplateCustomizer, LOG, authService, requestStatsMonitor, cacheMonitor);
//        this.groupingVersionColumn = staticMetaProvider.getReportGroups().getGroupingVersionColumn();
//        this.reportIdColumn = staticMetaProvider.getReportGroups().getReportIdColumn();
//        // An External Column
//        this.reportNameColumn = staticMetaProvider.getReportGroups().getReportConfigIdColumn();
    }

    @Override
    public Optional<PagedModel<GroupingHandle>> retrievePage(
            GroupsRequestKey requestKey, int page,
            int pageSize,
            SizeListener listener
    ) {

        int size = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;

//        PredicateTraversal<String> stringPredicateTraversal = PredicateTraversal.of(predicates, String.class);
//        PredicateTraversal<UUID> uuidPredicateTraversal = PredicateTraversal.of(predicates, UUID.class);

        UriTemplate uriTemplate = setupUriTemplate();

        ImmutableMap.Builder<String, Object> expansionBuilder = ImmutableMap.<String, Object>builder()
                .put(PAGE_REQUEST_PARAMETER, page)
                .put(SIZE_REQUEST_PARAMETER, size);


//        String reportId = this.mixinColumn(expansionBuilder, stringPredicateTraversal, this.reportIdColumn)
//                .orElseThrow(() -> new IllegalArgumentException(NO_RESULT_FOUND_FOR_COLUMN + HISTORY_REPORT_ID_COLUMN + IN_PREDICATES));
//        UUID groupingVersion = this.mixinColumn(expansionBuilder, uuidPredicateTraversal, this.groupingVersionColumn)
//                .orElseThrow(() -> new IllegalArgumentException(NO_RESULT_FOUND_FOR_COLUMN + GROUPING_VERSION_COLUMN + IN_PREDICATES));

        expansionBuilder.put(HISTORY_REPORT_ID_COLUMN, requestKey.reportId());
        expansionBuilder.put(GROUPING_VERSION_COLUMN, requestKey.groupingVersion());
        ImmutableMap<String, Object> expansion = expansionBuilder.build();
        URI templatedUri = uriTemplate.expand(expansion);

        Date apiCallTime = new Date();
        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(restTemplateCustomizer.getRestTemplateWithTechAuth());

        TypeReferences.PagedModelType<WrappedEntityModel<GroupHeader>> typeReference =
                new TypeReferences.PagedModelType<WrappedEntityModel<GroupHeader>>() {
                };

        try {
            final PagedModel<WrappedEntityModel<GroupHeader>> fieldsViews = traverson
                    .follow()
                    .toObject(typeReference);
            Optional<PagedModel<GroupingHandle>> groupingHandles = Optional.ofNullable(fieldsViews)
                    .map(item -> {
                        List<GroupingHandle> handles = item.getContent().stream()
                                .map(handle -> new GroupingHandle(requestKey.reportId(), requestKey.groupingVersion(), handle.getContent()))
                                .collect(Collectors.toList());
                        return PagedModel.of(handles, item.getMetadata());
                    });
            publishSize(listener, groupingHandles.orElse(PagedModel.empty()));
            if (fieldsViews != null)
                registerApiCall(requestKey.queryId(), apiCallTime, templatedUri.toString(), expansion);
            return groupingHandles;
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, "retrieveCollection", e, templatedUri);
        }
    }

    @Override
    protected LoadingCache<GroupsRequestKey, List<GroupingHandle>> setupCache(CacheLoader<GroupsRequestKey, List<GroupingHandle>> loader) {
        return Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofDays(1))
                .recordStats()
                .build(loader);
    }

    @Override
    protected void registerCache(CyodaCacheMonitor cacheMonitor, ContentIdLoadingCache<GroupsRequestKey, List<GroupingHandle>> cache) {
        cacheMonitor.register("GROUPS", cache, GroupsRequestKey::reportId, List::size);
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
        return UriTemplate.of(uri.toASCIIString() + REPORT_GROUPS_TEMPLATE)
                .with(vars);
    }

}
