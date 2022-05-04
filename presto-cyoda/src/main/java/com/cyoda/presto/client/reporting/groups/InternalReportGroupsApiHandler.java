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
import com.cyoda.presto.client.PagingApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.jodabeans.StandardColumnDefinition;
import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.logic.PredicateNode;
import com.cyoda.presto.client.paging.PagedIterator;
import com.cyoda.presto.client.reporting.BaseReportsApiHandler;
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.client.reporting.CyodaStaticReportTable;
import com.cyoda.presto.client.reporting.PredicateTraversal;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.service.api.beans.GroupHeader;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.joda.beans.MetaProperty;
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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.types.DataType.STRING;
import static com.cyoda.presto.client.types.DataType.UUID_TYPE;

/**
 * This is not intended to be exposed as a table, but used internally to fill the real table.
 */
class InternalReportGroupsApiHandler extends BaseReportsApiHandler<GroupingHandle>
        implements PagingApiRequestHandler<GroupingHandle> {

    static final String REPORT_ID_COLUMN_NAME = "id";
    static final String GROUPING_VERSION_COLUMN_NAME = "groupingVersion";

    private static final String PARENT_GROUP_COLUMN_NAME = "group_json";
    public static final String REPORT_GROUPS_TEMPLATE = "/{" + REPORT_ID_COLUMN_NAME + "" +
            "}/{" +
            GROUPING_VERSION_COLUMN_NAME + "}/groups/" + "{" + PARENT_GROUP_COLUMN_NAME + "}";

    private static final List<ColumnDefinition> COLUMN_DEFS = StandardColumnDefinition.builder()
            .add(new StandardColumnDefinition(0,REPORT_ID_COLUMN_NAME, StandardTypes.VARCHAR, STRING, null, null))
            .add(new StandardColumnDefinition(0,GROUPING_VERSION_COLUMN_NAME, StandardTypes.VARCHAR, UUID_TYPE, null, null))
            .add(GroupHeader.meta())
            .build();

    @Inject
    public InternalReportGroupsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                          RestTemplateCustomizer restTemplateCustomizer) {
        super(connectorId, config, typeManager,
                REPORT_ENDPOINT, CyodaStaticReportTable.REPORT_GROUPS.name(), COLUMN_DEFS,restTemplateCustomizer);
    }

    @Override
    public String getHandlerKey() {
        return this.getClass().getName();
    }

    @Override
    public Optional<PagedModel<GroupingHandle>> retrievePage(
            int page,
            int pageSize,
            List<CyodaColumnHandle> projectedColumns,
            PredicateNode<Any> predicates
    ) {

        int size = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;

        Collection<PredicateNode<?>> conjunctions = PredicateNode.conjunctions(predicates);
        PredicateTraversal traversal = PredicateTraversal.of(conjunctions);

        UriTemplate uriTemplate = setupUriTemplate();

        ImmutableMap.Builder<String, Object> expansionBuilder = ImmutableMap.<String, Object>builder()
                .put("page", page)
                .put("size", size);


        String reportId = mixinColumn(expansionBuilder,traversal,REPORT_ID_COLUMN_NAME);
        String groupingVersionString = mixinColumn(expansionBuilder,traversal,GROUPING_VERSION_COLUMN_NAME);
        UUID groupingVersion = UUID.fromString(groupingVersionString);

        URI templatedUri = uriTemplate.expand(expansionBuilder.build());

        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(restTemplate);

        TypeReferences.PagedModelType<GroupHeader> typeReference =
                new TypeReferences.PagedModelType<GroupHeader>() {};

        try {
            final PagedModel<GroupHeader> fieldsViews = traverson
                    .follow()
                    .toObject(typeReference);
            return Optional.ofNullable(fieldsViews)
                    .map(item->{
                        List<GroupingHandle> handles = item.getContent().stream()
                                .map(handle -> new GroupingHandle(reportId, groupingVersion, handle))
                                .collect(Collectors.toList());
                        return PagedModel.of(handles,item.getMetadata());
                    });
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, "retrieveCollection", e, templatedUri);
        }
    }

    private String mixinColumn(ImmutableMap.Builder<String, Object> expansionBuilder,
                                PredicateTraversal traversal,String columnName
    ) {
        Optional<Set<String>> values = traversal.assembleFilterings(columnName);
        LOG.debug("selecting values:",()->values.map(it-> String.join(",", it)).orElse("EMPTY"));

        Preconditions.checkArgument(values.isPresent());

        Set<String> theValues = values
                .orElseThrow(()->new IllegalArgumentException("No consistent result found for column " + columnName + " in predicates"));

        if ( theValues.size() > 1 ) throw new IllegalStateException("Predicates should only have one element for " + columnName);
        if (!theValues.isEmpty()) {
            String result = theValues.iterator().next();
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
        return UriTemplate.of(uri.toASCIIString()+ REPORT_GROUPS_TEMPLATE);
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull GroupingHandle field, CyodaColumnHandle columnHandle) {
        if ( REPORT_ID_COLUMN_NAME.equals(columnHandle.getColumnName()) ) {
            return field.reportId;
        }
        if ( GROUPING_VERSION_COLUMN_NAME.equals(columnHandle.getColumnName()) ) {
            return field.groupingVersion;
        }
        MetaProperty<?> metaProperty = field.groupHeader.metaBean().metaPropertyMap().get(columnHandle.getColumnName());
        if ( metaProperty == null ) {
            throw new IllegalArgumentException(columnHandle.getColumnName()+" is not defined on ReportDefinitionsView");
        }
        return field.groupHeader.metaBean().metaProperty(columnHandle.getColumnName()).get(field.groupHeader);
    }

    @Override
    public Iterator<GroupingHandle> getResponseIterator(
            int pageSize,
            List<CyodaColumnHandle> projectedColumns,
            PredicateNode<Any> predicates
    ) {
        return new PagedIterator<>(this, pageSize, projectedColumns, predicates).iterator();
    }



}
