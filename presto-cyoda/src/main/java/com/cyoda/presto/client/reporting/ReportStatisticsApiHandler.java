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

package com.cyoda.presto.client.reporting;

import com.cyoda.core.model.reports.DistributedReportInfoView;
import com.cyoda.core.model.reports.ReportHistoryFieldsView;
import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.client.PagingApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.jodabeans.StandardColumnDefinition;
import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.logic.PredicateNode;
import com.cyoda.presto.client.paging.PagedIterator;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.beans.MetaProperty;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.server.core.TypeReferences;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;

public class ReportStatisticsApiHandler extends BaseReportsApiHandler<DistributedReportInfoView>
        implements PagingApiRequestHandler<DistributedReportInfoView> {

    private static final String REPORT_ID_COLUMN_NAME = "id";
    private static final String GROUPING_VERSION_COLUMN_NAME = "groupingVersion";

    @SuppressWarnings("java:S1075")
    public static final String REPORT_STATS_TEMPLATE = "/{" + REPORT_ID_COLUMN_NAME + "}/{" +
            GROUPING_VERSION_COLUMN_NAME + "}/stats{?full}";

    private final ReportHistoryApiHandler reportHistoryApiHandler;

    private static final List<ColumnDefinition> COLUMN_DEFS = StandardColumnDefinition.builder()
            .add(DistributedReportInfoView.meta())
            .build();

    @Inject
    public ReportStatisticsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                      RestTemplateCustomizer restTemplateCustomizer) {
        super(connectorId, config, typeManager,
                REPORT_ENDPOINT, CyodaStaticReportTable.REPORT_STATS.name(), COLUMN_DEFS,restTemplateCustomizer);
        this.reportHistoryApiHandler = new ReportHistoryApiHandler(connectorId,config,typeManager,restTemplateCustomizer);
    }

    @Override
    public String getHandlerKey() {
        return CyodaStaticReportTable.REPORT_STATS.name();
    }

    @Override
    public Optional<PagedModel<DistributedReportInfoView>> retrievePage(
            int page,
            int pageSize,
            List<CyodaColumnHandle> projectedColumns,
            PredicateNode<Any> predicates
    ) {

        UriTemplate uriTemplate = setupUriTemplate();
        PagedModel<ReportHistoryFieldsView> reportHistoryModel = reportHistoryApiHandler
                .retrievePage(
                        page,
                        pageSize,
                        projectedColumns,
                        predicates
                ).orElse(PagedModel.empty());
        List<DistributedReportInfoView> reportStatisticsView = getReportStatisticsView(uriTemplate, reportHistoryModel.getContent());

        return Optional.of(PagedModel.of(reportStatisticsView,reportHistoryModel.getMetadata()));
    }

    private List<DistributedReportInfoView> getReportStatisticsView(
            UriTemplate uriTemplate,
            @Nonnull Collection<ReportHistoryFieldsView> history
    ) {

        if ( history.isEmpty() ) return Collections.emptyList();
        ImmutableList.Builder<DistributedReportInfoView> builder = ImmutableList.builder();

        history.forEach(element -> {
            String reportId = element.getReportHistoryFields().get(REPORT_ID_COLUMN_NAME).toString();
            String groupingVersion = element.getReportHistoryFields().get(GROUPING_VERSION_COLUMN_NAME).toString();
            URI templatedUri = uriTemplate.expand(
                    ImmutableMap.of(
                            REPORT_ID_COLUMN_NAME, reportId,
                            GROUPING_VERSION_COLUMN_NAME,groupingVersion,
                            "full",true
                    )
            );

            Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
            traverson.setRestOperations(restTemplate);

            TypeReferences.EntityModelType<DistributedReportInfoView> typeReference
                    = new TypeReferences.EntityModelType<DistributedReportInfoView>(){};

            try {
                EntityModel<DistributedReportInfoView> entityModel = traverson
                        .follow()
                        .toObject(typeReference);
                Optional<DistributedReportInfoView> reportStatistics = Optional.ofNullable(entityModel)
                        .map(EntityModel::getContent);
                builder.add(reportStatistics.orElse(DistributedReportInfoView.builder().id(reportId).build()));
            } catch (HttpClientErrorException e) {
                throw requestFailedException(this, "retrieveCollection", e, templatedUri);
            }
        });
        List<DistributedReportInfoView> result = builder.build();
        LOG.debug("Got %s report definitions",result.size());
        return result;

    }

    private UriTemplate setupUriTemplate() {

        // /report/{id}/{grouping_version}/stats
        URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(REPORT_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, e);
        }
        return UriTemplate.of(uri.toASCIIString()+ REPORT_STATS_TEMPLATE);
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull DistributedReportInfoView field, CyodaColumnHandle columnHandle) {
        MetaProperty<?> metaProperty = field.metaBean().metaPropertyMap().get(columnHandle.getColumnName());
        if ( metaProperty == null ) {
            throw new IllegalArgumentException(columnHandle.getColumnName()+" is not defined on ReportDefinitionsView");
        }
        return field.metaBean().metaProperty(columnHandle.getColumnName()).get(field);
    }

    @Override
    public Iterator<DistributedReportInfoView> getResponseIterator(
            int pageSize,
            List<CyodaColumnHandle> projectedColumns,
            PredicateNode<Any> predicates
    ) {
        return new PagedIterator<>(this, pageSize, projectedColumns, predicates).iterator();
    }

}