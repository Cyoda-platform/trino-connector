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
import com.cyoda.presto.client.SupportedDataType;
import com.cyoda.presto.client.paging.PagedIterator;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.neededatcyoda.GridConfigFieldsView;
import com.facebook.presto.common.type.TimestampType;
import com.facebook.presto.spi.SchemaTableName;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.facebook.presto.common.type.VarcharType.createUnboundedVarcharType;
import static com.google.common.base.MoreObjects.toStringHelper;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class ConfiguredReportsApiHandler implements CyodaApiRequestHandler<GridConfigFieldsView> {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguredReportsApiHandler.class);

    public static final String REPORT_DEFS_ENDPOINT = "/api/platform-api/reporting/definitions";
    public static final int DEFAULT_PAGE_SIZE = 10;

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
        // TODO: Replace this hard-coded prototype. The information should come from the Dist Reporting API component.
        ImmutableList.Builder<CyodaColumnHandle> builder = ImmutableList.builder();
        int pos = 0;
        builder.add(new CyodaColumnHandle(connectorId.toString(), "id", createUnboundedVarcharType(), pos++, getHandlerKey()));
        builder.add(new CyodaColumnHandle(connectorId.toString(), "description", createUnboundedVarcharType(), pos++, getHandlerKey()));
        builder.add(new CyodaColumnHandle(connectorId.toString(), "type", createUnboundedVarcharType(), pos++, getHandlerKey()));
        builder.add(new CyodaColumnHandle(connectorId.toString(), "userId", createUnboundedVarcharType(), pos++, getHandlerKey()));
        builder.add(new CyodaColumnHandle(connectorId.toString(), "creationDate", TimestampType.TIMESTAMP, pos, getHandlerKey()));

        URI uri = config.getServerUrl().toURI().resolve(REPORT_DEFS_ENDPOINT);
        List<URI> sources = Collections.singletonList(uri);
        CyodaTable table = new CyodaTable(CyodaStaticReportTable.REPORTS.name(), builder.build(), sources, true);
        SchemaTableName key = new SchemaTableName(config.getSchemaName(), table.getName());
        return Collections.singletonMap(key, table);
    }

    @Nonnull
    public List<CyodaTable> getTables() {
        return cyodaTables;
    }

    @Override
    public Optional<PagedModel<GridConfigFieldsView>> retrievePage(
            Integer pageNum,
            Integer pageSize,
            String query) {

        int page = (pageNum == null ) ? 0 : pageNum;
        int size = (pageSize == null ) ? DEFAULT_PAGE_SIZE : pageSize;

        List<String> selectedFields = Arrays.asList("id","description","type","userId","creationDate");

        UriTemplate uriTemplate = setupUriTemplate(query);

        URI templatedUri = uriTemplate.expand(page, size, selectedFields);

        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(restTemplate);


        PagedModelType<GridConfigFieldsView> typeReference = new PagedModelType<GridConfigFieldsView>() {};

        try {
            final PagedModel<GridConfigFieldsView> gridConfigFieldsViews = traverson
                .follow()
                .toObject(typeReference);
            return Optional.ofNullable(gridConfigFieldsViews);
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this,"retrieveCollection", e, templatedUri);
        }
    }


    private UriTemplate setupUriTemplate(String query) {
        URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(REPORT_DEFS_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        final ImmutableList.Builder<TemplateVariable> builder = ImmutableList.builder();
        builder.add(
                TemplateVariable.requestParameter("page"),
                TemplateVariable.requestParameterContinued("size")//,
                //TemplateVariable.requestParameterContinued("fields")
        );
        if (query!=null && !query.isEmpty()) builder.add(TemplateVariable.requestParameterContinued("filterByType"));
        TemplateVariables vars = new TemplateVariables(builder.build());
        return UriTemplate.of(uri.toASCIIString())
                .with(vars);
    }
    @Override
    public SupportedDataType<?> getValue(GridConfigFieldsView entity, int field) {
        requireNonNull(entity, "entity is null");
        Map<String, String> fields = entity.getGridConfigFields();
        switch (field) {
            case 0:
                return SupportedDataType.of(fields.get("id"), String.class);
            case 1:
                return SupportedDataType.of(fields.get("description"), String.class);
            case 2:
                return SupportedDataType.of(fields.get("type"), String.class);
            case 3:
                return SupportedDataType.of(fields.get("userId"), String.class);
            case 4:
                return SupportedDataType.of(toLocalDateTime(fields.get("creationDate")), LocalDateTime.class);
            default:
                throw new IllegalArgumentException("field index " + field + " is out of bounds. valid is 0..4");
        }
    }

    @Override
    public Iterator<GridConfigFieldsView> getResponseIterator(Integer pageSize, String query) {
        return new PagedIterator<>(this,10,null).iterator();
    }


    private LocalDateTime toLocalDateTime(String str) {
        if (str == null) return null;
        return LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
    }


}
