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
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.facebook.airlift.json.JsonCodec;
import com.facebook.presto.common.type.JsonType;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.TypeSignature;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.client.Traverson;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.reporting.AbstractTableHolder.TableDefinitionHandle.asTableDefinitionHandle;
import static com.cyoda.presto.client.reporting.CyodaStaticReportTable.REPORT_DETAILS;
import static com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler.REPORT_DEFS_ENDPOINT;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.*;
import static com.cyoda.presto.client.types.DataType.LIST;
import static com.cyoda.presto.client.types.DataType.STRING;

// TODO: Need to have a plan/solution for report configurations that have changed, and for which existing reports
// exist (with the old version). Maybe we should have a design (in Cyoda) that assembles possible report configurations
// from report histories, and generates the reports table from that. Or better yet, have an API endpoint that
// returns "all" report configurations, existing ones and ones that are stored with a report, in an aggregated fashion
public class ReportConfigDetailsApiHandler extends BaseReportsApiHandler<ReportDefinitionHandle>
        implements PagingApiRequestHandler<ReportDefinitionHandle> {

    public static final String INVALID_REPORT_DEFINITION_FOR = "Invalid Report definition for ";
    private final ConfiguredReportsApiHandler configuredReportsApiHandler;

    public static final String REPORT_DETAILS_ENDPOINT = REPORT_DEFS_ENDPOINT + "/";

    enum ColumnDef implements ColumnDefinition {
        ID(0, REPORT_ID_COLUMN, StandardTypes.VARCHAR, STRING, null),
        REPORT_NAME(1, REPORT_NAME_COLUMN, StandardTypes.VARCHAR, STRING, null),
        REPORT_COLUMNS(2, REPORT_COLUMNS_COLUMN, StandardTypes.ARRAY, LIST, JsonType.JSON.getTypeSignature()),
        REPORT_JSON(3, REPORT_JSON_COLUMN, StandardTypes.JSON, STRING, null);

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("pos", pos)
                    .add("fieldName", fieldName)
                    .add("fieldTypeString", fieldTypeString)
                    .add("dataType", dataType)
                    .add("partType", parType)
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

    @Inject
    public ReportConfigDetailsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                         RestTemplateCustomizer restTemplateCustomizer) {
        super(connectorId, config, typeManager,
                REPORT_DETAILS_ENDPOINT,restTemplateCustomizer);
        this.configuredReportsApiHandler = new ConfiguredReportsApiHandler(connectorId,config,typeManager,restTemplateCustomizer);
    }

    @Override
    protected Map<TableDefinitionHandle, List<ColumnDefinition>> refreshFieldDefs() {
        return Collections.singletonMap(asTableDefinitionHandle(REPORT_DETAILS.name()),ImmutableList.copyOf(ColumnDef.values()));
    }

    @Override
    public String getHandlerKey() {
        return REPORT_DETAILS.name();
    }

    @Override
    public Optional<PagedModel<ReportDefinitionHandle>> retrievePage(int page, int pageSize, List<CyodaColumnHandle> projectedColumns, ColumnPredicateNode<Any> predicates) {

        UriTemplate uriTemplate = setupUriTemplate();

        PagedModel<GridConfigFieldsView> reportDefinitionModel = configuredReportsApiHandler.retrievePage(page, pageSize, projectedColumns, predicates).orElse(PagedModel.empty());
        Set<String> ids = reportDefinitionModel.getContent().stream().map(it -> it.getGridConfigFields().get(REPORT_ID_COLUMN)).collect(Collectors.toSet());

        List<ReportDefinitionHandle> reportDefinitionHandles = getReportDefinitionHandles(uriTemplate, ids);
        return Optional.of(PagedModel.of(reportDefinitionHandles,reportDefinitionModel.getMetadata()));
    }

    private List<ReportDefinitionHandle> getReportDefinitionHandles(UriTemplate uriTemplate, @Nonnull Set<String> ids) {

        if ( ids.isEmpty() ) return Collections.emptyList();
        ImmutableList.Builder<ReportDefinitionHandle> builder = ImmutableList.builder();
        ids.forEach(reportConfigId -> {
            URI templatedUri = uriTemplate.expand(Collections.singletonMap(REPORT_ID_COLUMN, reportConfigId));

            Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
            traverson.setRestOperations(restTemplate);
            String reportName =  toReportName(reportConfigId);

            try {
                @SuppressWarnings("unchecked")
                Map<String,?> map = Optional.ofNullable(traverson
                                .follow()
                                .toObject(Map.class))
                        .orElse(Collections.emptyMap());
                @SuppressWarnings("unchecked")
                Map<String,Object> repDef = Optional.ofNullable((Map<String,Object>) map.get("content")).orElse(Collections.emptyMap());
                repDef.remove("condition");
                String json = JsonCodec.mapJsonCodec(String.class, Object.class).toJson(repDef);

                List<CyodaColumnHandle> cols = extractColumns(reportName,repDef);

                builder.add(new ReportDefinitionHandle(reportConfigId, reportName, cols, json));
            } catch (HttpClientErrorException e) {
                throw requestFailedException(this, "retrieveCollection", e, templatedUri);
            }
        });
        List<ReportDefinitionHandle> result = builder.build();
        LOG.debug("Got %s report definitions",result.size());
        return result;

    }

    // TODO: Maybe use JsonPath on the json instead of this silly mechanism.
    @SuppressWarnings("unchecked")
    private List<CyodaColumnHandle> extractColumns(String reportName, Map<String, Object> repDef) {

        List<Map<String, String>> columns;
        List<Map<String, String>> colDefs;
        List<Map<String, String>> aliases;

        try {
            columns = (List<Map<String, String>>) Optional.ofNullable(repDef.get("columns"))
                    .orElseThrow(() -> new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR +reportName+". colums missing"));
            colDefs = (List<Map<String, String>>) Optional.ofNullable(repDef.get("colDefs"))
                    .orElseThrow(() -> new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR+reportName+". colDefs missing"));
            aliases = (List<Map<String, String>>) Optional.ofNullable(repDef.get("aliasDefs"))
                    .orElse(Collections.emptyList());
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR+reportName+". Structure is not as expected");
        }

        // TODO: Need to cover complex types, such as arrays and maps. Example: if the path ends with a [*], it's array
        // tenantId.legalEntity.meta.[*] --> a Map
        // companyId.employeeIds.[*]@org#cyoda#gs#business#model#companydata#SmallCompany$EntityRef.employeeId -> an Array
        // In such cases, we also need to capture a parametrized type, i.e. an map/array, with elements of a given type.
        // See also below, when we do a toType(dataType.getTypeString(),null,null)
        ImmutableList.Builder<CyodaColumnHandle> builder = ImmutableList.builder();
        AtomicInteger position = new AtomicInteger();
        columns.forEach(column -> {
            String columnName = Optional.ofNullable(column.get("name"))
                    .orElseThrow(() -> new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR+reportName+". colums[*].name missing"));
            // First search colDefs
            Optional<String> columnClass = colDefs.stream()
                    .filter(it -> columnName.equals(
                                    Optional.ofNullable(it.get("fullPath"))
                                            .orElseThrow(() -> new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR+reportName+". colDefs[*].fullPath missing"))
                            )
                    ).map(it -> Optional.ofNullable(it.get("colType"))
                            .orElseThrow(() -> new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR+reportName+". colDefs[*].colType missing"))
                    ).findAny();
            if ( !columnClass.isPresent() ) {
                columnClass = aliases.stream()
                        .filter(it -> columnName.equals(
                                        Optional.ofNullable(it.get("name"))
                                                .orElseThrow(() -> new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR+reportName+". aliasDefs[*].name missing"))
                                )
                        ).map(it -> Optional.ofNullable(it.get("aliasType"))
                                .orElseThrow(() -> new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR+reportName+". aliasDefs[*].aliasType missing"))
                        ).findAny();
            }
            if ( columnClass.isPresent() ) {
                 DataType dataType;
                try {
                    Class<?> clazz  = Class.forName(columnClass.get());
                    dataType = Optional.ofNullable(DataType.dataTypeFromClass(clazz)).orElse(DataType.OBJECT);
                } catch (ClassNotFoundException e) {
                    dataType = DataType.OBJECT;
                }
                dataType = mapDataType(dataType);
                CyodaColumnHandle columnHandle = new CyodaColumnHandle(
                        connectorId.toString(),
                        columnName,
                        // TODO: Need to cover Maps and Collections. But that will be a lot of redesign here.
                        toType(dataType.getTypeString(),null,null),
                        dataType,
                        position.getAndIncrement(),
                        getHandlerKey()
                );
                builder.add(columnHandle);
            } else throw new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR+reportName+". Column "+columnName+" not defined");
        });
        return builder.build();
    }

    private DataType mapDataType(DataType dataType) {
        // Need to project collections and maps onto Object for now
        switch (dataType) {
            case MAP:
            case LIST:
            case ARRAY:
                return DataType.OBJECT;
            default:
                return dataType;
        }
    }

    private UriTemplate setupUriTemplate() {

        URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(REPORT_DETAILS_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, e);
        }
        final ImmutableList.Builder<TemplateVariable> builder = ImmutableList.builder();
        builder.add(TemplateVariable.pathVariable("id"));
        builder.add();

        TemplateVariables vars = new TemplateVariables(builder.build());
        return UriTemplate.of(uri.toASCIIString()).with(vars);
    }

    @Nonnull
    @Override
    protected Object mapFieldValue(@Nonnull final Object value, CyodaColumnHandle columnHandle) {
        return super.mapFieldValue(value,columnHandle);
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull ReportDefinitionHandle field, CyodaColumnHandle columnHandle) {
        if ( REPORT_ID_COLUMN.equals(columnHandle.getColumnName())) {
            return field.reportConfigId;
        }
        if ( REPORT_NAME_COLUMN.equals(columnHandle.getColumnName())) {
            return field.reportName;
        }
        if ( REPORT_JSON_COLUMN.equals(columnHandle.getColumnName())) {
            return field.json;
        }
        if ( REPORT_COLUMNS_COLUMN.equals(columnHandle.getColumnName())) {
            return field.columns;
        }
        throw new IllegalArgumentException(columnHandle.getColumnName()+" is not defined on ReportDefinitionHandle");
    }

    @Override
    public Iterator<ReportDefinitionHandle> getResponseIterator(
            int pageSize,
            CyodaTableHandle tableHandle,
            ColumnPredicateNode<Any> predicates
    ) {
        List<CyodaColumnHandle> projectedColumns = tableHandle.getProjectedColumns().orElse(Collections.emptyList());
        return new PagedIterator<>(this, pageSize, projectedColumns, predicates).iterator();
    }

}
