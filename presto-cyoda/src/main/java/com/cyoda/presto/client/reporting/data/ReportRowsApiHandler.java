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
import com.cyoda.presto.CyodaTable;
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.ApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.jodabeans.StandardColumnDefinition;
import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.logic.ColumnPredicateNode;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.logic.Connective;
import com.cyoda.presto.client.reporting.BaseReportsApiHandler;
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.client.reporting.CyodaStaticReportTable;
import com.cyoda.presto.client.reporting.groups.GroupingHandle;
import com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle;
import com.cyoda.presto.client.types.TypesUtil;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.TypeSignature;
import com.facebook.presto.spi.SchemaTableName;
import com.facebook.presto.spi.TableNotFoundException;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.presto.SizeListener.NOT_LISTENING;
import static com.cyoda.presto.client.reporting.AbstractTableHolder.TableDefinitionHandle.asTableDefinitionHandle;
import static com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler.GROUPING_REPORT_CONFIG_ID_COLUMN;
import static com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler.GROUPING_VERSION_COLUMN;
import static com.cyoda.presto.client.types.DataType.LONG;
import static com.cyoda.presto.client.types.DataType.STRING;

public class ReportRowsApiHandler extends BaseReportsApiHandler<RowHandle>
        implements ApiRequestHandler<RowHandle> {

    private static final SupplierLogger LOG = SupplierLogger.get(ReportRowsApiHandler.class);

    static final String ROW_REPORT_ROW_NUMBER_COLUMN = "rowNum";
    static final String ROW_REPORT_ID_COLUMN = "reportId";
    static final String ROW_GROUPING_VERSION_COLUMN = "groupingVersion";
    static final String ROW_GROUP_JSON_BASE64_VARIABLE = "groupValuesJsonBase64";

    // These are also reserved words for column names coming from reports.
    private static final List<String> RESERVED_COLUMN_NAMES = ImmutableList.<String>builder()
            .add(ROW_REPORT_ID_COLUMN)
            .add(ROW_GROUPING_VERSION_COLUMN)
            .add(ROW_GROUP_JSON_BASE64_VARIABLE)
            .build();
    public static final String COLUMN_NOT_FOUND = " Column not found!";

    private final ReportGroupsApiHandler groupsApiHandler;
    private final InternalReportRowsApiHandler internalReportRowsApiHandler;

    private final ReportConfigDetailsApiHandler reportConfigDetailsHandler;
    private final Function<AuthContext,Flux<ReportDefinitionHandle>> fluxFunction;
    private final Function<AuthContext,ColumnsHolder> columnsHolderFunction;

    @Inject
    public ReportRowsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                  RestTemplateCustomizer restTemplateCustomizer) {
        super(connectorId, config, typeManager, REPORT_ENDPOINT,restTemplateCustomizer,LOG);

        this.groupsApiHandler = new ReportGroupsApiHandler(connectorId,config,typeManager,restTemplateCustomizer);
        this.internalReportRowsApiHandler = new InternalReportRowsApiHandler(connectorId,config,typeManager,restTemplateCustomizer);
        this.reportConfigDetailsHandler = new ReportConfigDetailsApiHandler(connectorId,config,typeManager,restTemplateCustomizer);

        this.fluxFunction = handleFlux();
        this.columnsHolderFunction = columnsHolderFunction(groupsApiHandler);
    }

    private Function<AuthContext, ColumnsHolder> columnsHolderFunction(ReportGroupsApiHandler groupsApiHandler) {
        return authPayload -> new ColumnsHolder(authPayload,groupsApiHandler,this);
    }

    private Function<AuthContext, Flux<ReportDefinitionHandle>> handleFlux() {
        return authPayload -> reportConfigDetailsHandler.asFlux(
                authPayload,
                config.getRequestPageSize(),
                new CyodaTableHandle(
                        authPayload,
                        connectorId.toString(),
                        config.getSchemaName(),
                        reportConfigDetailsHandler.getTables(authPayload).get(0).getName(),
                        Optional.empty(),
                        reportConfigDetailsHandler.getHandlerKey()
                ),
                CompoundPredicateNode.empty(null),
                NOT_LISTENING
        );
    }



    @Override
    protected Map<TableDefinitionHandle, List<ColumnDefinition>> refreshFieldDefs(AuthContext authContext) {
        LOG.info("Refreshing Field Definitions");
        Map<TableDefinitionHandle, List<ColumnDefinition>> result = new HashMap<>();
        Flux<ReportDefinitionHandle> flux = fluxFunction.apply(authContext);
        flux.doOnNext(item -> {
            String reportName = item.getReportName();
            String reportConfigId = item.getReportConfigId();
            String tableName = reportNameToTableName(reportConfigId);

            List<CyodaColumnHandle> columns = item.getColumns();
            List<ColumnDefinition> coldefs = columns.stream()
                    .map(it -> {
                        Preconditions.checkArgument(!RESERVED_COLUMN_NAMES.contains(it.getColumnName()), "Report %s is using a reserved column name: %s." +
                                " Reserved names are: %s", reportName, it.getColumnName(), Joiner.on(", ").join(RESERVED_COLUMN_NAMES));
                        return newColumnDefinition(it);
                    })
                    .collect(Collectors.toList());

            ColumnsHolder columnsHolder = columnsHolderFunction.apply(authContext);
            List<ColumnDefinition> theColDefs = StandardColumnDefinition.builder()
                    .add(new StandardColumnDefinition(0,ROW_REPORT_ROW_NUMBER_COLUMN,LONG))
                    .add(newColumnDefinition(columnsHolder.reportIdColumn))
                    .add(newColumnDefinition(columnsHolder.groupingVersionColumn))
                    .add(newColumnDefinition(columnsHolder.groupJsonBase64Column))
                    .addAll(coldefs)
                    .build();
            result.put(asTableDefinitionHandle(tableName, reportConfigId, item.getDescription()), theColDefs);
        }).blockLast();
        // If there are duplicates, last write wins.
        return ImmutableMap.copyOf(result);
    }

    private StandardColumnDefinition newColumnDefinition(CyodaColumnHandle columnHandle) {
        String columnName = columnHandle.getColumnName();
        return new StandardColumnDefinition(
                columnHandle.getOrdinalPosition(),
                columnName,
                columnHandle.getDataType()
        );
    }

    private TypeSignature determinMapValueType(Type columnType) {
        return TypesUtil.isMapType(columnType) ? TypesUtil.getValueType(columnType).getTypeSignature() : null;
    }

    @Nullable private TypeSignature determineParType(@Nonnull Type columnType) {
        if (TypesUtil.isArrayType(columnType)  ) {
            return TypesUtil.getElementType(columnType).getTypeSignature();
        } else if (TypesUtil.isMapType(columnType) ) {
            return TypesUtil.getKeyType(columnType).getTypeSignature();
        }
        return null;
    }

    @Override
    public String getHandlerKey() {
        return CyodaStaticReportTable.REPORT_ROWS.name();
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
        if ( groupValueJsonBase64 == null ) return Flux.empty();
        CompoundPredicateNode predicates = getCompoundPredicateNodeForInternal(tableHandle, withReportPredicate, handle);

        return internalReportRowsApiHandler.asFlux(authContext,pageSize, tableHandle, predicates,listener);
    }

    private CompoundPredicateNode getCompoundPredicateNodeForInternal(CyodaTableHandle tableHandle, ColumnPredicateNode<Any> predicates, GroupingHandle handle) {
        String reportId = handle.reportId;
        UUID groupingVersion = handle.groupingVersion;
        String groupValueJsonBase64 = handle.groupHeader.getGroupValuesJsonBase64();

        ColumnsHolder columnsHolder = columnsHolderFunction.apply(tableHandle.getAuthPayload());
        CompoundPredicateNode.Builder builder = CompoundPredicateNode.builder(Connective.AND);
        builder.addLeaf(columnsHolder.reportIdColumn.newEqualsPredicateFromJava(reportId));
        builder.addLeaf(columnsHolder.groupingVersionColumn.newEqualsPredicateFromJava(groupingVersion));
        builder.addLeaf(columnsHolder.groupJsonBase64Column.newEqualsPredicateFromJava(groupValueJsonBase64));
        builder.addMember(predicates);
        return builder.build();
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull RowHandle field, CyodaColumnHandle columnHandle) {
        if ( ROW_REPORT_ROW_NUMBER_COLUMN.equals(columnHandle.getColumnName()) ) {
            return field.rowNum;
        }
        if ( ROW_REPORT_ID_COLUMN.equals(columnHandle.getColumnName()) ) {
            return field.reportId;
        }
        if ( ROW_GROUPING_VERSION_COLUMN.equals(columnHandle.getColumnName()) ) {
            return field.groupingVersion;
        }
        if ( ROW_GROUP_JSON_BASE64_VARIABLE.equals(columnHandle.getColumnName()) ) {
            return field.groupJsonBase64;
        }
        return ReportRowNavigator.getValue(columnHandle.getColumnName(),field.reportRow);
    }

    @Override
    public Flux<RowHandle> asFlux(
            AuthContext authContext,
            int pageSize,
            CyodaTableHandle tableHandle,
            CompoundPredicateNode predicates,
            SizeListener listener
    ) {
        logCreation(pageSize, tableHandle, predicates, LOG);
        CompoundPredicateNode withReportPredicate = getCompoundPredicateNode(tableHandle, predicates);

        Flux<GroupingHandle> groupsFlux = groupsApiHandler
                .asFlux(authContext,pageSize, tableHandle, withReportPredicate,listener);

        return groupsFlux.flatMap(it-> internalFlux(authContext,pageSize,tableHandle,withReportPredicate,it,listener));
    }

    private CompoundPredicateNode getCompoundPredicateNode(CyodaTableHandle tableHandle, CompoundPredicateNode predicates) {
        SchemaTableName key = new SchemaTableName(tableHandle.getSchemaName(), tableHandle.getTableName());
        log.debug(() -> "table key is "+key);
        Map<SchemaTableName, CyodaTable> tableMap = lookupTableMap(tableHandle.getAuthPayload());
        CyodaTable cyodaTable = tableMap.get(key);
        if ( cyodaTable == null ) {
            tableMap = refreshTableMap(tableHandle.getAuthPayload());
            cyodaTable = tableMap.get(key);
            if (cyodaTable == null) {
                String keys = tableMap.keySet().stream().map(SchemaTableName::toString).collect(Collectors.joining(", "));
                log.error("LookupTable has keys " + keys);
                throw new TableNotFoundException(key, "Cannot find table for " + key);
            }
        }
        log.debug("Cyoda report table found: %s",cyodaTable.getName());
        String reportConfigurationId = cyodaTable.getReportConfigurationId();
        CompoundPredicateNode.Builder builder = CompoundPredicateNode.builder(Connective.AND);
        builder.addMember(predicates);

        ColumnsHolder columnsHolder = columnsHolderFunction.apply(tableHandle.getAuthPayload());

        builder.addLeaf(columnsHolder.reportConfigIdColumn.newEqualsPredicateFromJava(reportConfigurationId));
        return builder.build();
    }


    static class ColumnsHolder {

        private final CyodaColumnHandle reportIdColumn;
        private final CyodaColumnHandle groupingVersionColumn;
        private final CyodaColumnHandle groupJsonBase64Column;
        private final CyodaColumnHandle reportConfigIdColumn;

        ColumnsHolder(AuthContext authContext, ReportGroupsApiHandler groupsApiHandler,
                      ReportRowsApiHandler rowsApiHandler) {

            Collection<CyodaTable> groupTables = groupsApiHandler.getTables(authContext);
            Preconditions.checkArgument(groupTables.size()==1,"Unexpected number of tables from the %s. Expected size is 1, but was %s",
                    ReportGroupsApiHandler.class.getName(),groupTables.size());

            CyodaTable groupTable = groupTables.iterator().next();

            this.reportIdColumn = setupReportIdColumn(groupTable);
            this.groupingVersionColumn = setupGroupingVersionColumn(groupTable);
            this.groupJsonBase64Column = setupGroupJsonBase64Column(groupTable);
            this.reportConfigIdColumn = setupReportConfigIdColumn(groupTable);
        }

        private CyodaColumnHandle setupReportIdColumn(CyodaTable groupTable) {
            return groupTable.getColumns().stream()
                    .filter(it -> it.getColumnName().equals(HISTORY_REPORT_ID_COLUMN))
                    .findAny()
                    //TODO can't see why, since HISTORY_REPORT_ID_COLUMN == ROW_REPORT_ID_COLUMN
//                    .map(it -> new CyodaColumnHandle(  // Need to replace the column name with our local one.
//                            it.getConnectorId(),
//                            ROW_REPORT_ID_COLUMN,
//                            it.getColumnType(),
//                            it.getDataType(),
//                            it.getOrdinalPosition(),
//                            it.getRequestHandlerKey()))
                    .orElseThrow(() -> new IllegalStateException(HISTORY_REPORT_ID_COLUMN + COLUMN_NOT_FOUND));
        }

        private CyodaColumnHandle setupGroupingVersionColumn(CyodaTable groupTable) {
            return groupTable.getColumns().stream()
                    .filter(it -> it.getColumnName().equals(GROUPING_VERSION_COLUMN))
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException(GROUPING_VERSION_COLUMN + COLUMN_NOT_FOUND));    }

        private CyodaColumnHandle setupReportConfigIdColumn(CyodaTable groupTable) {
            return groupTable.getColumns().stream()
                    .filter(it -> it.getColumnName().equals(GROUPING_REPORT_CONFIG_ID_COLUMN))
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException(GROUPING_REPORT_CONFIG_ID_COLUMN + COLUMN_NOT_FOUND));    }

        private CyodaColumnHandle setupGroupJsonBase64Column(CyodaTable groupTable) {
            return groupTable.getColumns().stream()
                    .filter(it -> it.getColumnName().equals(ROW_GROUP_JSON_BASE64_VARIABLE))
        //TODO should work without it since UUID.TYPE_STRING = StandardTypes.VARCHAR
//                    .map(it -> new CyodaColumnHandle(
//                            it.getConnectorId(),
//                            it.getColumnName(),
//                            VarcharType.VARCHAR, // This is because Presto cannot deal with UUID, even if there is a UuidType.
//                            STRING,
//                            it.getOrdinalPosition(),
//                            it.getRequestHandlerKey(),
//                            it.getIsNullable()))
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException(ROW_GROUP_JSON_BASE64_VARIABLE + COLUMN_NOT_FOUND));
        }
    }
}

