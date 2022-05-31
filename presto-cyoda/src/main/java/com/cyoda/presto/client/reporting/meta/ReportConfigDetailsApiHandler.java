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
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.client.types.TypesUtil;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.type.JsonType;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.TypeSignature;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.util.Types;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.reporting.AbstractTableHolder.TableDefinitionHandle.asTableDefinitionHandle;
import static com.cyoda.presto.client.reporting.CyodaStaticReportTable.REPORT_DETAILS;
import static com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler.REPORT_DEFS_ENDPOINT;
import static com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler.ReportColumnType.ALIAS;
import static com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler.ReportColumnType.COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.*;
import static com.cyoda.presto.client.types.DataType.*;
import static java.lang.String.format;

// TODO: Need to have a plan/solution for report configurations that have changed, and for which existing reports
// exist (with the old version). Maybe we should have a design (in Cyoda) that assembles possible report configurations
// from report histories, and generates the reports table from that. Or better yet, have an API endpoint that
// returns "all" report configurations, existing ones and ones that are stored with a report, in an aggregated fashion
public class ReportConfigDetailsApiHandler extends BasePagingReportsApiHandler<ReportDefinitionHandle>
        implements PagingApiRequestHandler<ReportDefinitionHandle> {

    protected static final SupplierLogger LOG = SupplierLogger.get(ReportConfigDetailsApiHandler.class);

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
                REPORT_DETAILS_ENDPOINT, restTemplateCustomizer,LOG);
        this.configuredReportsApiHandler = new ConfiguredReportsApiHandler(connectorId, config, typeManager, restTemplateCustomizer);
    }

    @Override
    protected Map<TableDefinitionHandle, List<ColumnDefinition>> refreshFieldDefs(AuthContext authContext) {
        return Collections.singletonMap(asTableDefinitionHandle(REPORT_DETAILS.name()), ImmutableList.copyOf(ColumnDef.values()));
    }

    @Override
    public String getHandlerKey() {
        return REPORT_DETAILS.name();
    }

    @Override
    public Optional<PagedModel<ReportDefinitionHandle>> retrievePage(
            AuthContext authContext,
            int page,
            int pageSize,
            List<CyodaColumnHandle> projectedColumns,
            CompoundPredicateNode predicates,
            SizeListener listener
    ) {

        UriTemplate uriTemplate = setupUriTemplate();

        PagedModel<GridConfigFieldsView> reportDefinitionModel = configuredReportsApiHandler.retrievePage(
                authContext, page, pageSize, projectedColumns, predicates,listener).orElse(PagedModel.empty());
        // TODO: There is a bug, where the API returns one more than the page size, so use limit as long as this bug persists
        Set<String> ids = reportDefinitionModel.getContent().stream().limit(pageSize).map(it -> it.getGridConfigFields().get(REPORT_ID_COLUMN)).collect(Collectors.toSet());

        List<ReportDefinitionHandle> reportDefinitionHandles = getReportDefinitionHandles(authContext, uriTemplate, ids);
        PagedModel<ReportDefinitionHandle> reportDefs = PagedModel.of(reportDefinitionHandles, reportDefinitionModel.getMetadata());
        publishSize(listener,reportDefs);
        return Optional.of(reportDefs);
    }

    private List<ReportDefinitionHandle> getReportDefinitionHandles(AuthContext authContext, UriTemplate uriTemplate, @Nonnull Set<String> ids) {

        if (ids.isEmpty()) return Collections.emptyList();
        ImmutableList.Builder<ReportDefinitionHandle> builder = ImmutableList.builder();
        ids.forEach(reportConfigId -> {
            URI templatedUri = uriTemplate.expand(Collections.singletonMap(REPORT_ID_COLUMN, reportConfigId));

            Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
            traverson.setRestOperations(restTemplateCustomizer.getRestTemplate(authContext));
            String reportName = toReportName(reportConfigId);

            try {
                String jsonResult = Optional.ofNullable(traverson
                                .follow()
                                .toEntity(String.class)).map(ResponseEntity::getBody)
                        .orElseThrow(() -> new IllegalArgumentException("No body found at " + templatedUri));

                DocumentContext parse = JsonPath.parse(jsonResult,JSONPATHA_CONFIG);
                List<CyodaColumnHandle> cols = extractColumns(reportName, parse);
                String description = parse.read("$.content.description",String.class);

                builder.add(new ReportDefinitionHandle(reportConfigId, reportName, description, cols, jsonResult));
            } catch (HttpClientErrorException e) {
                throw requestFailedException(this, "retrieveCollection", e, templatedUri);
            }
        });
        List<ReportDefinitionHandle> result = builder.build();
        LOG.debug("Got %s report definitions", result.size());
        return result;

    }

    private List<CyodaColumnHandle> extractColumns(String reportName, DocumentContext context) {

        List<Map<String, String>> columns = Optional.ofNullable(context.read("$.content.columns", new TypeRef<List<Map<String, String>>>() {}))
                    .orElseThrow(() -> new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR + reportName + ". columns missing"));

        ImmutableList.Builder<CyodaColumnHandle> builder = ImmutableList.builder();
        AtomicInteger position = new AtomicInteger();
        columns.forEach(column -> {
            String columnName = Optional.ofNullable(column.get("name"))
                    .orElseThrow(() -> new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR + reportName + ". $.columns[*].name missing"));
            ReportColumnType reportColumnType = Optional.ofNullable(column.get("@bean")).map(this::getColType)
                    .orElseThrow(() -> new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR + reportName + ". $.columns[*].@bean missing"));

            ParameterizedType colParType;
            switch (reportColumnType) {
                case COLUMN:
                    colParType = fromColDefs(reportName, context, columnName);
                    break;
                case ALIAS:
                    colParType = fromAliasDefs(reportName, context, columnName);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported Cyoda report column type " + reportColumnType);
            }

            DataType dataType;
            dataType = mapDataType(
                    Optional.ofNullable(DataType.dataTypeFromClass((Class<?>) colParType.getRawType())).orElse(DataType.OBJECT)
            );
            Type[] actualTypeArguments = colParType.getActualTypeArguments();
            TypeSignature firstArg = Optional.ofNullable(actualTypeArguments.length > 0 ? (Class<?>) actualTypeArguments[0] : null)
                    .map(arg -> TypesUtil.toType(DataType.dataTypeFromClass(arg).getTypeString(), null, null, typeManager).getTypeSignature()).orElse(null);
            TypeSignature secondArg = Optional.ofNullable(actualTypeArguments.length > 1 ? (Class<?>) actualTypeArguments[1] : null)
                    .map(arg -> TypesUtil.toType(DataType.dataTypeFromClass(arg).getTypeString(), null, null, typeManager).getTypeSignature()).orElse(null);
            CyodaColumnHandle columnHandle = new CyodaColumnHandle(
                    connectorId.toString(),
                    columnName,
                    toType(dataType.getTypeString(), firstArg, secondArg),
                    dataType,
                    position.getAndIncrement(),
                    getHandlerKey()
            );
            builder.add(columnHandle);
        });
        return builder.build();
    }

    private ReportColumnType getColType(String s) {
        if (s.endsWith(COLUMN.getColType())) return COLUMN;
        if (s.endsWith(ALIAS.getColType())) return ALIAS;
        throw new IllegalArgumentException("Unknown report column type " + s + ". It should be one of " + COLTYPE_SUMMARY);
    }


    private @Nonnull ParameterizedType toParametrizedType(@Nonnull String className, @Nonnull String path) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            clazz = Object.class;
        }
        // Need to check if a Map or a List is returned.
        // TODO: Just a hack right now. Check actual logic on Cyoda side.
        // Currently Maps are never returned in reports, only their values. See MapAllElementAccessorCmp
        // We can multiple [*] references in a CyodaColumnPath. See for example TestTrade.
