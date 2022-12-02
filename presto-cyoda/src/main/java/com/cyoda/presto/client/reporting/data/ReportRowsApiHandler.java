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

package com.cyoda.presto.client.reporting.data;

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
import com.cyoda.presto.client.reporting.groups.GroupingHandle;
import com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportMetadataProvider;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.type.TypeManager;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.UUID;

public class ReportRowsApiHandler extends BaseReportsApiHandler<RowHandle>
        implements ApiRequestHandler<RowHandle> {

    private static final SupplierLogger LOG = SupplierLogger.get(ReportRowsApiHandler.class);

    public static final String ROW_REPORT_ROW_NUMBER_COLUMN = "rowNum";
    public static final String ROW_REPORT_ID_COLUMN = "reportId";
    public static final String ROW_GROUPING_VERSION_COLUMN = "groupingVersion";
    public static final String ROW_GROUP_JSON_BASE64_VARIABLE = "groupValuesJsonBase64";

    private final ReportGroupsApiHandler groupsApiHandler;
    private final InternalReportRowsApiHandler internalReportRowsApiHandler;

    private final StaticReportMetadataProvider staticReportMetadataProvider;

    @Inject
    public ReportRowsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                RestTemplateCustomizer restTemplateCustomizer,
                                StaticReportMetadataProvider staticReportMetadataProvider,
                                ReportGroupsApiHandler reportGroupsApiHandler,
                                InternalReportRowsApiHandler internalReportRowsApiHandler) {
        super(connectorId, config, typeManager, restTemplateCustomizer, LOG);
        this.staticReportMetadataProvider = staticReportMetadataProvider;
        this.groupsApiHandler = reportGroupsApiHandler;
        this.internalReportRowsApiHandler = internalReportRowsApiHandler;
    }


    protected Flux<RowHandle> internalFlux(
            AuthContext authContext,
            int pageSize,
            CyodaTableHandle tableHandle,
            ColumnPredicateNode<Any> withReportPredicate,
            @Nonnull GroupingHandle handle,
            SizeListener listener
    ) {
        String groupValueJsonBase64 = handle.groupHeader.getGroupValuesJsonBase64();
        if (groupValueJsonBase64 == null) return Flux.empty();
        CompoundPredicateNode predicates = getCompoundPredicateNodeForInternal(tableHandle, withReportPredicate, handle);

        return internalReportRowsApiHandler.asFlux(authContext, tableHandle, predicates, listener);
    }

    private CompoundPredicateNode getCompoundPredicateNodeForInternal(CyodaTableHandle tableHandle, ColumnPredicateNode<Any> predicates, GroupingHandle handle) {
        String reportId = handle.reportId;
        UUID groupingVersion = handle.groupingVersion;
        String groupValueJsonBase64 = handle.groupHeader.getGroupValuesJsonBase64();

        CompoundPredicateNode.Builder builder = CompoundPredicateNode.builder(Connective.AND);
        builder.addLeaf(staticReportMetadataProvider.getReportGroups().getReportIdColumn().newEqualsPredicateFromJava(reportId));
        builder.addLeaf(staticReportMetadataProvider.getReportGroups().getGroupingVersionColumn().newEqualsPredicateFromJava(groupingVersion));
        builder.addLeaf(staticReportMetadataProvider.getReportGroups().getGroupJsonBase64Column().newEqualsPredicateFromJava(groupValueJsonBase64));
        builder.addMember(predicates);
        return builder.build();
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull RowHandle field, CyodaColumnHandle columnHandle) {
        if (ROW_REPORT_ROW_NUMBER_COLUMN.equals(columnHandle.getColumnName())) {
            return field.rowNum;
        }
        if (ROW_REPORT_ID_COLUMN.equals(columnHandle.getColumnName())) {
            return field.reportId;
        }
        if (ROW_GROUPING_VERSION_COLUMN.equals(columnHandle.getColumnName())) {
            return field.groupingVersion;
        }
        if (ROW_GROUP_JSON_BASE64_VARIABLE.equals(columnHandle.getColumnName())) {
            return field.groupJsonBase64;
        }
        return columnHandle.getValue(field.reportRow);
    }

    @Override
    public Flux<RowHandle> asFlux(
            AuthContext authContext,
            CyodaTableHandle tableHandle,
            CompoundPredicateNode predicates,
            SizeListener listener
    ) {
        int pageSize = getPageSize();
        logCreation(pageSize, tableHandle, predicates, LOG);
        CompoundPredicateNode withReportPredicate = getCompoundPredicateNode(tableHandle, predicates);

        Flux<GroupingHandle> groupsFlux = groupsApiHandler
                .asFlux(authContext, tableHandle, withReportPredicate, listener);

        return groupsFlux.flatMap(it -> internalFlux(authContext, pageSize, tableHandle, withReportPredicate, it, listener));
    }

    private CompoundPredicateNode getCompoundPredicateNode(CyodaTableHandle tableHandle, CompoundPredicateNode predicates) {
        SchemaTableName key = new SchemaTableName(tableHandle.getSchemaName(), tableHandle.getTableName());
        log.debug(() -> "table key is " + key);
        String reportConfigurationId = tableHandle.getReportConfigId();
        CompoundPredicateNode.Builder builder = CompoundPredicateNode.builder(Connective.AND);
        builder.addMember(predicates);

        builder.addLeaf(staticReportMetadataProvider.getReportGroups().getReportConfigIdColumn().newEqualsPredicateFromJava(reportConfigurationId));
        return builder.build();
    }
}

