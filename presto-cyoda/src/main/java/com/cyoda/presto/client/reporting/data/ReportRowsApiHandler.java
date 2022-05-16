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
import com.cyoda.presto.client.ApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.jodabeans.StandardColumnDefinition;
import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.logic.ColumnPredicateUtils;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.logic.Connective;
import com.cyoda.presto.client.logic.ColumnPredicateNode;
import com.cyoda.presto.client.reporting.BaseReportsApiHandler;
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.client.reporting.CyodaStaticReportTable;
import com.cyoda.presto.client.reporting.groups.GroupingHandle;
import com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle;
import com.cyoda.presto.client.types.DataTypeValue;
import com.cyoda.presto.client.types.TypesUtil;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.TypeSignature;
import com.facebook.presto.common.type.VarcharType;
import com.facebook.presto.spi.SchemaTableName;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.airlift.slice.Slice;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.AbstractTableHolder.TableDefinitionHandle.asTableDefinitionHandle;
import static com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler.GROUPING_REPORT_CONFIG_ID_COLUMN;
import static com.cyoda.presto.client.types.DataType.STRING;

public class ReportRowsApiHandler extends BaseReportsApiHandler<RowHandle>
        implements ApiRequestHandler<RowHandle> {

    static final String ROW_REPORT_ID_COLUMN = "reportId";
    static final String ROW_GROUP_JSON_BASE64_VARIABLE = "groupValuesJsonBase64";

    // These are also reserved words for column names coming from reports.
    private static final List<String> RESERVED_COLUMN_NAMES = ImmutableList.<String>builder()
            .add(ROW_REPORT_ID_COLUMN)
            .add(ROW_GROUP_JSON_BASE64_VARIABLE)
            .build();
    public static final String COLUMN_NOT_FOUND = " Column not found!";

    private final ReportGroupsApiHandler groupsApiHandler;
    private final InternalReportRowsApiHandler internalReportRowsApiHandler;

    private final CyodaColumnHandle reportIdColumn;
    private final CyodaColumnHandle groupJsonBase64Column;
    private final CyodaColumnHandle reportConfigIdColumn;
    private final ReportConfigDetailsApiHandler reportConfigDetailsHandler;

    private final Iterable<ReportDefinitionHandle> handleIterable;

    @Inject
    public ReportRowsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                  RestTemplateCustomizer restTemplateCustomizer) {
        super(connectorId, config, typeManager, REPORT_ENDPOINT,restTemplateCustomizer);

        this.groupsApiHandler = new ReportGroupsApiHandler(connectorId,config,typeManager,restTemplateCustomizer);
        this.internalReportRowsApiHandler = new InternalReportRowsApiHandler(connectorId,config,typeManager,restTemplateCustomizer);
        this.reportConfigDetailsHandler = new ReportConfigDetailsApiHandler(connectorId,config,typeManager,restTemplateCustomizer);

        CyodaTable table = reportConfigDetailsHandler.getTables().get(0);
        CyodaTableHandle tableHandle = new CyodaTableHandle(connectorId.toString(), config.getSchemaName(), table.getName(), Optional.empty(), reportConfigDetailsHandler.getHandlerKey());
        this.handleIterable = () -> reportConfigDetailsHandler.getResponseIterator(config.getRequestPageSize(), tableHandle, CompoundPredicateNode.empty(null));

        List<CyodaTable> groupTables = groupsApiHandler.getTables();
        Preconditions.checkArgument(groupTables.size()==1,"Unexpected number of tables from the %s. Expected size is 1, but was %s",
                ReportGroupsApiHandler.class.getName(),groupTables.size());

        CyodaTable groupTable = groupTables.get(0);

        this.reportIdColumn = setupReportIdColumn(groupTable);
        this.groupJsonBase64Column = setupGroupJsonBase64Column(groupTable);
        this.reportConfigIdColumn = setupReportConfigIdColumn(groupTable);
    }

    private CyodaColumnHandle setupReportConfigIdColumn(CyodaTable groupTable) {
        return groupTable.getColumns().stream()
                .filter(it -> it.getColumnName().equals(GROUPING_REPORT_CONFIG_ID_COLUMN))
                .findAny()
                .orElseThrow(() -> new IllegalStateException(GROUPING_REPORT_CONFIG_ID_COLUMN + COLUMN_NOT_FOUND));    }

    private CyodaColumnHandle setupGroupJsonBase64Column(CyodaTable groupTable) {
        return groupTable.getColumns().stream()
                .filter(it -> it.getColumnName().equals(ROW_GROUP_JSON_BASE64_VARIABLE))
                .map(it -> new CyodaColumnHandle(
                        it.getConnectorId(),
                        it.getColumnName(),
                        VarcharType.VARCHAR, // This is because Presto cannot deal with UUID, even if there is a UuidType.
                        STRING,
                        it.getOrdinalPosition(),
                        it.getRequestHandlerKey(),
                        it.getIsNullable())
                ).findAny()
                .orElseThrow(() -> new IllegalStateException(ROW_GROUP_JSON_BASE64_VARIABLE + COLUMN_NOT_FOUND));
    }

    private CyodaColumnHandle setupReportIdColumn(CyodaTable groupTable) {
        return groupTable.getColumns().stream()
                .filter(it -> it.getColumnName().equals(HISTORY_REPORT_ID_COLUMN))
                .findAny()
                .map(it -> new CyodaColumnHandle(  // Need to replace the column name with our local one.
                        it.getConnectorId(),
                        ROW_REPORT_ID_COLUMN,
                        it.getColumnType(),
                        it.getDataType(),
                        it.getOrdinalPosition(),
                        it.getRequestHandlerKey())
                ).orElseThrow(() -> new IllegalStateException(HISTORY_REPORT_ID_COLUMN + COLUMN_NOT_FOUND));
    }

    @Override
    protected Map<TableDefinitionHandle, List<ColumnDefinition>> refreshFieldDefs() {
        Map<TableDefinitionHandle,List<ColumnDefinition>> result = new HashMap<>();
        StreamSupport.stream(handleIterable.spliterator(), true)
                .forEach( item -> {
                    String reportName = item.getReportName();
                    String reportConfigId = item.getReportConfigId();
                    String tableName = reportNameToTableName(reportConfigId);

                    List<CyodaColumnHandle> columns = item.getColumns();
                    List<ColumnDefinition> coldefs = columns.stream()
                            .map(it->{
                                Preconditions.checkArgument(!RESERVED_COLUMN_NAMES.contains(it.getColumnName()),"Report %s is using a reserved column name: %s." +
                                        " Reserved names are: %s",reportName,it.getColumnName(), Joiner.on(", ").join(RESERVED_COLUMN_NAMES));
                                return newColumnDefinition(it);
                            })
                            .collect(Collectors.toList());

                    ImmutableList.Builder<ColumnDefinition> colBuilder = ImmutableList.builder();
                    colBuilder.add(newColumnDefinition(reportIdColumn));
                    colBuilder.add(newColumnDefinition(groupJsonBase64Column));
                    colBuilder.addAll(coldefs);

                    // If there are duplicates, last write wins.
                    result.put(asTableDefinitionHandle(tableName, reportConfigId, item.getDescription()),colBuilder.build());
                });
        return ImmutableMap.copyOf(result);
    }

    private StandardColumnDefinition newColumnDefinition(CyodaColumnHandle columnHandle) {
        Type columnType = columnHandle.getColumnType();
        String columnName = columnHandle.getColumnName();
        return new StandardColumnDefinition(
                columnHandle.getOrdinalPosition(),
                columnName,
                columnType.getTypeSignature().getBase(),
                columnHandle.getDataType(),
                determineParType(columnType),
                determinMapValueType(columnType)
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

    protected Iterable<RowHandle> groupsIterator(
            int pageSize,
            CyodaTableHandle tableHandle,
            ColumnPredicateNode<Any> withReportPredicate,
            @Nonnull GroupingHandle handle
    ) {
        String reportId = handle.reportId;
        String groupValueJsonBase64 = handle.groupHeader.getGroupValuesJsonBase64();

        if ( groupValueJsonBase64 == null ) return Collections::emptyIterator;

        Slice historyIdSlice = DataTypeValue.of(reportId).asSlice(VarcharType.VARCHAR);
        Slice groupingValueSlice = DataTypeValue.of(groupValueJsonBase64).asSlice(VarcharType.VARCHAR);

        CompoundPredicateNode.Builder builder = CompoundPredicateNode.builder(Connective.AND);
        builder.addLeaf(ColumnPredicateUtils.newEqualsPredicate(reportIdColumn, historyIdSlice,String.class));
        builder.addLeaf(ColumnPredicateUtils.newEqualsPredicate(groupJsonBase64Column, groupingValueSlice,String.class));
        builder.addMember(withReportPredicate);
        ColumnPredicateNode<Any> predicates = builder.build();

        return () -> internalReportRowsApiHandler.getResponseIterator(pageSize, tableHandle, predicates);
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull RowHandle field, CyodaColumnHandle columnHandle) {
        if ( ROW_REPORT_ID_COLUMN.equals(columnHandle.getColumnName()) ) {
            return field.reportId;
        }
        if ( ROW_GROUP_JSON_BASE64_VARIABLE.equals(columnHandle.getColumnName()) ) {
            return field.groupJsonBase64;
        }
        return ReportRowNavigator.getValue(columnHandle.getColumnName(),field.reportRow);
    }

    @Override
    protected @Nonnull Object mapFieldValue(@Nonnull final Object value, CyodaColumnHandle columnHandle) {
        return super.mapFieldValue(value,columnHandle);
    }

    @Override
    public Iterator<RowHandle> getResponseIterator(
            int pageSize,
            CyodaTableHandle tableHandle,
            ColumnPredicateNode<Any> predicates
    ) {

        String reportConfigurationId = lookupTableMap()
                .get(new SchemaTableName(tableHandle.getSchemaName(), tableHandle.getTableName()))
                .getReportConfigurationId();
        Slice reportConfigIdSlice = DataTypeValue.of(reportConfigurationId).asSlice(VarcharType.VARCHAR);
        CompoundPredicateNode.Builder builder = CompoundPredicateNode.builder(Connective.AND);
        builder.addMember(predicates);
        builder.addLeaf(ColumnPredicateUtils.newEqualsPredicate(reportConfigIdColumn, reportConfigIdSlice,String.class));
        ColumnPredicateNode<Any> withReportPredicate = builder.build();

        Iterable<GroupingHandle> statsIterable = () -> groupsApiHandler
                .getResponseIterator(pageSize, tableHandle, withReportPredicate);

        return StreamSupport.stream(statsIterable.spliterator(),true)
                .flatMap(it-> StreamSupport.stream(groupsIterator(pageSize,tableHandle,withReportPredicate,it).spliterator(), true))
                .iterator();
    }
}

