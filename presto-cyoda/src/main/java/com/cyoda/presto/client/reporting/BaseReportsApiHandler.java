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
import com.cyoda.presto.client.ApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.types.SupportedDataType;
import com.cyoda.presto.client.types.TypesUtil;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.TypeSignature;
import com.facebook.presto.spi.SchemaTableName;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.types.DataType.*;
import static java.util.Objects.requireNonNull;

public abstract class BaseReportsApiHandler<T> extends AbstractTableHolder implements ApiRequestHandler<T> {

    protected static final SupplierLogger LOG = SupplierLogger.get(BaseReportsApiHandler.class);

    public static final String REPORT_ENDPOINT = "/api/platform-api/reporting/report";
    public static final String PAGE_REQUEST_PARAMETER = "page";
    public static final String SIZE_REQUEST_PARAMETER = "size";
    public static final String FIELDS_REQUEST_PARAMETER = "fields";

    public static final int DEFAULT_PAGE_SIZE = 10;

    protected final CyodaConnectorId connectorId;
    protected final CyodaConfig config;
    protected final RestTemplate restTemplate;
    protected final TypeManager typeManager;

    protected BaseReportsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager, String endpoint, RestTemplateCustomizer restTemplateCustomizer) {
        super(endpoint);
        this.connectorId = requireNonNull(connectorId, "connectorId is null");
        this.config = requireNonNull(config, "config is null");
        this.typeManager = requireNonNull(typeManager, "typeManager is null");
        // TODO: This means that the authentication parameters are fixed at startup. Need to make this more flexible, without restarting presto.
        restTemplate = restTemplateCustomizer.getRestTemplate();
    }

    protected Map<SchemaTableName, CyodaTable> setupTableMap(String endpoint, Map<TableDefinitionHandle, List<ColumnDefinition>> fieldDefs) {
        final URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(endpoint);
            LOG.debug("Server URI %s", uri::toASCIIString);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Bad endpoint: "+endpoint,e);
        }
        List<URI> sources = Collections.singletonList(uri);
        Map<TableDefinitionHandle,List<CyodaColumnHandle>> cyodaColumnHandles = createCyodaColumnHandles(fieldDefs);
        return cyodaColumnHandles.entrySet().stream().collect(Collectors.toMap(
                entry-> new SchemaTableName(config.getSchemaName(), entry.getKey().tableName),
                entry -> new CyodaTable(entry.getKey().tableName, cyodaColumnHandles.get(entry.getKey()), entry.getKey().reportConfigurationId, sources)
        ));
    }

    protected Map<TableDefinitionHandle,List<CyodaColumnHandle>> createCyodaColumnHandles(Map<TableDefinitionHandle, List<ColumnDefinition>> fieldDefs) {
        return ImmutableMap.copyOf(
                fieldDefs.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, it->
                                it.getValue().stream()
                                        .map(fieldDef -> new CyodaColumnHandle(
                                                connectorId.toString(),
                                                fieldDef.getFieldName(),
                                                toType(fieldDef),
                                                fieldDef.getDataType(),
                                                fieldDef.getPos(),
                                                getHandlerKey()
                                        )).collect(Collectors.toList())
                        ))
        );
    }


    protected Type toType(ColumnDefinition fieldDef) {
        return TypesUtil.toType(fieldDef,typeManager);
    }
    protected Type toType(String fieldTypeString,TypeSignature parType, TypeSignature mapValueType) {
        return TypesUtil.toType(fieldTypeString,parType,mapValueType,typeManager);
    }

    @Override
    public boolean hasTable(SchemaTableName tableName) {
        return getTableMap().containsKey(tableName);
    }

    @Override
    public List<CyodaTable> getTables() {
        return ImmutableList.copyOf(getTableMap().values());
    }

    @Override
    public @Nullable SupportedDataType<?> getValue(@Nullable T entity, CyodaColumnHandle columnHandle) {
        if ( entity == null ) return null;
        Object value = getFieldValueFromEntity(entity,columnHandle);
        if ( value == null ) return null;
        Object mappedField = mapFieldValue(value,columnHandle);
        return SupportedDataType.ofAny(mappedField,columnHandle.getDataType().getJavaType());
    }

    protected @Nonnull Object mapFieldValue(@Nonnull final Object value, CyodaColumnHandle columnHandle) {
        if (columnHandle.getDataType() == UUID_TYPE && value instanceof String) {
            return UUID.fromString((String) value);
        }
        if (columnHandle.getDataType() == DATE && value instanceof String) {
            return toDate((String) value);
        }
        if (columnHandle.getDataType() == LOCAL_DATE_TIME && value instanceof String) {
            return toLocalDateTime((String) value);
        }
        if (columnHandle.getDataType() == LOCAL_DATE && value instanceof String) {
            return toLocalDate((String) value);
        }
        return value;
    }

    private Date toDate(String str) {
        if (str == null) return null;
        LocalDateTime localDateTime = toLocalDateTime(str);
        return Timestamp.valueOf(localDateTime);
    }

    protected LocalDateTime toLocalDateTime(String str) {
        return LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
    }

    protected LocalDate toLocalDate(String str) {
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

    protected static @Nonnull String reportNameToTableName(@Nonnull String reportName) {
        Preconditions.checkNotNull(reportName,"reportName is null");
        Preconditions.checkArgument(!reportName.isEmpty(),"reportName is empty");
        String result = reportName
                .replace(" ", "")
                .replaceAll("[$\\-&%§@*#']", "") // Let's not allow complicated things.
                .replaceAll("([a-z])([A-Z]+)", "$1_$2")
                .toUpperCase(Locale.ROOT);
        Preconditions.checkArgument(!result.isEmpty(),"generated tableName is empty");
        return result;
    }
}
