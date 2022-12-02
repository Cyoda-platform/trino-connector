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
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.PagingApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.reporting.BasePagingReportsApiHandler;
import com.cyoda.presto.client.reporting.PredicateTraversal;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportMetadataProvider;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.logging.SupplierLogger;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_NAME_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_TABLE_NAME_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler.HISTORY_FILTER_BY_TYPE_REQUEST_PARAMETER;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportTable.REPORTS;

public class ConfiguredReportsApiHandler extends BasePagingReportsApiHandler<GridConfigFieldsView>
        implements PagingApiRequestHandler<GridConfigFieldsView> {

    protected static final SupplierLogger LOG = SupplierLogger.get(ConfiguredReportsApiHandler.class);

    public static final String REPORT_DEFS_ENDPOINT = "/api/platform-api/reporting/definitions";
    private final CyodaColumnHandle typeColumn;


    public static final List<String> selectedFields = REPORTS.getFieldList();


    @Inject
    public ConfiguredReportsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                       RestTemplateCustomizer restTemplateCustomizer,
                                       StaticReportMetadataProvider staticReportMetadataProvider) {
        super(connectorId, config, typeManager, restTemplateCustomizer, LOG);
        this.typeColumn = staticReportMetadataProvider.getReports().getTypeColumn();

    }


    @Override
    public Optional<PagedModel<GridConfigFieldsView>> retrievePage(
            AuthContext authContext,
            int page,
            int pageSize,
            CompoundPredicateNode predicates,
            SizeListener listener
    ) {

        int size = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;


        PredicateTraversal<String> traversal = PredicateTraversal.of(predicates, String.class);

//        List<String> columnsWithFilter = Collections.singletonList(REPORT_TYPE_COLUMN);
//        LOG.debug("Columns with Filter: %s",() -> Joiner.on(", ").join(columnsWithFilter));

        UriTemplate uriTemplate = setupUriTemplate();

        ImmutableMap.Builder<String, Object> expansionBuilder = ImmutableMap.<String, Object>builder()
                .put(PAGE_REQUEST_PARAMETER, page)
                .put(SIZE_REQUEST_PARAMETER, size)
                .put(FIELDS_REQUEST_PARAMETER, selectedFields);

        Optional<SortedSet<String>> filterByType = traversal.assembleEqualsPredicateValuesFromAnd(this.typeColumn);

        if (!filterByType.isPresent()) return Optional.empty();

        if (!filterByType.get().isEmpty()) {
            expansionBuilder.put(HISTORY_FILTER_BY_TYPE_REQUEST_PARAMETER, filterByType.get());
        }

        URI templatedUri = uriTemplate.expand(expansionBuilder.build());

        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(restTemplateCustomizer.getRestTemplate(authContext));

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
            throw requestFailedException(this, "retrieveCollection", e, templatedUri);
        }
    }

    private void addReportAndTableName(PagedModel<GridConfigFieldsView> gridConfigFieldsViews) {
        if (gridConfigFieldsViews == null) return;
        Collection<GridConfigFieldsView> content = gridConfigFieldsViews.getContent();
        content.forEach(it -> {
            String id = it.getGridConfigFields().get("id");
            String repName = toReportName(id);
            it.addField(REPORT_NAME_COLUMN, repName);
            String tableName = reportNameToTableName(id).toLowerCase();
            it.addField(REPORT_TABLE_NAME_COLUMN, tableName);
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


    @Override
    protected @Nullable Object getFieldValueFromEntity(@Nonnull GridConfigFieldsView entity, CyodaColumnHandle columnHandle) {
        Map<String, String> fields = entity.getGridConfigFields();
        return fields.get(columnHandle.getColumnName());
    }

}
