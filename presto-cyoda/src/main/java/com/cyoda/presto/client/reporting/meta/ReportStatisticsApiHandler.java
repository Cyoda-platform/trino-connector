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

import com.cyoda.core.model.reports.DistributedReportInfoView;
import com.cyoda.core.model.reports.ReportHistoryFieldsView;
import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.PagingApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.jodabeans.StandardColumnDefinition;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.reporting.BasePagingReportsApiHandler;
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.logging.SupplierLogger;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.reporting.AbstractTableHolder.TableDefinitionHandle.asTableDefinitionHandle;
import static com.cyoda.presto.client.reporting.CyodaStaticReportTable.REPORT_STATS;
import static com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler.GROUPING_VERSION_COLUMN;

public class ReportStatisticsApiHandler extends BasePagingReportsApiHandler<DistributedReportInfoView>
        implements PagingApiRequestHandler<DistributedReportInfoView> {

    protected static final SupplierLogger LOG = SupplierLogger.get(ReportStatisticsApiHandler.class);


    @SuppressWarnings("java:S1075")
    public static final String REPORT_STATS_TEMPLATE = "/{" + HISTORY_REPORT_ID_COLUMN + "}/{" +
            GROUPING_VERSION_COLUMN + "}/stats{?full}";

    private final ReportHistoryApiHandler reportHistoryApiHandler;

    private static final List<ColumnDefinition> COLUMN_DEFS = StandardColumnDefinition.builder()
            .add(DistributedReportInfoView.meta())
            .build();

    @Inject
    public ReportStatisticsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                      RestTemplateCustomizer restTemplateCustomizer) {
        super(connectorId, config, typeManager, REPORT_ENDPOINT ,restTemplateCustomizer,LOG);
        this.reportHistoryApiHandler = new ReportHistoryApiHandler(connectorId,config,typeManager,restTemplateCustomizer);
    }

    @Override
    protected Map<TableDefinitionHandle, List<ColumnDefinition>> refreshFieldDefs(AuthContext authContext) {
        return Collections.singletonMap(asTableDefinitionHandle(REPORT_STATS.name()),COLUMN_DEFS);
    }

    @Override
    public String getHandlerKey() {
        return REPORT_STATS.name();
    }

    @Override
    public Optional<PagedModel<DistributedReportInfoView>> retrievePage(
            AuthContext authContext,
            int page,
            int pageSize,
            List<CyodaColumnHandle> projectedColumns,
            CompoundPredicateNode predicates,
            SizeListener listener
    ) {

        UriTemplate uriTemplate = setupUriTemplate();
        PagedModel<ReportHistoryFieldsView> reportHistoryModel = reportHistoryApiHandler
                .retrievePage(
                        authContext,
                        page,
                        pageSize,
                        projectedColumns,
                        predicates,
                        listener
                ).orElse(PagedModel.empty());
        List<DistributedReportInfoView> reportStatisticsView = getReportStatisticsView(authContext,uriTemplate, reportHistoryModel.getContent());

        PagedModel<DistributedReportInfoView> reportStatsView = PagedModel.of(reportStatisticsView, reportHistoryModel.getMetadata());
        publishSize(listener,reportStatsView);
        return Optional.of(reportStatsView);
    }

    private List<DistributedReportInfoView> getReportStatisticsView(
            AuthContext authContext,
            UriTemplate uriTemplate,
            @Nonnull Collection<ReportHistoryFieldsView> history
    ) {

        if ( history.isEmpty() ) return Collections.emptyList();
        ImmutableList.Builder<DistributedReportInfoView> builder = ImmutableList.builder();

        history.forEach(element -> {
            String reportId = element.getReportHistoryFields().get(HISTORY_REPORT_ID_COLUMN).toString();
            String groupingVersion = element.getReportHistoryFields().get(GROUPING_VERSION_COLUMN).toString();
            URI templatedUri = uriTemplate.expand(
                    ImmutableMap.of(
                            HISTORY_REPORT_ID_COLUMN, reportId,
                            GROUPING_VERSION_COLUMN,groupingVersion,
                            "full",true
                    )
            );

            Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
            traverson.setRestOperations(restTemplateCustomizer.getRestTemplate(authContext));

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
        LOG.debug("Got %s report statistics",result.size());
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
}