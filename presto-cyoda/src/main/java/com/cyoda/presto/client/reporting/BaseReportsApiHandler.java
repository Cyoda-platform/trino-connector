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
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.ApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.client.types.DataTypeValue;
import com.cyoda.presto.client.types.TypesUtil;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.TypeSignature;
import com.facebook.presto.spi.SchemaTableName;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.hateoas.PagedModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.types.DataType.*;
import static java.util.Objects.requireNonNull;

// TODO: The API calls to Cyoda need to have some check on API version. Sasha might be able to say how he did it for UI
public abstract class BaseReportsApiHandler<T> extends AbstractTableHolder implements ApiRequestHandler<T> {

    public static final String REPORT_ENDPOINT = "/api/platform-api/reporting/report";
    public static final String PAGE_REQUEST_PARAMETER = "page";
    public static final String SIZE_REQUEST_PARAMETER = "size";
    public static final String FIELDS_REQUEST_PARAMETER = "fields";

    public static final int DEFAULT_PAGE_SIZE = 10;

    protected final CyodaConnectorId connectorId;
    protected final CyodaConfig config;
    protected final RestTemplateCustomizer restTemplateCustomizer;
    protected final TypeManager typeManager;
    protected final SupplierLogger log;

    protected BaseReportsApiHandler(CyodaConnectorId connectorId,
                                    CyodaConfig config,
                                    TypeManager typeManager,
                                    String endpoint,
                                    RestTemplateCustomizer restTemplateCustomizer,
                                    SupplierLogger log) {
        super(endpoint);
        this.connectorId = requireNonNull(connectorId, "connectorId is null");
        this.config = requireNonNull(config, "config is null");
        this.typeManager = requireNonNull(typeManager, "typeManager is null");
        this.restTemplateCustomizer = restTemplateCustomizer;
        this.log = log;
    }

    protected CyodaColumnHandle getColumnByName(List<ColumnDefinition> columnDefinitions, String columnName) {
        Optional<ColumnDefinition> found = columnDefinitions.stream()
                .filter(it -> it.getFieldName().equals(columnName))
                .findAny();
        return createColumnHandle(found.orElseThrow(() -> new IllegalStateException("Should not happen")));
    }

    protected Map<SchemaTableName, CyodaTable> setupTableMap(String endpoint, Map<TableDefinitionHandle, List<ColumnDefinition>> fieldDefs) {
        final URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(endpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Bad endpoint: "+endpoint,e);
        }
        List<URI> sources = Collections.singletonList(uri);
        Map<TableDefinitionHandle,List<CyodaColumnHandle>> cyodaColumnHandles = createCyodaColumnHandles(fieldDefs);
        return cyodaColumnHandles.entrySet().stream().collect(Collectors.toMap(
                entry-> new SchemaTableName(config.getSchemaName(), entry.getKey().tableName),
                entry -> new CyodaTable(
                        entry.getKey().tableName,
                        cyodaColumnHandles.get(entry.getKey()),
                        entry.getKey().reportConfigurationId,
                        entry.getKey().description,
                        sources
                )
        ));
    }

