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

import com.cyoda.core.reports.DistributedReportInfoDto;
import com.cyoda.core.model.reports.ReportHistoryFieldsView;
import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.reporting.BasePagingReportsApiHandler;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.logging.SupplierLogger;
import com.cyoda.service.interactors.WrappedEntityModel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.trino.spi.StandardErrorCode;
import io.trino.spi.TrinoException;
import io.trino.spi.type.TypeManager;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.server.core.TypeReferences;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.GROUPING_VERSION_COLUMN;

public class ReportStatisticsApiHttp extends BasePagingReportsApiHandler<ReportConfigKey, DistributedReportInfoDto> implements ReportStatisticsApi {

    protected static final SupplierLogger LOG = SupplierLogger.get(ReportStatisticsApiHttp.class);


    @SuppressWarnings("java:S1075")
    public static final String REPORT_STATS_TEMPLATE = "/{" + HISTORY_REPORT_ID_COLUMN + "}/{" +
            GROUPING_VERSION_COLUMN + "}/stats{?full}";

    private final ReportHistoryApiHttp reportHistoryApiHandler;


    @Inject
    public ReportStatisticsApiHttp(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                   RestTemplateCustomizer restTemplateCustomizer,
                                   ReportHistoryApiHttp reportHistoryApiHandler,
                                   AuthService authService,
                                   CyodaApiRequestStatsMonitor requestStatsMonitor) {
        super(config, restTemplateCustomizer, LOG, authService, requestStatsMonitor);
        this.reportHistoryApiHandler = reportHistoryApiHandler;
    }

    @Override
    protected Optional<PagedModel<DistributedReportInfoDto>> retrievePage(
            ReportConfigKey requestKey, int page,
            int pageSize,
            SizeListener listener
    ) {

        UriTemplate uriTemplate = setupUriTemplate();
        PagedModel<ReportHistoryFieldsView> reportHistoryModel = reportHistoryApiHandler
                .retrievePage(
                        requestKey,
                        page,
                        pageSize,
                        listener).orElse(PagedModel.empty());
        List<DistributedReportInfoDto> reportStatisticsView = getReportStatisticsView(
                uriTemplate, reportHistoryModel.getContent(), requestKey.queryId());

        PagedModel<DistributedReportInfoDto> reportStatsView = PagedModel.of(reportStatisticsView, reportHistoryModel.getMetadata());
        publishSize(listener, reportStatsView);
        return Optional.of(reportStatsView);
    }

    private List<DistributedReportInfoDto> getReportStatisticsView(
            UriTemplate uriTemplate,
            @Nonnull Collection<ReportHistoryFieldsView> history,
            String queryId
    ) {

        if (history.isEmpty()) return Collections.emptyList();
        ImmutableList.Builder<DistributedReportInfoDto> builder = ImmutableList.builder();

        history.forEach(element -> {
            String reportId = element.getReportHistoryFields().get(HISTORY_REPORT_ID_COLUMN).toString();
            String groupingVersion = element.getReportHistoryFields().get(GROUPING_VERSION_COLUMN).toString();
            Map<String, Object> expansion = ImmutableMap.of(
                    HISTORY_REPORT_ID_COLUMN, reportId,
                    GROUPING_VERSION_COLUMN, groupingVersion,
                    "full", true
            );
            URI templatedUri = uriTemplate.expand(expansion);
            Date callTime = new Date();
            Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
            traverson.setRestOperations(getTechRestTemplate());

            TypeReferences.EntityModelType<WrappedEntityModel<DistributedReportInfoDto>> typeReference
                    = new TypeReferences.EntityModelType<WrappedEntityModel<DistributedReportInfoDto>>() {
            };

            try {
                EntityModel<WrappedEntityModel<DistributedReportInfoDto>> entityModel = traverson
                        .follow()
                        .toObject(typeReference);
                Optional<DistributedReportInfoDto> reportStatistics = Optional.ofNullable(entityModel)
                        .map(EntityModel::getContent).map(WrappedEntityModel::getContent);
                builder.add(reportStatistics.orElse(DistributedReportInfoDto.builder().id(reportId).build()));
            } catch (HttpClientErrorException e) {
                throw requestFailedException(this, e, templatedUri);
            } finally {
                registerApiCall(queryId, callTime, templatedUri.toString(), expansion);
            }
        });
        List<DistributedReportInfoDto> result = builder.build();
        LOG.debug("Got %s report statistics", result.size());
        return result;

    }

    private UriTemplate setupUriTemplate() {

        // /report/{id}/{grouping_version}/stats
        URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(REPORT_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new TrinoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, e);
        }
        return UriTemplate.of(uri.toASCIIString() + REPORT_STATS_TEMPLATE);
    }

}