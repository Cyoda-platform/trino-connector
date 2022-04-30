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

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.client.PagingApiRequestHandler;
import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.logic.PredicateNode;
import com.cyoda.presto.client.neededatcyoda.GridConfigFieldsView;
import com.cyoda.presto.client.paging.PagedIterator;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.server.core.TypeReferences.PagedModelType;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import static com.cyoda.presto.client.neededatcyoda.GridConfigFieldsView.ID_COLUMN_NAME;
import static com.cyoda.presto.client.neededatcyoda.GridConfigFieldsView.NAME_COLUMN_NAME;
import static com.cyoda.presto.client.reporting.CyodaStaticReportTable.REPORTS;
import static com.cyoda.presto.client.types.DataType.LOCAL_DATE_TIME;
import static com.cyoda.presto.client.types.DataType.STRING;

public class ConfiguredReportsApiHandler extends BaseReportsApiHandler<PagedModel<GridConfigFieldsView>,GridConfigFieldsView>
        implements PagingApiRequestHandler<GridConfigFieldsView> {

    protected static final SupplierLogger LOG = SupplierLogger.get(ConfiguredReportsApiHandler.class);


    enum FieldDef implements FieldDefinition {
        ID              (0, ID_COLUMN_NAME, StandardTypes.VARCHAR,STRING),
        NAME            (1, NAME_COLUMN_NAME,StandardTypes.VARCHAR, STRING),
        DESCRIPTION     (1, GridConfigFieldsView.DESCRIPTION_COLUMN_NAME,StandardTypes.VARCHAR,STRING),
        TYPE            (2, GridConfigFieldsView.TYPE_COLUMN_NAME,StandardTypes.VARCHAR,STRING),
        USER_ID         (3, GridConfigFieldsView.USER_ID_COLUMN_NAME,StandardTypes.VARCHAR,STRING),
        CREATION_DATE   (4, GridConfigFieldsView.CREATION_DATE_COLUMN_NAME,StandardTypes.TIMESTAMP, LOCAL_DATE_TIME);

        private final int pos;
        private final String fieldName;
        private final String fieldTypeString;
        private final DataType dataType;

        FieldDef(int pos, String fieldName, String fieldTypeString, DataType dateType) {
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
        public Type getParType() {
            return null;
        }

    }
    public static final List<String> selectedFields = ImmutableList.copyOf(
            Arrays.stream(FieldDef.values()).map(FieldDef::getFieldName).collect(Collectors.toList())
    );


    @Inject
    public ConfiguredReportsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager) {
        super(connectorId, config, typeManager,
                REPORT_DEFS_ENDPOINT, REPORTS.name(),FieldDef.values());
    }

    @Override
    public String getHandlerKey() {
        return REPORTS.name();
    }


    @Override
    public Optional<PagedModel<GridConfigFieldsView>> retrievePage(
            int page,
            int pageSize,
            List<CyodaColumnHandle> projectedColumns, PredicateNode<Any> predicates) {

        int size = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;


        Collection<PredicateNode<?>> conjunctions = PredicateNode.conjunctions(predicates);
        PredicateTraversal traversal = PredicateTraversal.of(conjunctions);

        List<String> columnsWithFilter = Collections.singletonList(GridConfigFieldsView.TYPE_COLUMN_NAME);
        LOG.debug("Columns with Filter: %s",() -> Joiner.on(", ").join(columnsWithFilter));

        UriTemplate uriTemplate = setupUriTemplate();

         ImmutableMap.Builder<String, Object> expansionBuilder = ImmutableMap.<String, Object>builder()
                .put("page", page)
                .put("size", size)
                .put("fields", selectedFields);

        Optional<Set<String>> filterByType = traversal.assembleFilterings(GridConfigFieldsView.TYPE_COLUMN_NAME);

        if (filterByType.isPresent() && !filterByType.get().isEmpty()) {
            expansionBuilder.put("filterByType", filterByType.get());
        }

        URI templatedUri = uriTemplate.expand(expansionBuilder.build());

        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(restTemplate);

        PagedModelType<GridConfigFieldsView> typeReference = new PagedModelType<GridConfigFieldsView>() {
        };

        try {
            final PagedModel<GridConfigFieldsView> gridConfigFieldsViews = traverson
                    .follow()
                    .toObject(typeReference);
            addReportName(gridConfigFieldsViews);
            return Optional.ofNullable(gridConfigFieldsViews);
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, "retrieveCollection", e, templatedUri);
        }
    }

    private void addReportName(PagedModel<GridConfigFieldsView> gridConfigFieldsViews) {
        if ( gridConfigFieldsViews == null ) return;
        Collection<GridConfigFieldsView> content = gridConfigFieldsViews.getContent();
        content.forEach( it -> {
            String id = it.getGridConfigFields().get(ID_COLUMN_NAME);
            String repName = id.replaceFirst("^(.+?)([^-]+)$","$2");
            it.addField(NAME_COLUMN_NAME,repName);
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
                TemplateVariable.requestParameter("page"),
                TemplateVariable.requestParameterContinued("size"),
                TemplateVariable.requestParameterContinued("fields"),
                TemplateVariable.requestParameterContinued("filterByType")
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
    public Iterator<GridConfigFieldsView> getResponseIterator(int pageSize,
                                                              CyodaTableHandle cyodaTableHandle, PredicateNode<Any> predicates) {
        return new PagedIterator<>(this, pageSize, cyodaTableHandle, predicates).iterator();
    }


    private LocalDateTime toLocalDateTime(String str) {
        if (str == null) return null;
        return LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
    }


}
