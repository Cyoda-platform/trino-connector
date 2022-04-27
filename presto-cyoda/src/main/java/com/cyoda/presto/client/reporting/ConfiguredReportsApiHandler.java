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
import com.cyoda.presto.CyodaTable;
import com.cyoda.presto.client.CyodaApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.logic.PredicateNode;
import com.cyoda.presto.client.neededatcyoda.GridConfigFieldsView;
import com.cyoda.presto.client.paging.PagedIterator;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.client.types.SupportedDataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.type.TimestampType;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.SchemaTableName;
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
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;
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
import static com.cyoda.presto.client.types.DataType.LOCAL_DATE_TIME;
import static com.cyoda.presto.client.types.DataType.STRING;
import static com.facebook.presto.common.type.VarcharType.VARCHAR;
import static java.util.Objects.requireNonNull;

public class ConfiguredReportsApiHandler implements CyodaApiRequestHandler<GridConfigFieldsView> {

    private static final SupplierLogger LOG = SupplierLogger.get(ConfiguredReportsApiHandler.class);

    public static final String REPORT_DEFS_ENDPOINT = "/api/platform-api/reporting/definitions";
    public static final int DEFAULT_PAGE_SIZE = 10;

    public static final String ID_COLUMN_NAME = "id";
    public static final String DESCRIPTION_COLUMN_NAME = "description";
    public static final String TYPE_COLUMN_NAME = "type";
    public static final String USER_ID_COLUMN_NAME = "userId";
    public static final String CREATION_DATE_COLUMN_NAME = "creationDate";

    enum FieldDef {
        ID              (0,ID_COLUMN_NAME,VARCHAR,STRING),
        DESCRIPTION     (1,DESCRIPTION_COLUMN_NAME,VARCHAR,STRING),
        TYPE            (2,TYPE_COLUMN_NAME,VARCHAR,STRING),
        USER_ID         (3,USER_ID_COLUMN_NAME,VARCHAR,STRING),
        CREATION_DATE   (4,CREATION_DATE_COLUMN_NAME,TimestampType.TIMESTAMP, LOCAL_DATE_TIME);

        private final int pos;
        private final String fieldName;
        private final Type fieldType;
        private final DataType dataType;

        FieldDef(int pos, String fieldName, Type fieldType, DataType dateType) {
            this.pos = pos;
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.dataType = dateType;
        }

        int getPos() {
            return pos;
        }

        String getFieldName() {
            return fieldName;
        }

        Type getFieldType() {
            return fieldType;
        }

        DataType getDataType() {
            return dataType;
        }
    }
    public static final List<String> selectedFields = ImmutableList.copyOf(
            Arrays.stream(FieldDef.values()).map(FieldDef::getFieldName).collect(Collectors.toList())
    );

    public static final List<String> filterableColumnNames = ImmutableList.<String>builder()
            .add(TYPE_COLUMN_NAME).build();

    public static final Map<Integer,String> positionToField = ImmutableMap.copyOf(
            Arrays.stream(FieldDef.values()).collect(Collectors.toMap(FieldDef::getPos,FieldDef::getFieldName))
    );

    private final CyodaConnectorId connectorId;
    private final CyodaConfig config;
    private final RestTemplate restTemplate;
    private final Map<SchemaTableName, CyodaTable> tableMap;
    private final List<CyodaTable> cyodaTables;

    @Inject
    public ConfiguredReportsApiHandler(CyodaConnectorId connectorId, CyodaConfig config) throws URISyntaxException {
        this.connectorId = requireNonNull(connectorId, "connectorId is null");
        this.config = requireNonNull(config, "config is null");

        // TODO: This means that the authentication parameters are fixed at startup. Need to make this more flexible, without restarting presto.
        this.restTemplate = RestTemplateCustomizer.newRestTemplate(config, MediaTypes.HAL_JSON);

        this.tableMap = setupTables();
        this.cyodaTables = ImmutableList.copyOf(tableMap.values());
    }

    @Override
    public String getHandlerKey() {
        return CyodaStaticReportTable.REPORTS.name();
    }

