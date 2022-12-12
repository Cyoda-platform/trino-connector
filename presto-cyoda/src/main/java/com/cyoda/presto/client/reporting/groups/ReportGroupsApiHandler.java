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
import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.logic.ColumnPredicateNode;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.logic.Connective;
import com.cyoda.presto.client.reporting.BaseReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportStatisticsApiHandler;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportMetadataProvider;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import io.trino.spi.type.TypeManager;
import org.joda.beans.MetaProperty;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.UUID;

import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_REPORT_NAME_VARIABLE;

public class ReportGroupsApiHandler extends BaseReportsApiHandler<GroupingHandle>
        implements ApiRequestHandler<GroupingHandle> {

    private static final SupplierLogger LOG = SupplierLogger.get(ReportGroupsApiHandler.class);
    private final ReportStatisticsApiHandler statisticsApiHandler;
    private final InternalReportGroupsApiHandler reportGroupsHandler;
    private final CyodaColumnHandle reportIdColumn;
    private final CyodaColumnHandle groupingVersionColumn;
    private final CyodaColumnHandle reportConfigurationIdColumn;


    public static final String GROUPING_VERSION_COLUMN = "groupingVersion";
    public static final String GROUPING_PARENT_COLUMN = "group_json";
    public static final String GROUPING_REPORT_CONFIG_ID_COLUMN = HISTORY_REPORT_NAME_VARIABLE;


    @Inject
    public ReportGroupsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                  RestTemplateCustomizer restTemplateCustomizer,
                                  StaticReportMetadataProvider staticMetaProvider,
                                  ReportStatisticsApiHandler reportStatisticsApiHandler,
                                  InternalReportGroupsApiHandler internalReportGroupsApiHandler) {
        super(connectorId, config, typeManager, restTemplateCustomizer, LOG);
        this.statisticsApiHandler = reportStatisticsApiHandler;
        this.reportGroupsHandler = internalReportGroupsApiHandler;
        reportIdColumn = staticMetaProvider.getReportGroups().getReportIdColumn();
        groupingVersionColumn = staticMetaProvider.getReportGroups().getGroupingVersionColumn();
        reportConfigurationIdColumn = staticMetaProvider.getReportGroups().getReportConfigIdColumn();
    }


    private CompoundPredicateNode getCompoundPredicateNode(ColumnPredicateNode<Any> predicates, DistributedReportInfoView stats) {
        String reportId = stats.getId();
        UUID groupingVersion = stats.getGroupingVersion();
        String reportConfigId = stats.getConfigName();

        CompoundPredicateNode.Builder builder = CompoundPredicateNode.builder(Connective.AND);
        builder.addLeaf(reportIdColumn.newEqualsPredicateFromJava(reportId));
        builder.addLeaf(groupingVersionColumn.newEqualsPredicateFromJava(groupingVersion));
        builder.addLeaf(reportConfigurationIdColumn.newEqualsPredicateFromJava(reportConfigId));
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
        if (stats.getGroupsCount() == 0) return Flux.empty();
        CompoundPredicateNode thesePredicates = getCompoundPredicateNode(predicates, stats);
        return reportGroupsHandler.asFlux(authContext, tableHandle, thesePredicates, listener);
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull GroupingHandle field, CyodaColumnHandle columnHandle) {
        if (HISTORY_REPORT_ID_COLUMN.equals(columnHandle.getColumnName())) {
            return field.reportId;
        }
        if (GROUPING_VERSION_COLUMN.equals(columnHandle.getColumnName())) {
            return field.groupingVersion;
        }
        if (HISTORY_REPORT_NAME_VARIABLE.equals(columnHandle.getColumnName())) {
            return field.reportConfigId;
        }
        MetaProperty<?> metaProperty = field.groupHeader.metaBean().metaPropertyMap().get(columnHandle.getColumnName());
        if (metaProperty == null) {
            throw new IllegalArgumentException(columnHandle.getColumnName() + " is not defined on ReportDefinitionsView");
        }
        return field.groupHeader.metaBean().metaProperty(columnHandle.getColumnName()).get(field.groupHeader);
    }

    @Override
    public Flux<GroupingHandle> asFlux(AuthContext authContext, CyodaTableHandle tableHandle, CompoundPredicateNode predicates, SizeListener listener) {
        int pageSize = getPageSize();
        logCreation(pageSize, tableHandle, predicates, LOG);
        Flux<DistributedReportInfoView> statsFlux = statisticsApiHandler
                .asFlux(authContext, tableHandle, predicates, listener);
        return statsFlux.flatMap(stats -> internalFlux(authContext, pageSize, tableHandle, predicates, stats, listener));
    }

}
