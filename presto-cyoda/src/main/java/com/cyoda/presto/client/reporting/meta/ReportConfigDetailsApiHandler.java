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

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.reporting.BaseReportsApiHandler;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportTable;
import com.cyoda.presto.client.types.CompoundDataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.util.Types;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.trino.spi.StandardErrorCode;
import io.trino.spi.TrinoException;
import io.trino.spi.type.TypeManager;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler.REPORT_DEFS_ENDPOINT;
import static com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler.ReportColumnType.ALIAS;
import static com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler.ReportColumnType.COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_ID_COLUMN;
import static java.lang.String.format;

// TODO: Need to have a plan/solution for report configurations that have changed, and for which existing reports
// exist (with the old version). Maybe we should have a design (in Cyoda) that assembles possible report configurations
// from report histories, and generates the reports table from that. Or better yet, have an API endpoint that
// returns "all" report configurations, existing ones and ones that are stored with a report, in an aggregated fashion
public class ReportConfigDetailsApiHandler extends BaseReportsApiHandler<String, ReportDefinitionHandle> {

    protected static final SupplierLogger LOG = SupplierLogger.get(ReportConfigDetailsApiHandler.class);

    public static final String INVALID_REPORT_DEFINITION_FOR = "Invalid Report definition for ";
    public static final String REPORT_DETAILS_ENDPOINT = REPORT_DEFS_ENDPOINT + "/";

    // These are also reserved words for column names coming from reports.
    // TODO This validation needs to be moved to platform
    private static final List<String> RESERVED_COLUMN_NAMES = StaticReportTable.REPORT_ROWS.getFieldList();

    private final UriTemplate uriTemplate;


    @Inject
    public ReportConfigDetailsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                         RestTemplateCustomizer restTemplateCustomizer) {
        super(connectorId, config, typeManager, restTemplateCustomizer, LOG);
        uriTemplate = setupUriTemplate();
    }


    public ReportDefinitionHandle getReportDefSingleHandle(String reportConfigId) {
        URI templatedUri = uriTemplate.expand(Collections.singletonMap(REPORT_ID_COLUMN, reportConfigId));

        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(restTemplateCustomizer.getRestTemplateWithTechAuth());
        String reportName = toReportName(reportConfigId);

        try {
            String jsonResult = Optional.ofNullable(traverson
                            .follow()
                            .toEntity(String.class)).map(ResponseEntity::getBody)
                    .orElseThrow(() -> new IllegalArgumentException("No body found at " + templatedUri));

            DocumentContext parse = JsonPath.parse(jsonResult, JSONPATHA_CONFIG);
            List<CyodaColumnHandle> cols = extractColumns(reportName, parse);
            String description = parse.read("$.content.description", String.class);
            boolean isSingleton = Optional.ofNullable(
                    parse.read("$.content.singletonReport", Boolean.class)
            ).orElse(false);
            List<String> groupingColumns = Optional.ofNullable(parse.read(
                            "$.content.columns",
                            new TypeRef<List<Map<String, String>>>() {
                            }
                    ))
                    .orElse(Collections.emptyList())
                    .stream().map(map -> map.get("name")).toList();

            return new ReportDefinitionHandle(reportConfigId, reportName, description, cols, jsonResult, isSingleton, groupingColumns);
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, "retrieveCollection", e, templatedUri);
        }
    }

    private List<CyodaColumnHandle> extractColumns(String reportName, DocumentContext context) {

        List<Map<String, String>> columns = Optional.ofNullable(context.read("$.content.columns", new TypeRef<List<Map<String, String>>>() {
                }))
                .orElseThrow(() -> new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR + reportName + ". columns missing"));

        ImmutableList.Builder<CyodaColumnHandle> builder = ImmutableList.builder();
        AtomicInteger position = new AtomicInteger(RESERVED_COLUMN_NAMES.size());
        columns.forEach(column -> {
            String columnName = Optional.ofNullable(column.get("name"))
                    .orElseThrow(() -> new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR + reportName + ". $.columns[*].name missing"));
            Preconditions.checkArgument(!RESERVED_COLUMN_NAMES.contains(columnName), "Report %s is using a reserved column name: %s." +
                    " Reserved names are: %s", reportName, columnName, Joiner.on(", ").join(RESERVED_COLUMN_NAMES));
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

            CompoundDataType dataType = CompoundDataType.of(colParType, columnName);
            CyodaColumnHandle columnHandle = new CyodaColumnHandle(
                    removeClassNamesFromPath(columnName),
                    dataType.toPrestoType(typeManager),
                    dataType,
                    position.getAndIncrement(),
                    true
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
        if (path.matches("^.+\\[\\*\\](@(\\w+#)+\\w+\\.\\w+)?$")) {
            return Types.newParameterizedType(List.class, clazz);
        }
//        if (path.endsWith("[*]")) {
//            return Types.newParameterizedType(List.class, clazz);
//        }
//        if (path.contains("[*]")) {
//            return Types.newParameterizedType(List.class, clazz);
//        }
        return Types.newParameterizedType(clazz);
    }

    private ParameterizedType fromColDefs(String reportName, DocumentContext documentContext, String columnName) {
        String usableColumnName = columnName.replace("'", "\\'").replace("\"", "\\\"");
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
                basePath + ".aliasType",
                basePath + ".aliasPaths.value[0].colDef.parts.value[-1:].path",
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
        TypeRef<List<T>> typeRef = new TypeRef<List<T>>() {
        };
        List<T> read = documentContext.read(path, typeRef);
        Preconditions.checkArgument(read.size() <= 1, "Non-unique selection. Found %s matching elements for %s", read.size(), path);
        return read.isEmpty() ? null : read.get(0);
    }


    private UriTemplate setupUriTemplate() {

        URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(REPORT_DETAILS_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new TrinoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, e);
        }
        final ImmutableList.Builder<TemplateVariable> builder = ImmutableList.builder();
        builder.add(TemplateVariable.pathVariable("id"));
        builder.add();

        TemplateVariables vars = new TemplateVariables(builder.build());
        return UriTemplate.of(uri.toASCIIString()).with(vars);
    }


    private static final List<String> COLTYPE_IDENTIFIERS = Arrays.stream(ReportColumnType.values()).map(ReportColumnType::getColType).collect(Collectors.toList());
    private static final String COLTYPE_SUMMARY = Joiner.on(", ").join(COLTYPE_IDENTIFIERS);

    private static String removeClassNamesFromPath(String source) {
        return source.replaceAll("@[^.]+", "");
    }

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