//        if (path.endsWith("[*]")) {
//            return Types.newParameterizedType(List.class, clazz);
//        }
//        if (path.contains("[*]")) {
//            return Types.newParameterizedType(List.class, clazz);
//        }
        return Types.newParameterizedType(clazz);
    }

    private ParameterizedType fromColDefs(String reportName, DocumentContext documentContext, String columnName) {
        String usableColumnName = columnName.replace("'","\\'").replace("\"","\\\"");
        String basePath = format("$.content.colDefs[?(@.fullPath =='%1$s')].parts.value[-1:]", usableColumnName);
        return getParameterizedType(
                documentContext,
                basePath + ".type",
                basePath + ".path",
                () -> INVALID_REPORT_DEFINITION_FOR + reportName + ". " + COLUMN + " " + columnName +
                        " not defined. Returning Object type."
        );
    }

    private ParameterizedType fromAliasDefs(String reportName, DocumentContext documentContext, String aliasName) {
        String basePath = format("$.content.aliasDefs[?(@.name =='%1$s')]", aliasName);
        return getParameterizedType(
                documentContext,
                basePath+".aliasType",
                basePath+".aliasPaths.value[0].colDef.parts.value[-1:].path",
                () -> INVALID_REPORT_DEFINITION_FOR + reportName + ". " + ALIAS + " " + aliasName +
                        " not defined. Returning Object type."
        );
    }

    private ParameterizedType getParameterizedType(DocumentContext documentContext, String pathToClassName, String pathToColumnPath, Supplier<String> warnMessageSupplier) {
        String className = getSingleValue(documentContext, pathToClassName);
        String columnPath = getSingleValue(documentContext, pathToColumnPath);
        if (className == null || columnPath == null) {
            LOG.warn(warnMessageSupplier.get());
            return toParametrizedType(Object.class.getName(), "");
        }
        return toParametrizedType(className, columnPath);
    }

    private <T> T getSingleValue(DocumentContext documentContext, String path) {
        TypeRef<List<T>> typeRef = new TypeRef<List<T>>() {};
        List<T> read = documentContext.read(path, typeRef);
        Preconditions.checkArgument(read.size()<=1,"Non-unique selection. Found %s matching elements for %s",read.size(),path);
        return read.isEmpty() ? null : read.get(0);
    }


    private DataType mapDataType(DataType dataType) {
        // TODO: Check if we ever have arrays as columns.
        if ( dataType == ARRAY ) throw new UnsupportedOperationException("Not yet clear if we have this use case");
        return dataType;
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
        return super.mapFieldValue(value, columnHandle);
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull ReportDefinitionHandle field, CyodaColumnHandle columnHandle) {
        if (REPORT_ID_COLUMN.equals(columnHandle.getColumnName())) {
            return field.reportConfigId;
        }
        if (REPORT_NAME_COLUMN.equals(columnHandle.getColumnName())) {
            return field.reportName;
        }
        if (REPORT_JSON_COLUMN.equals(columnHandle.getColumnName())) {
            return field.json;
        }
        if (REPORT_COLUMNS_COLUMN.equals(columnHandle.getColumnName())) {
            return field.columns;
        }
        throw new IllegalArgumentException(columnHandle.getColumnName() + " is not defined on ReportDefinitionHandle");
    }

    private static final List<String> COLTYPE_IDENTIFIERS = Arrays.stream(ReportColumnType.values()).map(ReportColumnType::getColType).collect(Collectors.toList());
    private static final String COLTYPE_SUMMARY = Joiner.on(", ").join(COLTYPE_IDENTIFIERS);

    enum ReportColumnType {
        COLUMN("ReportSimpleColumn"),
        ALIAS("ReportAliasColumn");

        private final String colType;

        ReportColumnType(String colType) {
            this.colType = colType;
        }

        public String getColType() {
            return colType;
        }
    }

    private static final Configuration JSONPATHA_CONFIG = Configuration.builder()
            .jsonProvider(new JacksonJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .build();
}
