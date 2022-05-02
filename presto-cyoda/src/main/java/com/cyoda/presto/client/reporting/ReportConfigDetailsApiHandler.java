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

import com.cyoda.api.view.GridConfigFieldsView;
import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.client.PagingApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.logic.PredicateNode;
import com.cyoda.presto.client.neededatcyoda.ReportDefinitionsView;
import com.cyoda.presto.client.paging.PagedIterator;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.facebook.airlift.json.JsonCodec;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.TypeManager;
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
import java.util.stream.Collectors;

import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.neededatcyoda.ReportDefinitionsView.*;
import static com.cyoda.presto.client.types.DataType.STRING;

public class ReportConfigDetailsApiHandler extends BaseReportsApiHandler<PagedModel<ReportDefinitionsView>,ReportDefinitionsView>
        implements PagingApiRequestHandler<ReportDefinitionsView> {

    private final ConfiguredReportsApiHandler configuredReportsApiHandler;

    enum FieldDef implements FieldDefinition {
        ID(0, REPORT_ID_COLUMN_NAME, StandardTypes.VARCHAR, STRING),
        REPORT_NAME(1, REPORT_NAME_COLUMN_NAME, StandardTypes.VARCHAR, STRING),
        REPORT_JSON(2, REPORT_JSON_COLUMN_NAME, StandardTypes.JSON, STRING);

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("pos", pos)
                    .add("fieldName", fieldName)
                    .add("fieldTypeString", fieldTypeString)
                    .add("dataType", dataType)
                    .toString();
        }

        private final int pos;
        private final String fieldName;
        private final String fieldTypeString;
        private final DataType dataType;

        FieldDef(int pos, String fieldName, String fieldTypeString, DataType dateType) {
            this.pos = pos;
            this.fieldName = fieldName;
            this.fieldTypeString = fieldTypeString;
            this.dataType = dateType;
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
    }

    @Inject
    public ReportConfigDetailsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                         RestTemplateCustomizer restTemplateCustomizer) {
        super(connectorId, config, typeManager,
                REPORT_DETAILS_ENDPOINT, CyodaStaticReportTable.REPORT_DETAILS.name(), FieldDef.values(),restTemplateCustomizer);
        this.configuredReportsApiHandler = new ConfiguredReportsApiHandler(connectorId,config,typeManager,restTemplateCustomizer);
    }



    @Override
    public String getHandlerKey() {
        return CyodaStaticReportTable.REPORT_DETAILS.name();
    }

    @Override
    public Optional<PagedModel<ReportDefinitionsView>> retrievePage(int page, int pageSize, List<CyodaColumnHandle> projectedColumns, PredicateNode<Any> predicates) {

        UriTemplate uriTemplate = setupUriTemplate();

        PagedModel<GridConfigFieldsView> reportDefinitionModel = configuredReportsApiHandler.retrievePage(page, pageSize, projectedColumns, predicates).orElse(PagedModel.empty());
        Set<String> ids = reportDefinitionModel.getContent().stream().map(it -> it.getGridConfigFields().get(REPORT_ID_COLUMN_NAME)).collect(Collectors.toSet());

        List<ReportDefinitionsView> reportDefinitionsViews = getReportDefinitionsViews(uriTemplate, ids);
        return Optional.of(PagedModel.of(reportDefinitionsViews,reportDefinitionModel.getMetadata()));
    }

    private List<ReportDefinitionsView> getReportDefinitionsViews(UriTemplate uriTemplate, @Nonnull Set<String> ids) {

        if ( ids.isEmpty() ) return Collections.emptyList();
        ImmutableList.Builder<ReportDefinitionsView> builder = ImmutableList.builder();
        ids.forEach(reportId -> {
            URI templatedUri = uriTemplate.expand(Collections.singletonMap(REPORT_ID_COLUMN_NAME, reportId));

            Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
            traverson.setRestOperations(restTemplate);

            try {
                @SuppressWarnings("unchecked")
                Map<String,?> map = Optional.ofNullable(traverson
                                .follow()
                                .toObject(Map.class))
                        .orElse(Collections.emptyMap());
                String id = null;
                String repName = null;
                @SuppressWarnings("unchecked")
                Map<String,Object> repDef = Optional.ofNullable((Map<String,Object>) map.get("content")).orElse(Collections.emptyMap());
                String json = JsonCodec.mapJsonCodec(String.class, Object.class).toJson(repDef);
                builder.add(new ReportDefinitionsView(id, repName, json));
            } catch (HttpClientErrorException e) {
                throw requestFailedException(this, "retrieveCollection", e, templatedUri);
            }
        });
        List<ReportDefinitionsView> result = builder.build();
        LOG.debug("Got %s report definitions",result.size());
        return result;

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
    protected Object mapFieldValue(@Nonnull Object field, CyodaColumnHandle columnHandle) {
        return field;
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull ReportDefinitionsView field, CyodaColumnHandle columnHandle) {
        if ( REPORT_ID_COLUMN_NAME.equals(columnHandle.getColumnName())) {
            return field.id;
        }
        if ( REPORT_NAME_COLUMN_NAME.equals(columnHandle.getColumnName())) {
            return field.reportName;
        }
        if ( REPORT_JSON_COLUMN_NAME.equals(columnHandle.getColumnName())) {
            return field.json;
        }
        throw new IllegalArgumentException(columnHandle.getColumnName()+" is not defined on ReportDefinitionsView");
    }

    @Override
    public Iterator<ReportDefinitionsView> getResponseIterator(
            int pageSize,
            CyodaTableHandle cyodaTableHandle,
            PredicateNode<Any> predicates
    ) {
        return new PagedIterator<>(this, pageSize, cyodaTableHandle, predicates).iterator();
    }

}
