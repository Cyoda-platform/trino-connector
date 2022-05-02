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

import com.cyoda.core.model.reports.DistributedReportInfoView;
import com.cyoda.core.model.reports.ReportHistoryFieldsView;
import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.client.PagingApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.logic.PredicateNode;
import com.cyoda.presto.client.paging.PagedIterator;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.client.types.SupportedDataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.TypeSignature;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.beans.MetaProperty;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.server.core.TypeReferences;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.cyoda.core.model.reports.ReportHistoryFieldsView.GROUPING_VERSION_COLUMN_NAME;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.REPORT_ID_COLUMN_NAME;
import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;

public class ReportStatisticsApiHandler extends BaseReportsApiHandler<PagedModel<DistributedReportInfoView>,DistributedReportInfoView>
        implements PagingApiRequestHandler<DistributedReportInfoView> {

    @SuppressWarnings("java:S1075")
    public static final String REPORT_STATS_TEMPLATE = "/{" + REPORT_ID_COLUMN_NAME + "}/{" +
            GROUPING_VERSION_COLUMN_NAME + "}/stats{?full}";

    private final ReportHistoryApiHandler reportHistoryApiHandler;

    private static final Map<String,MetaProperty<?>> metaPropertyMap = DistributedReportInfoView.meta().metaPropertyMap();

    private static FieldDefinition[] toFieldDefinition(Map<String,MetaProperty<?>> metaPropertyMap) {

        String[] keys = metaPropertyMap.keySet().toArray(new String[0]);
        return IntStream
                .range(0, metaPropertyMap.size())
                .mapToObj(i -> {
                    String key = keys[i];
                    MetaProperty<?> metaProperty = metaPropertyMap.get(key);
                    String fieldName = metaProperty.name();
                    java.lang.reflect.Type genericType = metaProperty.propertyGenericType();
                    if ( genericType instanceof ParameterizedType ) {
                        ParameterizedType myType = (ParameterizedType) genericType;
                        if (Map.class.isAssignableFrom((Class<?>)myType.getRawType()) ) {
                            java.lang.reflect.Type keyType = myType.getActualTypeArguments()[0];
                            java.lang.reflect.Type valueType = myType.getActualTypeArguments()[1];
                            DataType keyDataType = DataType.fromClass((Class<?>)keyType).orElse(DataType.OBJECT);
                            DataType valueDataType = DataType.fromClass((Class<?>)valueType).orElse(DataType.OBJECT);
                            return new FieldDef(
                                    i,
                                    fieldName,
                                    StandardTypes.MAP,
                                    DataType.MAP,
                                    SupportedDataType.toPrestoTypeSignature(keyDataType),
                                    SupportedDataType.toPrestoTypeSignature(valueDataType)
                            );
                        }
                        if (List.class.isAssignableFrom((Class<?>)myType.getRawType()) ) {
                            java.lang.reflect.Type valueType = myType.getActualTypeArguments()[0];
                            if (! (valueType instanceof Class) ) throw new UnsupportedOperationException("Not done yet");
                            DataType valueDataType = DataType.fromClass((Class<?>) valueType).orElse(DataType.OBJECT);
                            return new FieldDef(i,fieldName, StandardTypes.ARRAY,DataType.LIST,
                                    SupportedDataType.toPrestoTypeSignature(valueDataType),null);
                        }
                        throw new IllegalArgumentException("Not yet done");
                    } else {
                        DataType dataType = DataType.fromClass(metaProperty.propertyType()).orElse(DataType.OBJECT);
                        String fieldTypeString = dataType.getTypeString();
                        return (FieldDefinition) new FieldDef(i,fieldName,fieldTypeString,dataType,null,null);
                    }
                 }).toArray(FieldDefinition[]::new);
    }

    private static final FieldDefinition[] myFieldDefs = toFieldDefinition(metaPropertyMap);

    static class FieldDef implements FieldDefinition {

        private final int pos;
        private final String fieldName;
        private final String fieldTypeString;
        private final DataType dataType;
        private final TypeSignature parType;
        private final TypeSignature mapValueType;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("pos", pos)
                    .add("fieldName", fieldName)
                    .add("fieldTypeString", fieldTypeString)
                    .add("dataType", dataType)
                    .add("parType", parType)
                    .toString();
        }

        FieldDef(int pos, String fieldName, String fieldTypeString, DataType dateType, TypeSignature parType, TypeSignature mapValueType) {
            this.pos = pos;
            this.fieldName = fieldName;
            this.fieldTypeString = fieldTypeString;
            this.dataType = dateType;
            this.parType = parType;
            this.mapValueType = mapValueType;
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

        @Override
        public TypeSignature getMapValuetype() {
            return mapValueType;
        }
    }

    @Inject
    public ReportStatisticsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                      RestTemplateCustomizer restTemplateCustomizer) {
        super(connectorId, config, typeManager,
                REPORT_ENDPOINT, CyodaStaticReportTable.REPORT_STATS.name(), myFieldDefs,restTemplateCustomizer);
        this.reportHistoryApiHandler = new ReportHistoryApiHandler(connectorId,config,typeManager,restTemplateCustomizer);
    }



    @Override
    public String getHandlerKey() {
        return CyodaStaticReportTable.REPORT_STATS.name();
    }

    @Override
    public Optional<PagedModel<DistributedReportInfoView>> retrievePage(int page, int pageSize, List<CyodaColumnHandle> projectedColumns, PredicateNode<Any> predicates) {

        UriTemplate uriTemplate = setupUriTemplate();

        PagedModel<ReportHistoryFieldsView> reportHistoryModel = reportHistoryApiHandler.retrievePage(page, pageSize, projectedColumns, predicates).orElse(PagedModel.empty());

        List<DistributedReportInfoView> reportStatisticsView = getReportStatisticsView(uriTemplate, reportHistoryModel.getContent());
        return Optional.of(PagedModel.of(reportStatisticsView,reportHistoryModel.getMetadata()));
    }

    private List<DistributedReportInfoView> getReportStatisticsView(UriTemplate uriTemplate, @Nonnull Collection<ReportHistoryFieldsView> history) {

        if ( history.isEmpty() ) return Collections.emptyList();
        ImmutableList.Builder<DistributedReportInfoView> builder = ImmutableList.builder();

        history.forEach(element -> {
            String reportId = element.getReportHistoryFields().get(REPORT_ID_COLUMN_NAME).toString();
            String groupingVersion = element.getReportHistoryFields().get(GROUPING_VERSION_COLUMN_NAME).toString();
            URI templatedUri = uriTemplate.expand(
                    ImmutableMap.of(
                            REPORT_ID_COLUMN_NAME, reportId,
                            GROUPING_VERSION_COLUMN_NAME,groupingVersion,
                            "full",true
                    )
            );

            Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
            traverson.setRestOperations(restTemplate);

            TypeReferences.EntityModelType<DistributedReportInfoView> typeReference
                    = new TypeReferences.EntityModelType<DistributedReportInfoView>(){};

            try {
                EntityModel<DistributedReportInfoView> entityModel = traverson
                        .follow()
                        .toObject(typeReference);
                Optional<DistributedReportInfoView> reportStatistics = Optional.ofNullable(entityModel).map(EntityModel::getContent);
                builder.add(reportStatistics.orElse(DistributedReportInfoView.builder().id(reportId).build()));
            } catch (HttpClientErrorException e) {
                throw requestFailedException(this, "retrieveCollection", e, templatedUri);
            }
        });
        List<DistributedReportInfoView> result = builder.build();
        LOG.debug("Got %s report definitions",result.size());
        return result;

    }

    private UriTemplate setupUriTemplate() {

        // /report/{id}/{grouping_version}/stats
        URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(REPORT_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new PrestoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, e);
        }
        return UriTemplate.of(uri.toASCIIString()+ REPORT_STATS_TEMPLATE);
    }

    @Nonnull
    @Override
    protected Object mapFieldValue(@Nonnull Object field, CyodaColumnHandle columnHandle) {
        return field;
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull DistributedReportInfoView field, CyodaColumnHandle columnHandle) {
        MetaProperty<?> metaProperty = field.metaBean().metaPropertyMap().get(columnHandle.getColumnName());
        if ( metaProperty == null ) {
            throw new IllegalArgumentException(columnHandle.getColumnName()+" is not defined on ReportDefinitionsView");
        }
        return field.metaBean().metaProperty(columnHandle.getColumnName()).get(field);
    }

    @Override
    public Iterator<DistributedReportInfoView> getResponseIterator(
            int pageSize,
            CyodaTableHandle cyodaTableHandle,
            PredicateNode<Any> predicates
    ) {
        return new PagedIterator<>(this, pageSize, cyodaTableHandle, predicates).iterator();
    }

}