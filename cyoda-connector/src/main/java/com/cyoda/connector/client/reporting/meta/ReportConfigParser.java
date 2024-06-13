package com.cyoda.connector.client.reporting.meta;

import com.cyoda.connector.client.reporting.metaproviders.StaticTableMetadata;
import com.cyoda.connector.client.types.CompoundDataType;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.logging.SupplierLogger;
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
import io.trino.spi.type.TypeManager;

import javax.annotation.Nonnull;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.cyoda.connector.client.reporting.meta.ReportConfigParser.ReportColumnType.ALIAS;
import static com.cyoda.connector.client.reporting.meta.ReportConfigParser.ReportColumnType.COLUMN;


public class ReportConfigParser {
    protected static final SupplierLogger LOG = SupplierLogger.get(ReportConfigParser.class);
    public static final String INVALID_REPORT_DEFINITION_FOR = "Invalid Report definition for ";
    private static final List<String> RESERVED_COLUMN_NAMES = StaticTableMetadata.REPORT_ROWS.getFieldList();
    private static final Configuration JSONPATHA_CONFIG = Configuration.builder()
            .jsonProvider(new JacksonJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .build();
//    public static final String PATH_PREFIX = "$.content";
    public static final String PATH_PREFIX = "$";
    public static final String DESCRIPTION = PATH_PREFIX + ".description";
    public static final String SINGLETON_REPORT = PATH_PREFIX + ".singletonReport";
    public static final String COLUMNS = PATH_PREFIX + ".columns";
    public static final String GROUPING = PATH_PREFIX + ".grouping";

    public static ReportDefinitionHandle parseReportDefinitionHandle(String jsonResult, String reportConfigId, TypeManager typeManager) {
        DocumentContext parse = JsonPath.parse(jsonResult, JSONPATHA_CONFIG);
        List<CyodaColumnHandle> cols = extractColumns(reportConfigId, parse, typeManager);
        String description = parse.read(DESCRIPTION, String.class);
        boolean isSingleton = Optional.ofNullable(
                parse.read(SINGLETON_REPORT, Boolean.class)
        ).orElse(false);
        List<String> groupingColumns = Optional.ofNullable(parse.read(
                        GROUPING,
                        new TypeRef<List<Map<String, String>>>() {
                        }
                ))
                .orElse(Collections.emptyList())
                .stream().map(map -> map.get("name")).toList();

        return new ReportDefinitionHandle(reportConfigId, description, cols, jsonResult, isSingleton, groupingColumns);
    }

    private static List<CyodaColumnHandle> extractColumns(String configId, DocumentContext context, TypeManager typeManager) {

        List<Map<String, String>> columns = Optional.ofNullable(context.read(COLUMNS, new TypeRef<List<Map<String, String>>>() {
                }))
                .orElseThrow(() -> new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR + configId + ". columns missing"));

        ImmutableList.Builder<CyodaColumnHandle> builder = ImmutableList.builder();
        AtomicInteger position = new AtomicInteger(RESERVED_COLUMN_NAMES.size());
        columns.forEach(column -> {
            try {
                String columnName = Optional.ofNullable(column.get("name"))
                        .orElseThrow(() -> new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR + configId + ". $.columns[*].name missing"));
                Preconditions.checkArgument(!RESERVED_COLUMN_NAMES.contains(columnName), "Report %s is using a reserved column name: %s." +
                        " Reserved names are: %s", configId, columnName, Joiner.on(", ").join(RESERVED_COLUMN_NAMES));
                ReportColumnType reportColumnType = Optional.ofNullable(column.get("@bean")).map(ReportConfigParser::getColType)
                        .orElseThrow(() -> new IllegalArgumentException(INVALID_REPORT_DEFINITION_FOR + configId + ". $.columns[*].@bean missing"));

                ParameterizedType colParType;
                switch (reportColumnType) {
                    case COLUMN:
                        colParType = fromColDefs(configId, context, columnName);
                        break;
                    case ALIAS:
                        colParType = fromAliasDefs(configId, context, columnName);
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
            } catch (Exception e) {
                throw new RuntimeException("Mapping column to data type FAILED. Column:\n" + column.toString(), e);
            }
        });
        return builder.build();
    }
    private static final List<String> COLTYPE_IDENTIFIERS = Arrays.stream(ReportColumnType.values()).map(ReportColumnType::getColType).collect(Collectors.toList());
    private static final String COLTYPE_SUMMARY = Joiner.on(", ").join(COLTYPE_IDENTIFIERS);



    private static String removeClassNamesFromPath(String source) {
        return source.replaceAll("@[^.]+", "");
    }
    private static ReportColumnType getColType(String s) {
        if (s.endsWith(COLUMN.getColType())) return COLUMN;
        if (s.endsWith(ALIAS.getColType())) return ALIAS;
        throw new IllegalArgumentException("Unknown report column type " + s + ". It should be one of " + COLTYPE_SUMMARY);
    }

    private static @Nonnull ParameterizedType toParametrizedType(@Nonnull String className, @Nonnull String path) {
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
        if (clazz instanceof Class<?>) {
            return Types.newParameterizedType(clazz, Object.class);
        } else {
            return Types.newParameterizedType(clazz);
        }
    }

    private static ParameterizedType fromColDefs(String reportName, DocumentContext documentContext, String columnName) {
        String usableColumnName = columnName.replace("'", "\\'").replace("\"", "\\\"");
        String basePath = String.format(PATH_PREFIX + ".colDefs[?(@.fullPath =='%1$s')].parts.value[-1:]", usableColumnName);
        return getParameterizedType(
                documentContext,
                basePath + ".type",
                basePath + ".path",
                () -> INVALID_REPORT_DEFINITION_FOR + reportName + ". " + COLUMN + " " + columnName +
                        " not defined. Returning Object type."
        );
    }

    private static ParameterizedType fromAliasDefs(String reportName, DocumentContext documentContext, String aliasName) {
        String basePath = String.format(PATH_PREFIX + ".aliasDefs[?(@.name =='%1$s')]", aliasName);
        return getParameterizedType(
                documentContext,
                basePath + ".aliasType",
                basePath + ".aliasPaths.value[0].colDef.parts.value[-1:].path",
                () -> INVALID_REPORT_DEFINITION_FOR + reportName + ". " + ALIAS + " " + aliasName +
                        " not defined. Returning Object type."
        );
    }

    private static ParameterizedType getParameterizedType(DocumentContext documentContext, String pathToClassName, String pathToColumnPath, Supplier<String> warnMessageSupplier) {
        String className = getSingleValue(documentContext, pathToClassName);
        String columnPath = getSingleValue(documentContext, pathToColumnPath);
        if (className == null || columnPath == null) {
            LOG.warn(warnMessageSupplier.get());
            return toParametrizedType(Object.class.getName(), "");
        }
        return toParametrizedType(className, columnPath);
    }

    private static <T> T getSingleValue(DocumentContext documentContext, String path) {
        TypeRef<List<T>> typeRef = new TypeRef<List<T>>() {
        };
        List<T> read = documentContext.read(path, typeRef);
        Preconditions.checkArgument(read.size() <= 1, "Non-unique selection. Found %s matching elements for %s", read.size(), path);
        return read.isEmpty() ? null : read.get(0);
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
}