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
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.ApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.jodabeans.StandardColumnDefinition;
import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.logic.ColumnPredicateNode;
import com.cyoda.presto.client.logic.ColumnPredicateUtils;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.logic.Connective;
import com.cyoda.presto.client.logic.converters.PrestoValueConverter;
import com.cyoda.presto.client.logic.converters.PrestoValueConverterProvider;
import com.cyoda.presto.client.logic.converters.impl.UUIDPrestoValueConverter;
import com.cyoda.presto.client.reporting.BaseReportsApiHandler;
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.client.reporting.meta.ReportStatisticsApiHandler;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.cyoda.service.api.beans.GroupHeader;
import com.facebook.presto.common.type.StandardTypes;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.VarcharType;
import io.airlift.slice.Slice;
import org.joda.beans.MetaProperty;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_REPORT_NAME_VARIABLE;
import static com.cyoda.presto.client.reporting.AbstractTableHolder.TableDefinitionHandle.asTableDefinitionHandle;
import static com.cyoda.presto.client.reporting.CyodaStaticReportTable.REPORT_GROUPS;
import static com.cyoda.presto.client.types.DataType.STRING;
import static com.cyoda.presto.client.types.DataType.UUID_TYPE;

public class ReportGroupsApiHandler extends BaseReportsApiHandler<GroupingHandle>
        implements ApiRequestHandler<GroupingHandle> {

    private static final SupplierLogger LOG = SupplierLogger.get(ReportGroupsApiHandler.class);

    public static final String COLUMN_NOT_FOUND = " Column not found!";
    private final ReportStatisticsApiHandler statisticsApiHandler;
    private final InternalReportGroupsApiHandler reportGroupsHandler;
    private final Function<AuthContext, ColumnsHolder> columnsHolderFunction;


    public static final String GROUPING_VERSION_COLUMN = "groupingVersion";
    public static final String GROUPING_PARENT_COLUMN = "group_json";
    public static final String GROUPING_REPORT_CONFIG_ID_COLUMN = HISTORY_REPORT_NAME_VARIABLE;

    private static final List<ColumnDefinition> COLUMN_DEFS = StandardColumnDefinition.builder()
            .add(new StandardColumnDefinition(0, HISTORY_REPORT_ID_COLUMN, StandardTypes.VARCHAR, STRING, null, null))
            .add(new StandardColumnDefinition(0, GROUPING_VERSION_COLUMN, StandardTypes.VARCHAR, UUID_TYPE, null, null))
            .add(new StandardColumnDefinition(0, GROUPING_REPORT_CONFIG_ID_COLUMN, StandardTypes.VARCHAR, STRING, null, null))
            .add(GroupHeader.meta())
            .build();


    @Inject
    public ReportGroupsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                  RestTemplateCustomizer restTemplateCustomizer) {
        super(connectorId, config, typeManager, REPORT_ENDPOINT,restTemplateCustomizer,LOG);
        this.statisticsApiHandler = new ReportStatisticsApiHandler(connectorId,config,typeManager,restTemplateCustomizer);
        this.reportGroupsHandler = new InternalReportGroupsApiHandler(connectorId,config,typeManager,restTemplateCustomizer);

        columnsHolderFunction = columnsHolderFunction(reportGroupsHandler,statisticsApiHandler);
    }

    private Function<AuthContext, ColumnsHolder> columnsHolderFunction(InternalReportGroupsApiHandler reportGroupsHandler,
                                                                       ReportStatisticsApiHandler statisticsApiHandler) {
        return authPayload -> new ColumnsHolder(authPayload,reportGroupsHandler,statisticsApiHandler);
    }

    @Override
    protected Map<TableDefinitionHandle, List<ColumnDefinition>> refreshFieldDefs(AuthContext authContext) {
        return Collections.singletonMap(asTableDefinitionHandle(REPORT_GROUPS.name()), COLUMN_DEFS);
    }

    @Override
    public String getHandlerKey() {
        return REPORT_GROUPS.name();
    }

    protected Iterable<GroupingHandle> groupsIterator(
            AuthContext authContext,
            int pageSize,
            CyodaTableHandle tableHandle,
            ColumnPredicateNode<Any> predicates,
            @Nonnull DistributedReportInfoView stats,
            SizeListener listener
    ) {
        if (stats.getGroupsCount() == 0 ) return Collections::emptyIterator;
        CompoundPredicateNode thesePredicates = getCompoundPredicateNode(tableHandle, predicates, stats);
        return () -> reportGroupsHandler.asFlux(authContext,pageSize, tableHandle, thesePredicates,listener).toIterable().iterator();
    }

    private CompoundPredicateNode getCompoundPredicateNode(CyodaTableHandle tableHandle, ColumnPredicateNode<Any> predicates, DistributedReportInfoView stats) {
        String reportId = stats.getId();
        UUID groupingVersion = stats.getGroupingVersion();
        String reportConfigId = stats.getConfigName();

        PrestoValueConverter<UUID> uuidConverter = PrestoValueConverterProvider.getPrestoValueConverter(UUID_TYPE.asSupported());
        PrestoValueConverter<String> stringConverter = PrestoValueConverterProvider.getPrestoValueConverter(STRING.asSupported());

        Slice reportIdSlice = stringConverter.toSlice(VarcharType.VARCHAR,reportId);
        Slice groupingVersionSlice = uuidConverter.toSlice(UUIDPrestoValueConverter.TYPE,groupingVersion);
        Slice reportConfigIdSlice = stringConverter.toSlice(VarcharType.VARCHAR,reportConfigId);

        ColumnsHolder columnsHolder = columnsHolderFunction.apply(tableHandle.getAuthPayload());
        CompoundPredicateNode.Builder builder = CompoundPredicateNode.builder(Connective.AND);
        builder.addLeaf(ColumnPredicateUtils.newEqualsPredicate(columnsHolder.reportIdColumn, reportIdSlice,String.class));
        builder.addLeaf(ColumnPredicateUtils.newEqualsPredicate(columnsHolder.groupingVersionColumn, groupingVersionSlice,UUID.class));
        builder.addLeaf(ColumnPredicateUtils.newEqualsPredicate(columnsHolder.reportConfigurationIdColumn, reportConfigIdSlice,String.class));
        builder.addMember(predicates);

        return builder.build();
    }

    private Flux<GroupingHandle> internalFlux(
            AuthContext authContext,
            int pageSize,
            CyodaTableHandle tableHandle,
            ColumnPredicateNode<Any> predicates,
            @Nonnull DistributedReportInfoView stats,
            SizeListener listener
    ) {
        if (stats.getGroupsCount() == 0 ) return Flux.empty();
        CompoundPredicateNode thesePredicates = getCompoundPredicateNode(tableHandle, predicates, stats);
        return reportGroupsHandler.asFlux(authContext,pageSize, tableHandle, thesePredicates,listener);
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull GroupingHandle field, CyodaColumnHandle columnHandle) {
        if ( HISTORY_REPORT_ID_COLUMN.equals(columnHandle.getColumnName()) ) {
            return field.reportId;
        }
        if ( GROUPING_VERSION_COLUMN.equals(columnHandle.getColumnName()) ) {
            return field.groupingVersion;
        }
        if ( HISTORY_REPORT_NAME_VARIABLE.equals(columnHandle.getColumnName()) ) {
            return field.reportConfigId;
        }
        MetaProperty<?> metaProperty = field.groupHeader.metaBean().metaPropertyMap().get(columnHandle.getColumnName());
        if ( metaProperty == null ) {
            throw new IllegalArgumentException(columnHandle.getColumnName()+" is not defined on ReportDefinitionsView");
        }
        return field.groupHeader.metaBean().metaProperty(columnHandle.getColumnName()).get(field.groupHeader);
    }

    @Override
    protected @Nonnull Object mapFieldValue(@Nonnull final Object value, CyodaColumnHandle columnHandle) {
        return super.mapFieldValue(value,columnHandle);
    }


    @Override
    public Iterator<GroupingHandle> getResponseIterator(
            AuthContext authContext,
            int pageSize,
            CyodaTableHandle tableHandle,
            CompoundPredicateNode predicates,
            SizeListener listener
    ) {
        logCreation(pageSize, tableHandle, predicates, LOG);
        Iterable<DistributedReportInfoView> statsIterable = () -> statisticsApiHandler
                .getResponseIterator(authContext,pageSize, tableHandle, predicates,listener);

        return StreamSupport.stream(statsIterable.spliterator(),true)
                .flatMap(it->
                        StreamSupport.stream(groupsIterator(authContext,pageSize,tableHandle,predicates,it,listener).spliterator(), true)
                ).iterator();
    }

    @Override
    public Flux<GroupingHandle> asFlux(AuthContext authContext, int pageSize, CyodaTableHandle tableHandle, CompoundPredicateNode predicates, SizeListener listener) {
        logCreation(pageSize, tableHandle, predicates, LOG);
        Flux<DistributedReportInfoView> statsFlux = statisticsApiHandler
                .asFlux(authContext, pageSize, tableHandle, predicates, listener);
        return statsFlux.flatMap(stats -> internalFlux(authContext,pageSize,tableHandle,predicates,stats,listener));
    }

    static class ColumnsHolder {
        private final CyodaColumnHandle reportIdColumn;
        private final CyodaColumnHandle groupingVersionColumn;
        private final CyodaColumnHandle reportConfigurationIdColumn;

        ColumnsHolder(AuthContext authContext, InternalReportGroupsApiHandler reportGroupsHandler, ReportStatisticsApiHandler statisticsApiHandler) {
            this.reportIdColumn = setupReportIdColumn(authContext, reportGroupsHandler);
            this.groupingVersionColumn = setupGroupingVersionColumn(authContext, reportGroupsHandler);
            this.reportConfigurationIdColumn = setupReportConfigIdColumn(authContext, statisticsApiHandler);
        }

        private CyodaColumnHandle setupReportConfigIdColumn(AuthContext authContext, ReportStatisticsApiHandler statisticsApiHandler) {
            return statisticsApiHandler.getTables(authContext).get(0).getColumns().stream()
                    .filter(it -> it.getColumnName().equals(HISTORY_REPORT_NAME_VARIABLE))
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException(HISTORY_REPORT_NAME_VARIABLE + COLUMN_NOT_FOUND));
        }

        private CyodaColumnHandle setupGroupingVersionColumn(AuthContext authContext, InternalReportGroupsApiHandler reportGroupsHandler) {
            return reportGroupsHandler.getTables(authContext).get(0).getColumns().stream()
                    .filter(it -> it.getColumnName().equals(GROUPING_VERSION_COLUMN))
                    .map(it -> new CyodaColumnHandle(
                            it.getConnectorId(),
                            it.getColumnName(),
                            VarcharType.VARCHAR,
                            UUID_TYPE,
                            it.getOrdinalPosition(),
                            it.getRequestHandlerKey(),
                            it.getIsNullable())
                    ).findAny()
                    .orElseThrow(() -> new IllegalStateException(GROUPING_VERSION_COLUMN + COLUMN_NOT_FOUND));
        }

        private CyodaColumnHandle setupReportIdColumn(AuthContext authContext, InternalReportGroupsApiHandler reportGroupsHandler) {
            return reportGroupsHandler.getTables(authContext).get(0).getColumns().stream()
                    .filter(it -> it.getColumnName().equals(HISTORY_REPORT_ID_COLUMN))
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException(HISTORY_REPORT_ID_COLUMN + COLUMN_NOT_FOUND));
        }
    }

}
