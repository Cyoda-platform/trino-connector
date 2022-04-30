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
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.TypeSignature;
import com.facebook.presto.common.type.TypeSignatureParameter;
import com.facebook.presto.spi.SchemaTableName;
import com.google.common.collect.ImmutableList;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

abstract class BaseReportsApiHandler<S extends RepresentationModel<?>,T> implements ApiRequestHandler<S,T> {

    protected static final SupplierLogger LOG = SupplierLogger.get(BaseReportsApiHandler.class);

    public static final String REPORT_DEFS_ENDPOINT = "/api/platform-api/reporting/definitions";
    public static final String REPORT_HISTORY_ENDPOINT = "/api/platform-api/reporting/history";
    public static final String REPORT_DETAILS_ENDPOINT = REPORT_DEFS_ENDPOINT + "/";

    public static final int DEFAULT_PAGE_SIZE = 10;

    protected final CyodaConnectorId connectorId;
    protected final CyodaConfig config;
    protected final RestTemplate restTemplate;
    protected final Map<SchemaTableName, CyodaTable> tableMap;
    protected final List<CyodaTable> cyodaTables;
    protected final FieldDefinition[] fieldDefs;
    protected final TypeManager typeManager;

    protected BaseReportsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager, String endpoint, String tableName,
                                    FieldDefinition[] fieldDefs) {
        this.connectorId = requireNonNull(connectorId, "connectorId is null");
        this.config = requireNonNull(config, "config is null");
        this.typeManager = requireNonNull(typeManager, "typeManager is null");
        this.fieldDefs = requireNonNull(fieldDefs,"fieldDefs are null");

        // TODO: This means that the authentication parameters are fixed at startup. Need to make this more flexible, without restarting presto.
        this.restTemplate = RestTemplateCustomizer.newRestTemplate(config, MediaTypes.HAL_JSON);

        this.tableMap = setupTables(endpoint, tableName);
        this.cyodaTables = ImmutableList.copyOf(tableMap.values());

    }

    protected Map<SchemaTableName, CyodaTable> setupTables(String endpoint, String tableName) {
        ImmutableList<CyodaColumnHandle> cyodaColumnHandles = ImmutableList.copyOf(
                Arrays.stream(fieldDefs).map(it ->
                        new CyodaColumnHandle(
                                connectorId.toString(),
                                it.getFieldName(),
                                toType(it),
                                it.getDataType(),
                                it.getPos(),
                                getHandlerKey()
                        )
                ).collect(Collectors.toList())
        );

        final URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(endpoint);
            LOG.debug("Server URI %s", uri::toASCIIString);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Bad endpoint: "+endpoint,e);
        }
        List<URI> sources = Collections.singletonList(uri);
        CyodaTable table = new CyodaTable(tableName, cyodaColumnHandles, sources, true);
        SchemaTableName key = new SchemaTableName(config.getSchemaName(), table.getName());
        return Collections.singletonMap(key, table);
    }

    private static final List<String> supportedTypes = ImmutableList.of(
            StandardTypes.VARCHAR,StandardTypes.TIMESTAMP,StandardTypes.BOOLEAN,StandardTypes.UUID,StandardTypes.JSON
    );
    private Type toType(FieldDefinition fieldDef) {
        String fieldTypeString = fieldDef.getFieldTypeString();
        if (fieldTypeString.equals(StandardTypes.ARRAY)) {
            return typeManager.getParameterizedType(StandardTypes.ARRAY,
                    ImmutableList.of(TypeSignatureParameter.of(fieldDef.getParType().getTypeSignature())));
        }
        if (supportedTypes.contains(fieldTypeString)) {
            return typeManager.getType(new TypeSignature(fieldTypeString));
        } else {
            throw new UnsupportedOperationException(fieldDef + "Not yet mapped");
        }
    }

    @Override
    public boolean hasTable(SchemaTableName tableName) {
        return tableMap.containsKey(tableName);
    }

    @Override
    public List<CyodaTable> getTables() {
        return cyodaTables;
    }

    @Override
    public @Nullable SupportedDataType<?> getValue(@Nullable T entity, CyodaColumnHandle columnHandle) {
        if ( entity == null ) return null;
        Object value = getFieldValueFromEntity(entity,columnHandle);
        if ( value == null ) return null;
        Object mappedField = mapFieldValue(value,columnHandle);
        return SupportedDataType.ofAny(mappedField,columnHandle.getDataType().getJavaType());
    }

    protected abstract @Nonnull Object mapFieldValue(@Nonnull Object field, CyodaColumnHandle columnHandle);

    protected abstract @Nullable Object getFieldValueFromEntity(@Nonnull T field, CyodaColumnHandle columnHandle);



}
