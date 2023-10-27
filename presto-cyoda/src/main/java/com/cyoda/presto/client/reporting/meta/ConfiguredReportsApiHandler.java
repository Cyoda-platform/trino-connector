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

import com.cyoda.api.view.GridConfigFieldsView;
import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.reporting.BasePagingReportsApiHandler;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.logging.SupplierLogger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.trino.spi.StandardErrorCode;
import io.trino.spi.TrinoException;
import io.trino.spi.connector.SchemaTableName;
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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_SCHEMA_NAME_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_TABLE_NAME_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler.HISTORY_FILTER_BY_TYPE_REQUEST_PARAMETER;
import static com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata.REPORTS;

public class ConfiguredReportsApiHandler extends BasePagingReportsApiHandler<ReportListKey, GridConfigFieldsView> {

    protected static final SupplierLogger LOG = SupplierLogger.get(ConfiguredReportsApiHandler.class);

    public static final String REPORT_DEFS_ENDPOINT = "/api/platform-api/reporting/definitions";

    public static final List<String> selectedFields = REPORTS.getFieldList();


    @Inject
    public ConfiguredReportsApiHandler(CyodaConfig config,
                                       RestTemplateCustomizer restTemplateCustomizer,
                                       AuthService authService,
                                       CyodaApiRequestStatsMonitor requestStatsMonitor) {
        super(config, restTemplateCustomizer, LOG, authService, requestStatsMonitor);
    }


    @Override
    public Optional<PagedModel<GridConfigFieldsView>> retrievePage(
            ReportListKey requestKey, int page,
            int pageSize,
            SizeListener listener
    ) {

        int size = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;


//        List<String> columnsWithFilter = Collections.singletonList(REPORT_TYPE_COLUMN);
//        LOG.debug("Columns with Filter: %s",() -> Joiner.on(", ").join(columnsWithFilter));

        UriTemplate uriTemplate = setupUriTemplate();

        ImmutableMap.Builder<String, Object> expansionBuilder = ImmutableMap.<String, Object>builder()
                .put(PAGE_REQUEST_PARAMETER, page)
                .put(SIZE_REQUEST_PARAMETER, size)
                .put(FIELDS_REQUEST_PARAMETER, selectedFields);


        ImmutableMap<String, Object> expansion = expansionBuilder.build();
        URI templatedUri = uriTemplate.expand(expansion);

        Date callTime = new Date();
        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(restTemplateCustomizer.getRestTemplate(requestKey.authContext()));

        TypeReferences.PagedModelType<GridConfigFieldsView> typeReference
                = new TypeReferences.PagedModelType<GridConfigFieldsView>() {
        };
        try {
            final PagedModel<GridConfigFieldsView> gridConfigFieldsViews = traverson
                    .follow()
                    .toObject(typeReference);
            addReportAndTableName(gridConfigFieldsViews);
            publishSize(listener, gridConfigFieldsViews);
            return Optional.ofNullable(gridConfigFieldsViews);
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, e, templatedUri);
        } finally {
            registerApiCall(requestKey.queryId(), callTime, templatedUri.toString(), expansion);
        }
    }

    private void addReportAndTableName(PagedModel<GridConfigFieldsView> gridConfigFieldsViews) {
        if (gridConfigFieldsViews == null) return;
        Collection<GridConfigFieldsView> content = gridConfigFieldsViews.getContent();
        content.forEach(it -> {
            String id = it.getId();
            SchemaTableName tableName = configIdToSchemaTableName(id);
            it.addField(REPORT_SCHEMA_NAME_COLUMN, tableName.getSchemaName());
            it.addField(REPORT_TABLE_NAME_COLUMN, tableName.getTableName());
        });
    }

    private UriTemplate setupUriTemplate() {
        URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(REPORT_DEFS_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new TrinoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, e);
        }
        final ImmutableList.Builder<TemplateVariable> builder = ImmutableList.builder();
        builder.add(
                TemplateVariable.requestParameter(PAGE_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued(SIZE_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued(FIELDS_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued(HISTORY_FILTER_BY_TYPE_REQUEST_PARAMETER)
        );

        TemplateVariables vars = new TemplateVariables(builder.build());
        return UriTemplate.of(uri.toASCIIString())
                .with(vars);
    }



}
