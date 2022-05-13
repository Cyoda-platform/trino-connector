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
import com.cyoda.presto.client.PagingApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.logic.ColumnPredicateNode;
import com.cyoda.presto.client.paging.PagedIterator;
import com.cyoda.presto.client.reporting.BaseReportsApiHandler;
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.client.reporting.PredicateTraversal;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.TypeSignature;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.reporting.AbstractTableHolder.TableDefinitionHandle.asTableDefinitionHandle;
import static com.cyoda.presto.client.reporting.CyodaStaticReportTable.REPORTS;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.*;
import static com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler.HISTORY_FILTER_BY_TYPE_REQUEST_PARAMETER;
import static com.cyoda.presto.client.types.DataType.LOCAL_DATE_TIME;
import static com.cyoda.presto.client.types.DataType.STRING;

public class ConfiguredReportsApiHandler extends BaseReportsApiHandler<GridConfigFieldsView>
        implements PagingApiRequestHandler<GridConfigFieldsView> {

    protected static final SupplierLogger LOG = SupplierLogger.get(ConfiguredReportsApiHandler.class);

    public static final String REPORT_DEFS_ENDPOINT = "/api/platform-api/reporting/definitions";

    enum ColumnDef implements ColumnDefinition {
        ID              (0, REPORT_ID_COLUMN, StandardTypes.VARCHAR,STRING),
        NAME            (1, REPORT_NAME_COLUMN,StandardTypes.VARCHAR, STRING),
        TABLE_NAME      (2, REPORT_TABLE_NAME_COLUMN,StandardTypes.VARCHAR, STRING),
        DESCRIPTION     (3, REPORT_DESCRIPTION_COLUMN,StandardTypes.VARCHAR,STRING),
        TYPE            (4, REPORT_TYPE_COLUMN,StandardTypes.VARCHAR,STRING),
        USER_ID         (5, REPORT_USER_ID_COLUMN,StandardTypes.VARCHAR,STRING),
        CREATION_DATE   (6, REPORT_CREATION_DATE_COLUMN,StandardTypes.TIMESTAMP, LOCAL_DATE_TIME);

        private final int pos;
        private final String fieldName;
        private final String fieldTypeString;
        private final DataType dataType;

        ColumnDef(int pos, String fieldName, String fieldTypeString, DataType dateType) {
            this.pos = pos;
            this.fieldName = fieldName;
            this.fieldTypeString = fieldTypeString;
            this.dataType = dateType;
        }

        @Override
        public int getPos() {
            return pos;
        }

        @Override
        public String getFieldName() {
            return fieldName;
        }

        @Override
        public String getFieldTypeString() {
            return fieldTypeString;
        }

        @Override
        public DataType getDataType() {
            return dataType;
        }

        @Override
        public TypeSignature getParType() {
            return null;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("pos", pos)
                    .add("fieldName", fieldName)
                    .add("fieldTypeString", fieldTypeString)
                    .add("dataType", dataType)
                    .toString();
        }
    }
    public static final List<String> selectedFields = ImmutableList.copyOf(
            Arrays.stream(ColumnDef.values()).map(ColumnDef::getFieldName).collect(Collectors.toList())
    );


    @Inject
    public ConfiguredReportsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                       RestTemplateCustomizer restTemplateCustomizer) {
        super(connectorId, config, typeManager, REPORT_DEFS_ENDPOINT,restTemplateCustomizer);
    }

    @Override
    protected Map<TableDefinitionHandle, List<ColumnDefinition>> refreshFieldDefs() {
        return Collections.singletonMap(asTableDefinitionHandle(REPORTS.name()),ImmutableList.copyOf(ColumnDef.values()));
    }

    @Override
    public String getHandlerKey() {
        return REPORTS.name();
    }


    @Override
    public Optional<PagedModel<GridConfigFieldsView>> retrievePage(
            int page,
            int pageSize,
            List<CyodaColumnHandle> projectedColumns, ColumnPredicateNode<Any> predicates) {

        int size = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;


        Collection<ColumnPredicateNode<?>> conjunctions = ColumnPredicateNode.conjunctions(predicates);
        PredicateTraversal traversal = PredicateTraversal.of(conjunctions);

        List<String> columnsWithFilter = Collections.singletonList(REPORT_TYPE_COLUMN);
        LOG.debug("Columns with Filter: %s",() -> Joiner.on(", ").join(columnsWithFilter));

        UriTemplate uriTemplate = setupUriTemplate();

         ImmutableMap.Builder<String, Object> expansionBuilder = ImmutableMap.<String, Object>builder()
                .put(PAGE_REQUEST_PARAMETER, page)
                .put(SIZE_REQUEST_PARAMETER, size)
                .put(FIELDS_REQUEST_PARAMETER, selectedFields);

        Optional<Set<String>> filterByType = traversal.assembleFilterings(REPORT_TYPE_COLUMN);

        if (!filterByType.isPresent()) return Optional.empty();

        if (!filterByType.get().isEmpty()) {
            expansionBuilder.put(HISTORY_FILTER_BY_TYPE_REQUEST_PARAMETER, filterByType.get());
        }

        URI templatedUri = uriTemplate.expand(expansionBuilder.build());

        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(restTemplate);

        TypeReferences.PagedModelType<GridConfigFieldsView> typeReference
                = new TypeReferences.PagedModelType<GridConfigFieldsView>() {};
        try {
            final PagedModel<GridConfigFieldsView> gridConfigFieldsViews = traverson
                    .follow()
                    .toObject(typeReference);
            addReportAndTableName(gridConfigFieldsViews);
            return Optional.ofNullable(gridConfigFieldsViews);
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, "retrieveCollection", e, templatedUri);
        }
    }

    private void addReportAndTableName(PagedModel<GridConfigFieldsView> gridConfigFieldsViews) {
        if ( gridConfigFieldsViews == null ) return;
        Collection<GridConfigFieldsView> content = gridConfigFieldsViews.getContent();
        content.forEach( it -> {
            String id = it.getGridConfigFields().get("id");
            String repName = toReportName(id);
            it.addField(REPORT_NAME_COLUMN,repName);
            String tableName = reportNameToTableName(repName).toLowerCase();
            it.addField(REPORT_TABLE_NAME_COLUMN,tableName);
        });
    }

    private UriTemplate setupUriTemplate() {
        URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(REPORT_DEFS_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, e);
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

    @Override
    protected @Nonnull Object mapFieldValue(@Nonnull Object field, CyodaColumnHandle columnHandle) {
        if (columnHandle.getDataType() == LOCAL_DATE_TIME) {
            return toLocalDateTime((String) field);
        }
        return field;
    }

    @Override
    public Iterator<GridConfigFieldsView> getResponseIterator(
            int pageSize,
            CyodaTableHandle tableHandle,
            ColumnPredicateNode<Any> predicates
    ) {
        List<CyodaColumnHandle> projectedColumns = tableHandle.getProjectedColumns().orElse(Collections.emptyList());
        return new PagedIterator<>(this, pageSize, projectedColumns, predicates).iterator();
    }


}
