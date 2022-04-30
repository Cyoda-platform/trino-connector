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
import com.cyoda.presto.client.neededatcyoda.ReportHistoryFieldsView;
import com.cyoda.presto.client.paging.PagedIterator;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.VarcharType;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.neededatcyoda.ReportHistoryFieldsView.*;
import static com.cyoda.presto.client.types.DataType.*;

public class ReportHistoryApiHandler extends BaseReportsApiHandler<PagedModel<ReportHistoryFieldsView>,ReportHistoryFieldsView>
        implements PagingApiRequestHandler<ReportHistoryFieldsView> {

    private static final SupplierLogger LOG = SupplierLogger.get(ReportHistoryApiHandler.class);

    enum FieldDef implements FieldDefinition {
        ID(0, REPORT_ID_COLUMN_NAME, StandardTypes.VARCHAR, STRING, null),
        CREATION_DATE(1, CREATE_TIME_COLUMN_NAME, StandardTypes.TIMESTAMP, DATE, null),

        TYPE(2, TYPE_COLUMN_NAME, StandardTypes.VARCHAR, STRING, null),
        STATUS(3, STATUS_NAME_COLUMN_NAME, StandardTypes.VARCHAR, STRING, null),
        HIERARCHY_ENABLE(4, HIERARHY_ENABLE_COLUMN_NAME, StandardTypes.BOOLEAN, BOOLEAN, null),
        // For Trino this can be a UUID, but Presto wants VARCHAR.
        GROUPING_VERSION(5, GROUPING_VERSION_COLUMN_NAME, StandardTypes.VARCHAR, UUID_TYPE, null),
        GROUPING_COLUMNS(6, GROUPING_COLUMNS_COLUMN_NAME, StandardTypes.ARRAY, LIST, VarcharType.VARCHAR),
        USER_NAME(6,USER_NAME_COLUMN_NAME, StandardTypes.VARCHAR,STRING,null);


        private final int pos;
        private final String fieldName;
        private final String fieldTypeString;
        private final DataType dataType;
        private final Type parType;

        FieldDef(int pos, String fieldName, String fieldTypeString, DataType dateType, Type parType) {
            this.pos = pos;
            this.fieldName = fieldName;
            this.fieldTypeString = fieldTypeString;
            this.dataType = dateType;
            this.parType = parType;
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
            return parType;
        }
    }

    public static final List<String> selectedFields = ImmutableList.copyOf(
            Arrays.stream(FieldDef.values()).map(ReportHistoryApiHandler.FieldDef::getFieldName).collect(Collectors.toList())
    );

    @Inject
    public ReportHistoryApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager) {
        super(connectorId, config, typeManager, REPORT_HISTORY_ENDPOINT, CyodaStaticReportTable.REPORT_HISTORIES.name(),
                FieldDef.values());
    }


    @Override
    public String getHandlerKey() {
        return CyodaStaticReportTable.REPORT_HISTORIES.name();
    }


    @Override
    public Optional<PagedModel<ReportHistoryFieldsView>> retrievePage(
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
                .put("size", size)
                .put("fields", selectedFields);


        // Filter criteria: filterByType, user (single), creationdate range.
        Optional<Set<String>> filterByType = traversal.assembleFilterings(GridConfigFieldsView.TYPE_COLUMN_NAME);
        LOG.debug("selecting by types:",()->filterByType.map(it-> String.join(",", it)));

        if (filterByType.isPresent() && !filterByType.get().isEmpty()) {
            expansionBuilder.put("filterByType", filterByType.get());
        }


        URI templatedUri = uriTemplate.expand(expansionBuilder.build());

        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(restTemplate);

        TypeReferences.PagedModelType<ReportHistoryFieldsView> typeReference =
                new TypeReferences.PagedModelType<ReportHistoryFieldsView>() {
        };

        try {
            final PagedModel<ReportHistoryFieldsView> fieldsViews = traverson
                    .follow()
                    .toObject(typeReference);
            return Optional.ofNullable(fieldsViews);
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, "retrieveCollection", e, templatedUri);
        }
    }

    private UriTemplate setupUriTemplate() {

        URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(REPORT_HISTORY_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, e);
        }
        final ImmutableList.Builder<TemplateVariable> builder = ImmutableList.builder();
        builder.add(
                TemplateVariable.requestParameter("page"),
                TemplateVariable.requestParameterContinued("size"),
                TemplateVariable.requestParameterContinued("fields"),
                TemplateVariable.requestParameterContinued("filterByType"),
                TemplateVariable.requestParameterContinued("username"),
                TemplateVariable.requestParameterContinued("report_name"),
                TemplateVariable.requestParameterContinued("from"),
                TemplateVariable.requestParameterContinued("to")
        );

        TemplateVariables vars = new TemplateVariables(builder.build());
        return UriTemplate.of(uri.toASCIIString())
                .with(vars);
    }

    @Override
    protected @Nullable Object getFieldValueFromEntity(@Nonnull ReportHistoryFieldsView entity, CyodaColumnHandle columnHandle) {
        Map<String, Object> fields = entity.getReportHistoryFields();
        return fields.get(columnHandle.getColumnName());
    }

    @Override
    protected @Nonnull Object mapFieldValue(@Nonnull Object value, CyodaColumnHandle columnHandle) {
        if (columnHandle.getDataType() == DATE) {
            return toDate((String) value);
        }
        if (columnHandle.getDataType() == UUID_TYPE) {
            return UUID.fromString((String) value);
        }
        return value;
    }



    private Date toDate(String str) {
        if (str == null) return null;
        LocalDateTime localDateTime = LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
        return Timestamp.valueOf(localDateTime);
    }

    @Override
    public Iterator<ReportHistoryFieldsView> getResponseIterator(
            int pageSize,
            CyodaTableHandle cyodaTableHandle,
            PredicateNode<Any> predicates
    ) {
        return new PagedIterator<>(this, pageSize, cyodaTableHandle, predicates).iterator();
    }

}
