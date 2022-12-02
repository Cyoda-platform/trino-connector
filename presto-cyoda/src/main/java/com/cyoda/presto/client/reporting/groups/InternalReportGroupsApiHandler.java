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
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.PagingApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.reporting.BasePagingReportsApiHandler;
import com.cyoda.presto.client.reporting.PredicateTraversal;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportMetadataProvider;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.cyoda.service.api.beans.GroupHeader;
import com.cyoda.service.interactors.WrappedEntityModel;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.trino.spi.StandardErrorCode;
import io.trino.spi.TrinoException;
import io.trino.spi.type.TypeManager;
import org.joda.beans.MetaProperty;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler.GROUPING_PARENT_COLUMN;
import static com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler.GROUPING_VERSION_COLUMN;

/**
 * This is not intended to be exposed as a table, but used internally to fill the real table.
 */
public class InternalReportGroupsApiHandler extends BasePagingReportsApiHandler<GroupingHandle>
        implements PagingApiRequestHandler<GroupingHandle> {

    protected static final SupplierLogger LOG = SupplierLogger.get(InternalReportGroupsApiHandler.class);

    public static final String REPORT_GROUPS_TEMPLATE = "/{" + HISTORY_REPORT_ID_COLUMN + "" +
            "}/{" +
            GROUPING_VERSION_COLUMN + "}/groups/" + "{" + GROUPING_PARENT_COLUMN + "}";

    public static final String IN_PREDICATES = " in predicates";
    public static final String NO_RESULT_FOUND_FOR_COLUMN = "no result found for column ";
    private final CyodaColumnHandle groupingVersionColumn;
    private final CyodaColumnHandle reportNameColumn;
    private final CyodaColumnHandle reportIdColumn;

    @Inject
    public InternalReportGroupsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                          RestTemplateCustomizer restTemplateCustomizer,
                                          StaticReportMetadataProvider staticMetaProvider) {
        super(connectorId, config, typeManager, restTemplateCustomizer, LOG);
        this.groupingVersionColumn = staticMetaProvider.getReportGroups().getGroupingVersionColumn();
        this.reportIdColumn = staticMetaProvider.getReportGroups().getReportIdColumn();
        // An External Column
        this.reportNameColumn = staticMetaProvider.getReportGroups().getReportConfigIdColumn();

    }

    @Override
    public Optional<PagedModel<GroupingHandle>> retrievePage(
            AuthContext authContext,
            int page,
            int pageSize,
            CompoundPredicateNode predicates,
            SizeListener listener
    ) {

        int size = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;

        PredicateTraversal<String> stringPredicateTraversal = PredicateTraversal.of(predicates, String.class);
        PredicateTraversal<UUID> uuidPredicateTraversal = PredicateTraversal.of(predicates, UUID.class);

        UriTemplate uriTemplate = setupUriTemplate();

        ImmutableMap.Builder<String, Object> expansionBuilder = ImmutableMap.<String, Object>builder()
                .put(PAGE_REQUEST_PARAMETER, page)
                .put(SIZE_REQUEST_PARAMETER, size);


        String reportId = this.mixinColumn(expansionBuilder, stringPredicateTraversal, this.reportIdColumn)
                .orElseThrow(() -> new IllegalArgumentException(NO_RESULT_FOUND_FOR_COLUMN + HISTORY_REPORT_ID_COLUMN + IN_PREDICATES));
        UUID groupingVersion = this.mixinColumn(expansionBuilder, uuidPredicateTraversal, this.groupingVersionColumn)
                .orElseThrow(() -> new IllegalArgumentException(NO_RESULT_FOUND_FOR_COLUMN + GROUPING_VERSION_COLUMN + IN_PREDICATES));
        String reportConfigName = this.<String>mixinColumn(expansionBuilder, stringPredicateTraversal, this.reportNameColumn).orElse(null);

        URI templatedUri = uriTemplate.expand(expansionBuilder.build());

        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(restTemplateCustomizer.getRestTemplate(authContext));

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
                                .map(handle -> new GroupingHandle(reportId, groupingVersion, handle.getContent(), reportConfigName))
                                .collect(Collectors.toList());
                        return PagedModel.of(handles, item.getMetadata());
                    });
            publishSize(listener, groupingHandles.orElse(PagedModel.empty()));
            return groupingHandles;
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, "retrieveCollection", e, templatedUri);
        }
    }

    private <T extends Comparable<? super T>> Optional<T> mixinColumn(ImmutableMap.Builder<String, Object> expansionBuilder,
                                                                      PredicateTraversal<T> traversal, CyodaColumnHandle columnHandle
    ) {
        String columnName = columnHandle.getColumnName();
        Optional<SortedSet<T>> values = traversal.assembleEqualsPredicateValuesFromAnd(columnHandle);
        LOG.debug("selecting values for %s : %s", () -> columnName, () -> values.map(it -> String.join(",", it.toString())).orElse("EMPTY"));

        Preconditions.checkArgument(values.isPresent());

        Set<T> theValues = values
                .orElseThrow(() -> new IllegalArgumentException("No consistent result found for column " + columnName + IN_PREDICATES));

        if (theValues.size() > 1)
            throw new IllegalStateException("Predicates should only have one element for " + columnName);
        if (!theValues.isEmpty()) {
            T result = theValues.iterator().next();
            expansionBuilder.put(columnName, result);
            return Optional.of(result);
        }
        return Optional.empty();
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

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull GroupingHandle field, CyodaColumnHandle columnHandle) {
        if (HISTORY_REPORT_ID_COLUMN.equals(columnHandle.getColumnName())) {
            return field.reportId;
        }
        if (GROUPING_VERSION_COLUMN.equals(columnHandle.getColumnName())) {
            return field.groupingVersion;
        }
        MetaProperty<?> metaProperty = field.groupHeader.metaBean().metaPropertyMap().get(columnHandle.getColumnName());
        if (metaProperty == null) {
            throw new IllegalArgumentException(columnHandle.getColumnName() + " is not defined on ReportDefinitionsView");
        }
        return field.groupHeader.metaBean().metaProperty(columnHandle.getColumnName()).get(field.groupHeader);
    }

}
