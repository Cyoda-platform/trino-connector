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
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.PagingApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.jodabeans.StandardColumnDefinition;
import com.cyoda.presto.client.logic.ColumnPredicate;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.reporting.BasePagingReportsApiHandler;
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.client.reporting.PredicateTraversal;
import com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.cyoda.service.api.beans.GroupHeader;
import com.cyoda.service.api.beans.ReportRow;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.reporting.AbstractTableHolder.TableDefinitionHandle.asTableDefinitionHandle;
import static com.cyoda.presto.client.reporting.CyodaStaticReportTable.REPORT_ROWS;
import static com.cyoda.presto.client.reporting.data.ReportRowsApiHandler.*;
import static com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler.GROUPING_REPORT_CONFIG_ID_COLUMN;
import static com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler.GROUPING_VERSION_COLUMN;
import static com.cyoda.presto.client.types.DataType.LONG;
import static com.cyoda.presto.client.types.DataType.STRING;

public class InternalReportRowsApiHandler extends BasePagingReportsApiHandler<RowHandle>
        implements PagingApiRequestHandler<RowHandle> {

    protected static final SupplierLogger LOG = SupplierLogger.get(InternalReportRowsApiHandler.class);

    static final String REPORT_ROWS_TEMPLATE = "/{" + ROW_REPORT_ID_COLUMN + "" +
            "}/group_rows/{" +
            ROW_GROUP_JSON_BASE64_VARIABLE + "}";

    private static final List<ColumnDefinition> COLUMN_DEFS = StandardColumnDefinition.builder()
            // TODO: Needs to be a Long
            .add(new StandardColumnDefinition(0, ROW_REPORT_ROW_NUMBER_COLUMN, StandardTypes.BIGINT,LONG,null,null))
            .add(new StandardColumnDefinition(0, ROW_REPORT_ID_COLUMN, StandardTypes.VARCHAR, STRING, null, null))
            .add(new StandardColumnDefinition(0, ROW_GROUP_JSON_BASE64_VARIABLE, StandardTypes.VARCHAR, STRING, null, null))
            .add(GroupHeader.meta())
            .build();
    private final CyodaColumnHandle rowNumberColumn;
    private final CyodaColumnHandle rowIdColumn;
    private final CyodaColumnHandle groupingVersionColumn;
    private final CyodaColumnHandle groupJsonBase64Column;

    @Inject
    public InternalReportRowsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                          RestTemplateCustomizer restTemplateCustomizer) {
        super(connectorId, config, typeManager, REPORT_ENDPOINT,restTemplateCustomizer,LOG);
        this.rowNumberColumn = getColumnByName(ROW_REPORT_ROW_NUMBER_COLUMN);
        this.rowIdColumn = getColumnByName(ROW_REPORT_ID_COLUMN);
        this.groupJsonBase64Column = getColumnByName(ROW_GROUP_JSON_BASE64_VARIABLE);

        this.groupingVersionColumn = getColumnByName(ReportGroupsApiHandler.COLUMN_DEFS,GROUPING_VERSION_COLUMN);

    }

    private CyodaColumnHandle getColumnByName(String columnName) {
        return createColumnHandle(COLUMN_DEFS.stream()
                .filter(it -> it.getFieldName().equals(columnName))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Should not happen")));
    }

    @Override
    protected Map<TableDefinitionHandle, List<ColumnDefinition>> refreshFieldDefs(AuthContext authContext) {
        return Collections.singletonMap(asTableDefinitionHandle(REPORT_ROWS.name()), COLUMN_DEFS);
    }

    @Override
    public String getHandlerKey() {
        return this.getClass().getName();
    }

    @Override
    public Optional<PagedModel<RowHandle>> retrievePage(
            AuthContext authContext,
            int page,
            int pageSize,
            List<CyodaColumnHandle> projectedColumns,
            CompoundPredicateNode predicates,
            SizeListener listener
    ) {

        int size = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;

        PredicateTraversal<Long> longPredicateTraversal = PredicateTraversal.of(predicates,Long.class);
        PredicateTraversal<String> stringPredicateTraversal = PredicateTraversal.of(predicates,String.class);
        PredicateTraversal<UUID> uuidPredicateTraversal = PredicateTraversal.of(predicates,UUID.class);


        UriTemplate uriTemplate = setupUriTemplate();

        Set<ColumnPredicate<Long>> columnPredicates = longPredicateTraversal.parseFor(this.rowNumberColumn);

        Preconditions.checkArgument(!columnPredicates.isEmpty(),"Bug in Traversal");

        return columnPredicates.stream().flatMap(it->RowNumHandle.from(it,page,size).stream())
                .map( it-> exchange(
                        authContext,
                        page,
                        pageSize,
                        listener,
                        stringPredicateTraversal,
                        uuidPredicateTraversal,
                        uriTemplate,
                        it)
                ).reduce(Optional.of(PagedModel.empty()),(result,rowHandles) ->
                        Optional.of(merge(result.get(),rowHandles.orElse(null)))
                );
    }

    private @Nonnull PagedModel<RowHandle> merge(@Nonnull PagedModel<RowHandle> result, @Nullable  PagedModel<RowHandle> response) {
        if ( response == null ) return result;
        ImmutableList.Builder<RowHandle> builder = ImmutableList.builder();
        builder.addAll(result.getContent());
        builder.addAll(response.getContent());
        Collection<RowHandle> content = builder.build();

        PagedModel.PageMetadata resultMetadata = result.getMetadata();
        PagedModel.PageMetadata responseMetadata = Optional.ofNullable(response.getMetadata()).orElseThrow(
                ()->new IllegalArgumentException("No pageMeta attached to response. Cannot continue")
        );

        long totalElements = Optional.ofNullable(resultMetadata).map(PagedModel.PageMetadata::getTotalElements).orElse(0L)
                + responseMetadata.getTotalElements();
        PagedModel.PageMetadata meta = new PagedModel.PageMetadata(
                Optional.ofNullable(resultMetadata).map(PagedModel.PageMetadata::getSize).orElse(responseMetadata.getSize()),
                Optional.ofNullable(resultMetadata).map(PagedModel.PageMetadata::getNumber).orElse(responseMetadata.getNumber()),
                totalElements);
        return PagedModel.of(content,meta);
    }

    private Optional<PagedModel<RowHandle>> exchange(AuthContext authContext, int page, int pageSize, SizeListener listener, PredicateTraversal<String> stringPredicateTraversal, PredicateTraversal<UUID> uuidPredicateTraversal, UriTemplate uriTemplate, RowNumHandle rowNumHandle) {
        ImmutableMap.Builder<String, Object> expansionBuilder = ImmutableMap.<String, Object>builder()
                .put(PAGE_REQUEST_PARAMETER, rowNumHandle.page)
                .put(SIZE_REQUEST_PARAMETER, rowNumHandle.size);


        String reportId = mixinColumn(expansionBuilder, stringPredicateTraversal, this.rowIdColumn);
        UUID groupingVersion = mixinColumn(expansionBuilder, uuidPredicateTraversal, this.groupingVersionColumn);
        String groupJsonString = mixinColumn(expansionBuilder, stringPredicateTraversal, this.groupJsonBase64Column);

        URI templatedUri = uriTemplate.expand(expansionBuilder.build());

        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(restTemplateCustomizer.getRestTemplate(authContext));

        TypeReferences.PagedModelType<ReportRow> typeReference =
                new TypeReferences.PagedModelType<ReportRow>() {};

        try {
            final PagedModel<ReportRow> fieldsViews = traverson
                    .follow()
                    .toObject(typeReference);
            publishSize(listener,fieldsViews);
            return Optional.ofNullable(fieldsViews)
                    .map(item -> {
                        AtomicLong rowNum = new AtomicLong(rowNumHandle.offset);
                        List<RowHandle> handles = item.getContent().stream()
                                .map(reportRow -> new RowHandle(reportId, groupingVersion, groupJsonString, reportRow, rowNum.incrementAndGet()))
                                .filter(reportRow -> rowNumHandle.isInRowWindow(reportRow.rowNum))
                                .collect(Collectors.toList());
                        PagedModel.PageMetadata apiMeta = Optional.ofNullable(fieldsViews.getMetadata()).orElseThrow(() -> new IllegalStateException("No meta attached"));
                        PagedModel.PageMetadata metadata = rowNumHandle.createPageMeta(page, pageSize, item, apiMeta);
                        return PagedModel.of(handles, metadata);
                    });
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, "retrieveCollection", e, templatedUri);
        }
    }


    private <T extends Comparable<? super T>> T mixinColumn(ImmutableMap.Builder<String, Object> expansionBuilder,
                               PredicateTraversal<T> traversal,CyodaColumnHandle columnHandle
    ) {
        String columnName = columnHandle.getColumnName();
        Optional<SortedSet<T>> values = traversal.assembleEqualsPredicateValuesFromAnd(columnHandle);
        LOG.debug("selecting values for %s : %s",()->columnName, ()->values.map(it-> String.join(",", it.toString())).orElse("EMPTY"));

        Preconditions.checkArgument(values.isPresent());

        Set<T> theValues = values
                .orElseThrow(()->new IllegalArgumentException("No consistent result found for column " + columnName + " in predicates"));

        if ( theValues.size() > 1 ) throw new IllegalStateException("Predicates should only have one element for " + columnName);
        if (!theValues.isEmpty()) {
            T result = theValues.iterator().next();
            expansionBuilder.put(columnName, result);
            return result;
        }
        throw new IllegalArgumentException("no result found for column " + columnName + " in predicates");
    }
    private UriTemplate setupUriTemplate() {

        URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(REPORT_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, e);
        }
        final ImmutableList.Builder<TemplateVariable> builder = ImmutableList.builder();
        builder.add(
                TemplateVariable.requestParameter(PAGE_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued(SIZE_REQUEST_PARAMETER)
        );

        TemplateVariables vars = new TemplateVariables(builder.build());
        return UriTemplate.of(uri.toASCIIString()+ REPORT_ROWS_TEMPLATE)
                .with(vars);
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull RowHandle field, CyodaColumnHandle columnHandle) {
        if ( ROW_REPORT_ROW_NUMBER_COLUMN.equals(columnHandle.getColumnName()) ) {
            return field.rowNum;
        }
        if ( ROW_REPORT_ID_COLUMN.equals(columnHandle.getColumnName()) ) {
            return field.reportId;
        }
        if ( ROW_GROUPING_VERSION_COLUMN.equals(columnHandle.getColumnName()) ) {
            return field.groupingVersion;
        }
        if ( ROW_GROUP_JSON_BASE64_VARIABLE.equals(columnHandle.getColumnName()) ) {
            return field.groupJsonBase64;
        }
        return field.reportRow.get(columnHandle.getColumnName());
    }



}