    @Override
    public boolean hasTable(SchemaTableName tableName) {
        return tableMap.containsKey(tableName);
    }

    private Map<SchemaTableName, CyodaTable> setupTables() throws URISyntaxException {
        ImmutableList<CyodaColumnHandle> cyodaColumnHandles = ImmutableList.copyOf(
                Arrays.stream(FieldDef.values()).map(it ->
                        new CyodaColumnHandle(
                                connectorId.toString(),
                                it.fieldName,
                                it.fieldType,
                                it.dataType,
                                it.pos,
                                getHandlerKey()
                        )
                ).collect(Collectors.toList())
        );

        URI uri = config.getServerUrl().toURI().resolve(REPORT_DEFS_ENDPOINT);
        LOG.debug("Server URI %s", uri::toASCIIString);
        List<URI> sources = Collections.singletonList(uri);
        CyodaTable table = new CyodaTable(CyodaStaticReportTable.REPORTS.name(), cyodaColumnHandles, sources, true);
        SchemaTableName key = new SchemaTableName(config.getSchemaName(), table.getName());
        return Collections.singletonMap(key, table);
    }

    @Nonnull
    public List<CyodaTable> getTables() {
        return cyodaTables;
    }

    @Override
    public Optional<PagedModel<GridConfigFieldsView>> retrievePage(
            int page,
            int pageSize,
            List<CyodaColumnHandle> projectedColumns, PredicateNode<Any> predicates) {

        int size = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;


        Collection<PredicateNode<?>> conjunctions = PredicateNode.conjunctions(predicates);
        PredicateTraversal traversal = PredicateTraversal.of(conjunctions);

        List<String> columnsWithFilter = Collections.singletonList(TYPE_COLUMN_NAME);
        LOG.debug("Columns with Filter: %s",() -> Joiner.on(", ").join(columnsWithFilter));

        UriTemplate uriTemplate = setupUriTemplate(columnsWithFilter);

        ImmutableMap.Builder<String, Object> expansionBuilder = ImmutableMap.<String, Object>builder()
                .put("page", page)
                .put("size", size)
                .put("fields", selectedFields);


        Optional<Set<String>> filterByTypeMaybe = traversal.assembleFilterings(TYPE_COLUMN_NAME);
        if ( !filterByTypeMaybe.isPresent() ) return Optional.empty();
        Set<String> filterByType = filterByTypeMaybe.get();

        LOG.debug( "Filter on type column: %s",() -> Joiner.on(", ").join(filterByType));

        if (!filterByType.isEmpty()) {
            expansionBuilder.put("filterByType", filterByType);
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
            return Optional.ofNullable(gridConfigFieldsViews);
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, "retrieveCollection", e, templatedUri);
        }
    }

    private UriTemplate setupUriTemplate(List<String> additionalVars) {
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
                TemplateVariable.requestParameterContinued("fields")
        );
        if (additionalVars.contains(TYPE_COLUMN_NAME))
            builder.add(TemplateVariable.requestParameterContinued("filterByType"));

        TemplateVariables vars = new TemplateVariables(builder.build());
        return UriTemplate.of(uri.toASCIIString())
                .with(vars);
    }

    @Override
    public SupportedDataType<?> getValue(GridConfigFieldsView entity, CyodaColumnHandle columnHandle) {
        requireNonNull(entity, "entity is null");
        Map<String, String> fields = entity.getGridConfigFields();
        int field = columnHandle.getOrdinalPosition();
        switch (field) {
            case 0:
                return SupportedDataType.of(fields.get(ID_COLUMN_NAME), String.class);
            case 1:
                return SupportedDataType.of(fields.get(DESCRIPTION_COLUMN_NAME), String.class);
            case 2:
                return SupportedDataType.of(fields.get(TYPE_COLUMN_NAME), String.class);
            case 3:
                return SupportedDataType.of(fields.get(USER_ID_COLUMN_NAME), String.class);
            case 4:
                return SupportedDataType.of(toLocalDateTime(fields.get(CREATION_DATE_COLUMN_NAME)), LocalDateTime.class);
            default:
                throw new IllegalArgumentException("field index " + field + " is out of bounds. valid is 0..4");
        }
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