    protected Map<TableDefinitionHandle,List<CyodaColumnHandle>> createCyodaColumnHandles(Map<TableDefinitionHandle, List<ColumnDefinition>> fieldDefs) {
        return ImmutableMap.copyOf(
                fieldDefs.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, it->
                                it.getValue().stream()
                                        .map(fieldDef -> createColumnHandle(fieldDef)).collect(Collectors.toList())
                        ))
        );
    }

    protected CyodaColumnHandle createColumnHandle(ColumnDefinition fieldDef) {
        return new CyodaColumnHandle(
                connectorId.toString(),
                fieldDef.getFieldName(),
                fieldDef.getDataType().toPrestoType(typeManager),
                fieldDef.getDataType(),
                fieldDef.getPos(),
                getHandlerKey(),
                true
        );
    }


    protected Type toType(ColumnDefinition fieldDef) {
        return TypesUtil.toType(fieldDef,typeManager);
    }
    protected Type toType(String fieldTypeString,TypeSignature parType, TypeSignature mapValueType) {
        return TypesUtil.toType(fieldTypeString,parType,mapValueType,typeManager);
    }

    @Override
    public boolean hasTable(AuthContext authContext, SchemaTableName tableName) {
        return lookupTableMap(authContext).containsKey(tableName) || refreshTableMap(authContext).containsKey(tableName);
    }

    @Override
    public List<CyodaTable> getTables(AuthContext authContext) {
        return ImmutableList.copyOf(refreshTableMap(authContext).values());
    }

    @Override
    public @Nullable
    DataTypeValue<?> getValue(@Nullable T entity, CyodaColumnHandle columnHandle) {
        if ( entity == null ) return null;
        Object value = getFieldValueFromEntity(entity,columnHandle);
        if ( value == null ) return null;
        Object mappedField = mapFieldValue(value,columnHandle);
        return DataTypeValue.ofAny(mappedField,columnHandle.getDataType().getJavaType());
    }

    // TODO: These belong in the PrestoValueConverter
    protected @Nonnull Object mapFieldValue(@Nonnull final Object value, CyodaColumnHandle columnHandle) {
        if (columnHandle.getDataType() == LIST) {
            Type elementType = TypesUtil.getElementType(columnHandle.getColumnType());
            return ((List<?>) value).stream().map(it -> mapFieldSingleValue(it, DataType.fromType(elementType))).collect(Collectors.toList());
        }
//        if (columnHandle.getDataType() == ARRAY) {
//            return processList(Arrays.stream(((Object[])value)).collect(Collectors.toList()), columnHandle)
//                    .toArray();
//        }

        //TODO when does this happen?
        if ( value instanceof List && columnHandle.getDataType() != LIST && ((List<?>)value).size() == 1) {
            return mapFieldSingleValue(((List<?>)value).get(0),columnHandle.getDataType());
        }

        return mapFieldSingleValue(value, columnHandle.getDataType());
    }

    private Object mapFieldSingleValue(Object value, DataType dataType) {
        //TODO Converter's input is Slice
        if (dataType == UUID_TYPE && value instanceof String) {
            return UUID.fromString((String) value);
        }
        //TODO Converter's input is Long here and for all other date types
        if (dataType == DATE && value instanceof String) {
            return toDate((String) value);
        }
        if (dataType == LOCAL_DATE_TIME && value instanceof String) {
            return toLocalDateTime((String) value);
        }
        if (dataType == LOCAL_DATE && value instanceof String) {
            return toLocalDate((String) value);
        }
        if (dataType == ZONED_DATE_TIME && value instanceof String) {
            return toZonedDateTime((String) value);
        }
        //TODO Converter's input is Slice
        if (dataType == BIG_DECIMAL && value instanceof Number && !(value instanceof BigDecimal)) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return value;
    }

    private ZonedDateTime toZonedDateTime(String str) {
        return ZonedDateTime.parse(str, DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }

    private Date toDate(String str) {
        if (str == null) return null;
        LocalDateTime localDateTime = toLocalDateTime(str);
        return Timestamp.valueOf(localDateTime);
    }

    private LocalDateTime toLocalDateTime(String str) {
        return LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
    }

    private LocalDate toLocalDate(String str) {
        return LocalDate.parse(str, DateTimeFormatter.ISO_DATE);
    }

    protected abstract @Nullable Object getFieldValueFromEntity(@Nonnull T field, CyodaColumnHandle columnHandle);

    protected static String toReportName(@Nonnull String reportConfigId) {
        Preconditions.checkNotNull(reportConfigId,"reportConfigId is null");
        int start = reportConfigId.lastIndexOf('-');
        if (start < 0) {
            throw new IllegalArgumentException("report ID "+reportConfigId+" has incompatible format." +
                    " It should be <Tenant>-<EntityTypee>-<ReportName>");
        }
        String reportName = reportConfigId.substring(start + 1);
        Preconditions.checkArgument(!reportName.isEmpty(),"report ID '%s' has incompatible format." +
                " It should be <Tenant>-<EntityTypee>-<ReportName>",reportConfigId);
        return reportName;
    }

    public static @Nonnull String reportNameToTableName(@Nonnull String reportName) {
        Preconditions.checkNotNull(reportName,"reportName is null");
        Preconditions.checkArgument(!reportName.isEmpty(),"reportName is empty");
        String result = reportName
                //.replace(" ", "")
                //.replaceAll("[$\\-&%§@*#, ']", "_") // Let's not allow complicated things.
                //.replaceAll("([a-z])([A-Z]+)", "$1_$2")
                .toUpperCase(Locale.ROOT);
        Preconditions.checkArgument(!result.isEmpty(),"generated tableName is empty");
        return result;
    }

    protected <S> void  publishSize(SizeListener listener, PagedModel<S> pagedModel) {
        PagedModel.PageMetadata pageMetadata = Optional.ofNullable(pagedModel)
                .map(PagedModel::getMetadata)
                .orElse(PagedModel.empty().getMetadata());
        listener.sizeKnown(pageMetadata == null ? 0 : pageMetadata.getTotalElements());
    }

    protected static void logCreation(int pageSize, CyodaTableHandle tableHandle, CompoundPredicateNode predicates, SupplierLogger logger) {
        logger.debug("building responseIterator for %s with pageSize %s and predicates %s",
                tableHandle::getTableName,
                ()-> pageSize,
                predicates::toString
        );
    }

}
