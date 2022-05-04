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

package com.cyoda.presto.client.reporting.groups;

import com.cyoda.core.model.reports.DistributedReportInfoView;
import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.client.ApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.jodabeans.StandardColumnDefinition;
import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.logic.Connective;
import com.cyoda.presto.client.logic.LeafPredicateNode;
import com.cyoda.presto.client.logic.PredicateBuilder;
import com.cyoda.presto.client.logic.PredicateNode;
import com.cyoda.presto.client.reporting.BaseReportsApiHandler;
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.client.reporting.CyodaStaticReportTable;
import com.cyoda.presto.client.reporting.ReportStatisticsApiHandler;
import com.cyoda.presto.client.types.SupportedDataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.service.api.beans.GroupHeader;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.VarcharType;
import io.airlift.slice.Slice;
import org.joda.beans.MetaProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.StreamSupport;

import static com.cyoda.presto.client.reporting.groups.InternalReportGroupsApiHandler.GROUPING_VERSION_COLUMN_NAME;
import static com.cyoda.presto.client.reporting.groups.InternalReportGroupsApiHandler.REPORT_ID_COLUMN_NAME;
import static com.cyoda.presto.client.types.DataType.STRING;
import static com.cyoda.presto.client.types.DataType.UUID_TYPE;

public class ReportGroupsApiHandler extends BaseReportsApiHandler<GroupingHandle>
        implements ApiRequestHandler<GroupingHandle> {

    private final ReportStatisticsApiHandler statisticsApiHandler;
    private final InternalReportGroupsApiHandler reportGroupsHandler;

    private static final List<ColumnDefinition> COLUMN_DEFS = StandardColumnDefinition.builder()
            .add(new StandardColumnDefinition(0,REPORT_ID_COLUMN_NAME, StandardTypes.VARCHAR, STRING, null, null))
            .add(new StandardColumnDefinition(0,GROUPING_VERSION_COLUMN_NAME, StandardTypes.VARCHAR, UUID_TYPE, null, null))
            .add(GroupHeader.meta())
            .build();

    private final CyodaColumnHandle reportIdColumn;
    private final CyodaColumnHandle groupingVersionColumn;

    @Inject
    public ReportGroupsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                  RestTemplateCustomizer restTemplateCustomizer) {
        super(connectorId, config, typeManager,
                REPORT_ENDPOINT, CyodaStaticReportTable.REPORT_GROUPS.name(), COLUMN_DEFS,restTemplateCustomizer);
        this.statisticsApiHandler = new ReportStatisticsApiHandler(connectorId,config,typeManager,restTemplateCustomizer);
        this.reportGroupsHandler = new InternalReportGroupsApiHandler(connectorId,config,typeManager,restTemplateCustomizer);
        this.reportIdColumn = reportGroupsHandler.getTables().get(0).getColumns().stream()
                .filter(it -> it.getColumnName().equals(REPORT_ID_COLUMN_NAME))
                .findAny()
                .orElseThrow(()->new IllegalStateException("Report ID Column not found!"));
        this.groupingVersionColumn = reportGroupsHandler.getTables().get(0).getColumns().stream()
                .filter(it -> it.getColumnName().equals(GROUPING_VERSION_COLUMN_NAME))
                .map(it->new CyodaColumnHandle(
                        it.getConnectorId(),
                        it.getColumnName(),
                        VarcharType.VARCHAR, // This is because Presto cannot deal with UUID, even if there is a UuidType.
                        STRING,
                        it.getOrdinalPosition(),
                        it.getRequestHandlerKey(),
                        it.getIsNullable())
                ).findAny()
                .orElseThrow(()->new IllegalStateException("Column not found!"));
    }

    @Override
    public String getHandlerKey() {
        return CyodaStaticReportTable.REPORT_GROUPS.name();
    }

    protected Iterable<GroupingHandle> groupsIterator(
            int pageSize,
            List<CyodaColumnHandle> projectedColumns,
            @Nonnull DistributedReportInfoView stats
    ) {
        String reportId = stats.getId();
        String groupingVersion = stats.getGroupingVersion().toString();

        if (stats.getGroupsCount() == 0 ) return Collections::emptyIterator;

        Slice reportIdSlice = SupportedDataType.of(reportId,String.class).asSlice(VarcharType.VARCHAR);
        Slice groupingVersionSlice = SupportedDataType.of(groupingVersion,String.class).asSlice(VarcharType.VARCHAR);
        List<PredicateNode<?>> members = Arrays.asList(
                LeafPredicateNode.leaf(PredicateBuilder.createEqualsPredicate(reportIdColumn, reportIdSlice)),
                LeafPredicateNode.leaf(PredicateBuilder.createEqualsPredicate(groupingVersionColumn, groupingVersionSlice))
        );
        PredicateNode<Any> predicates = CompoundPredicateNode.of(members, Connective.AND);

        return () -> reportGroupsHandler.getResponseIterator(pageSize, projectedColumns, predicates);
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull GroupingHandle field, CyodaColumnHandle columnHandle) {
        if ( REPORT_ID_COLUMN_NAME.equals(columnHandle.getColumnName()) ) {
            return field.reportId;
        }
        if ( GROUPING_VERSION_COLUMN_NAME.equals(columnHandle.getColumnName()) ) {
            return field.groupingVersion;
        }
        MetaProperty<?> metaProperty = field.groupHeader.metaBean().metaPropertyMap().get(columnHandle.getColumnName());
        if ( metaProperty == null ) {
            throw new IllegalArgumentException(columnHandle.getColumnName()+" is not defined on ReportDefinitionsView");
        }
        return field.groupHeader.metaBean().metaProperty(columnHandle.getColumnName()).get(field.groupHeader);
    }

    @Override
    protected @Nonnull Object mapFieldValue(@Nonnull Object value, CyodaColumnHandle columnHandle) {
        return value;
    }


    @Override
    public Iterator<GroupingHandle> getResponseIterator(
            int pageSize,
            List<CyodaColumnHandle> projectedColumns,
            PredicateNode<Any> predicates
    ) {
        Iterable<DistributedReportInfoView> statsIterable = () -> statisticsApiHandler
                .getResponseIterator(pageSize, projectedColumns, predicates);

        return StreamSupport.stream(statsIterable.spliterator(),true)
                .flatMap(it->
                        StreamSupport.stream(groupsIterator(pageSize,projectedColumns,it).spliterator(), true)
                ).iterator();
    }


}
