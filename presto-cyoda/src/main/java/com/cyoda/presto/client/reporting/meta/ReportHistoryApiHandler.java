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

import com.cyoda.core.model.reports.ReportHistoryFieldsView;
import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.client.PagingApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
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
import com.facebook.presto.common.type.VarcharType;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cyoda.core.model.reports.ReportHistoryFieldsView.*;
import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.reporting.AbstractTableHolder.TableDefinitionHandle.asTableDefinitionHandle;
import static com.cyoda.presto.client.reporting.CyodaStaticReportTable.REPORT_HISTORIES;
import static com.cyoda.presto.client.types.DataType.*;

public class ReportHistoryApiHandler extends BaseReportsApiHandler<ReportHistoryFieldsView>
        implements PagingApiRequestHandler<ReportHistoryFieldsView> {

    private static final SupplierLogger LOG = SupplierLogger.get(ReportHistoryApiHandler.class);

    public static final String REPORT_HISTORY_ENDPOINT = "/api/platform-api/reporting/history";
    public static final String HISTORY_REPORT_NAME_REQUEST_PARAMETER = "report_name";
    public static final String HISTORY_REPORT_NAMES_REQUEST_PARAMETER = "report_names";
    public static final String HISTORY_FILTER_BY_TYPE_REQUEST_PARAMETER = "filterByType";

    enum ColumnDef implements ColumnDefinition {
        ID(0, HISTORY_REPORT_ID_COLUMN, StandardTypes.VARCHAR, STRING, null),
        REPORT_NAME(1,HISTORY_REPORT_NAME_VARIABLE, StandardTypes.VARCHAR, STRING, null),
        CREATION_DATE(2, HISTORY_CREATE_TIME_COLUMN, StandardTypes.TIMESTAMP, LOCAL_DATE_TIME, null),
        TYPE(3, HISTORY_TYPE_COLUMN, StandardTypes.VARCHAR, STRING, null),
        STATUS(4, HISTORY_STATUS_NAME_COLUMN, StandardTypes.VARCHAR, STRING, null),
        HIERARCHY_ENABLE(5, HISTORY_HIERARHY_ENABLE_COLUMN, StandardTypes.BOOLEAN, BOOLEAN, null),
        // For Trino this can be a UUID, but Presto wants VARCHAR.
        GROUPING_VERSION(6, HISTORY_GROUPING_VERSION_COLUMN, StandardTypes.VARCHAR, UUID_TYPE, null),
        GROUPING_COLUMNS(7, HISTORY_GROUPING_COLUMNS_COLUMN, StandardTypes.ARRAY, LIST, VarcharType.VARCHAR.getTypeSignature()),
        USER_NAME(8, HISTORY_USER_NAME_COLUMN, StandardTypes.VARCHAR,STRING,null);


        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("pos", pos)
                    .add("fieldName", fieldName)
                    .add("fieldTypeString", fieldTypeString)
                    .add("dataType", dataType)
                    .add("parType", parType)
                    .toString();
        }

        private final int pos;
        private final String fieldName;
        private final String fieldTypeString;
        private final DataType dataType;
        private final TypeSignature parType;

        ColumnDef(int pos, String fieldName, String fieldTypeString, DataType dateType, TypeSignature parType) {
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
        public TypeSignature getParType() {
            return parType;
        }
    }

    public static final List<String> selectedFields = ImmutableList.copyOf(
            Arrays.stream(ColumnDef.values()).map(ColumnDef::getFieldName).collect(Collectors.toList())
    );

    @Inject
    public ReportHistoryApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                   RestTemplateCustomizer restTemplateCustomizer) {
        super(connectorId, config, typeManager, REPORT_HISTORY_ENDPOINT,restTemplateCustomizer);
    }

    @Override
    protected Map<TableDefinitionHandle, List<ColumnDefinition>> refreshFieldDefs() {
        return Collections.singletonMap(asTableDefinitionHandle(REPORT_HISTORIES.name()),ImmutableList.copyOf(ColumnDef.values()));
    }

    @Override
    public String getHandlerKey() {
        return REPORT_HISTORIES.name();
    }


    @Override
    public Optional<PagedModel<ReportHistoryFieldsView>> retrievePage(
            int page,
            int pageSize,
            List<CyodaColumnHandle> projectedColumns,
            CompoundPredicateNode predicates
    ) {

        int size = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;

        PredicateTraversal traversal = PredicateTraversal.of(predicates);

        UriTemplate uriTemplate = setupUriTemplate();

        ImmutableMap.Builder<String, Object> expansionBuilder = ImmutableMap.<String, Object>builder()
                .put(PAGE_REQUEST_PARAMETER, page)
                .put(SIZE_REQUEST_PARAMETER, size)
                .put(FIELDS_REQUEST_PARAMETER, selectedFields);


        // TODO: Add the other selection possibilities from the report history endpoint.
        Optional<Set<String>> filterByType = traversal.assembleFilterings(HISTORY_TYPE_COLUMN);
        LOG.debug("selecting by types:",()->filterByType.map(it-> String.join(",", it)).orElse("EMPTY"));

        // If the optional is empty, it means the predicates are such that everything must be filtered.
        if (!filterByType.isPresent()) return Optional.empty();

        if (!filterByType.get().isEmpty()) {
            expansionBuilder.put(HISTORY_FILTER_BY_TYPE_REQUEST_PARAMETER, filterByType.get());
        }

        Optional<Set<String>> reportNames = traversal.assembleFilterings(HISTORY_REPORT_NAME_VARIABLE);
        LOG.debug("selecting by report names:",()->reportNames.map(it-> String.join(",", it)).orElse("EMPTY"));
        if (!reportNames.isPresent()) return Optional.empty();
        if (!reportNames.get().isEmpty()) {
            if (reportNames.get().size() == 1 ) {
                expansionBuilder.put(HISTORY_REPORT_NAME_REQUEST_PARAMETER, reportNames.get().iterator().next());
            } else {
                expansionBuilder.put(HISTORY_REPORT_NAMES_REQUEST_PARAMETER, reportNames.get());
            }
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
                TemplateVariable.requestParameter(PAGE_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued(SIZE_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued(FIELDS_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued(HISTORY_FILTER_BY_TYPE_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued("username"),
                TemplateVariable.requestParameterContinued(HISTORY_REPORT_NAME_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued(HISTORY_REPORT_NAMES_REQUEST_PARAMETER),
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

        String columnName = columnHandle.getColumnName();
        if ( HISTORY_REPORT_ID_COLUMN.equals(columnName)) {
            columnName = "id";
        }
        return fields.get(columnName);
    }

    @Override
    public Iterator<ReportHistoryFieldsView> getResponseIterator(
            int pageSize,
            CyodaTableHandle tableHandle,
            CompoundPredicateNode predicates
    ) {
        List<CyodaColumnHandle> projectedColumns = tableHandle.getProjectedColumns().orElse(Collections.emptyList());
        return new PagedIterator<>(this, pageSize, projectedColumns, predicates).iterator();
    }

}
