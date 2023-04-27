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

package com.cyoda.presto.client.reporting.meta;

import com.cyoda.core.model.reports.ReportHistoryFieldsView;
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

import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportTable.REPORT_HISTORIES;

public class ReportHistoryApiHandler extends CachedPagingReportsApiHandler<ReportConfigKey, ReportHistoryFieldsView> {

    private static final SupplierLogger LOG = SupplierLogger.get(ReportHistoryApiHandler.class);

    public static final String REPORT_HISTORY_ENDPOINT = "/api/platform-api/reporting/history";
    public static final String HISTORY_REPORT_NAME_REQUEST_PARAMETER = "report_name";
    public static final String HISTORY_REPORT_NAMES_REQUEST_PARAMETER = "report_names";
    public static final String HISTORY_REPORT_IDS_REQUEST_PARAMETER = "reportIds";
    public static final String HISTORY_FILTER_BY_TYPE_REQUEST_PARAMETER = "filterByType";
//    private final CyodaColumnHandle typeColumn;
//    private final CyodaColumnHandle reportNameColumn;
//    private final CyodaColumnHandle reportIdColumn;

    public static final List<String> selectedFields = REPORT_HISTORIES.getFieldList();

    @Inject
    public ReportHistoryApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                   RestTemplateCustomizer restTemplateCustomizer,
                                   AuthService authService,
                                   CyodaApiRequestStatsMonitor requestStatsMonitor,
                                   CyodaCacheMonitor cacheMonitor) {
        super(connectorId, config, typeManager, restTemplateCustomizer, LOG, authService, requestStatsMonitor, cacheMonitor);
//        this.typeColumn = staticMetaProvider.getReportHistory().getTypeColumn();
//        this.reportNameColumn = staticMetaProvider.getReportHistory().getReportNameColumn();
//        this.reportIdColumn = staticMetaProvider.getReportHistory().getReportIdColumn();
    }


    @Override
    public Optional<PagedModel<ReportHistoryFieldsView>> retrievePage(
            ReportConfigKey requestKey, int page,
            int pageSize,
            SizeListener listener
    ) {
        int size = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;


        ImmutableMap.Builder<String, Object> expansionBuilder = ImmutableMap.<String, Object>builder()
                .put(PAGE_REQUEST_PARAMETER, page)
                .put(SIZE_REQUEST_PARAMETER, size)
                .put(FIELDS_REQUEST_PARAMETER, selectedFields);


        expansionBuilder.put(HISTORY_REPORT_NAME_REQUEST_PARAMETER, requestKey.configId());
        UriTemplate uriTemplate = setupUriTemplate();
        ImmutableMap<String, Object> expansion = expansionBuilder.build();
        URI templatedUri = uriTemplate.expand(expansion);

        Date apiCallTime = new Date();
        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(restTemplateCustomizer.getRestTemplateWithTechAuth());

        TypeReferences.PagedModelType<ReportHistoryFieldsView> typeReference =
                new TypeReferences.PagedModelType<ReportHistoryFieldsView>() {
                };

        try {
            final PagedModel<ReportHistoryFieldsView> fieldsViews = traverson
                    .follow()
                    .toObject(typeReference);
            publishSize(listener, fieldsViews);
            if (fieldsViews != null)
                registerApiCall(requestKey.queryId(), apiCallTime, templatedUri.toString(), expansion);
            return Optional.ofNullable(fieldsViews);
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, "retrieveCollection", e, templatedUri);
        }
    }

    @Override
    protected LoadingCache<ReportConfigKey, List<ReportHistoryFieldsView>> setupCache(CacheLoader<ReportConfigKey, List<ReportHistoryFieldsView>> loader) {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(5))
                .recordStats()
                .build(loader);
    }

    @Override
    protected void registerCache(CyodaCacheMonitor cacheMonitor, ContentIdLoadingCache<ReportConfigKey, List<ReportHistoryFieldsView>> cache) {
        cacheMonitor.register("HISTORY", cache,
                ReportConfigKey::configId, List::size);
    }

    private UriTemplate setupUriTemplate() {

        URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(REPORT_HISTORY_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new TrinoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, e);
        }
        final ImmutableList.Builder<TemplateVariable> builder = ImmutableList.builder();
        builder.add(
                TemplateVariable.requestParameter(PAGE_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued(SIZE_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued(FIELDS_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued(HISTORY_FILTER_BY_TYPE_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued("username"),
                TemplateVariable.requestParameterContinued(HISTORY_REPORT_NAME_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued(HISTORY_REPORT_NAMES_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued(HISTORY_REPORT_IDS_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued("from"),
                TemplateVariable.requestParameterContinued("to")
        );

        TemplateVariables vars = new TemplateVariables(builder.build());
        return UriTemplate.of(uri.toASCIIString())
                .with(vars);
    }

}
