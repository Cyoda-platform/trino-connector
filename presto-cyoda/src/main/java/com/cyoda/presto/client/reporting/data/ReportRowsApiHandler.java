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
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.logic.Connective;
import com.cyoda.presto.client.logic.LeafPredicateNode;
import com.cyoda.presto.client.logic.PredicateBuilder;
import com.cyoda.presto.client.logic.PredicateNode;
import com.cyoda.presto.client.reporting.BaseReportsApiHandler;
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.client.reporting.CyodaStaticReportTable;
import com.cyoda.presto.client.reporting.groups.GroupingHandle;
import com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle;
import com.cyoda.presto.client.types.SupportedDataType;
import com.cyoda.presto.client.types.TypesUtil;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.TypeSignature;
import com.facebook.presto.common.type.VarcharType;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.airlift.slice.Slice;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_ID_COLUMN;
import static com.cyoda.presto.client.types.DataType.STRING;

public class ReportRowsApiHandler extends BaseReportsApiHandler<RowHandle>
        implements ApiRequestHandler<RowHandle> {

    static final String ROW_HISTORY_ID_COLUMN = "historyId";
    static final String ROW_GROUP_JSON_BASE64_VARIABLE = "groupValuesJsonBase64";

    // These are also reserved words for column names coming from reports.
    private static final List<String> RESERVED_COLUMN_NAMES = ImmutableList.<String>builder()
            .add(ROW_HISTORY_ID_COLUMN)
            .add(ROW_GROUP_JSON_BASE64_VARIABLE)
            .build();

    private final ReportGroupsApiHandler groupsApiHandler;
    private final InternalReportRowsApiHandler internalReportRowsApiHandler;

    private final CyodaColumnHandle historyIdColumn;
    private final CyodaColumnHandle groupJsonBase64Column;
    private final ReportConfigDetailsApiHandler reportConfigDetailsHandler;

    private final Iterable<ReportDefinitionHandle> handleIterable;

    @Inject
    public ReportRowsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                  RestTemplateCustomizer restTemplateCustomizer) {
        super(connectorId, config, typeManager, REPORT_ENDPOINT,restTemplateCustomizer);

        this.groupsApiHandler = new ReportGroupsApiHandler(connectorId,config,typeManager,restTemplateCustomizer);
        this.internalReportRowsApiHandler = new InternalReportRowsApiHandler(connectorId,config,typeManager,restTemplateCustomizer);
        this.reportConfigDetailsHandler = new ReportConfigDetailsApiHandler(connectorId,config,typeManager,restTemplateCustomizer);

        this.handleIterable = () -> reportConfigDetailsHandler.getResponseIterator(config.getRequestPageSize(), Collections.emptyList(), CompoundPredicateNode.EMPTY);

        List<CyodaTable> groupTables = groupsApiHandler.getTables();
        Preconditions.checkArgument(groupTables.size()==1,"Unexpected number of tables from the %s. Expected size is 1, but was %s",
                ReportGroupsApiHandler.class.getName(),groupTables.size());

        CyodaTable groupTable = groupTables.get(0);

        this.historyIdColumn = groupTable.getColumns().stream()
                .filter(it -> it.getColumnName().equals(HISTORY_ID_COLUMN))
                .findAny()
                .map(it -> new CyodaColumnHandle(  // Need to replace the column name with our local one.
                        it.getConnectorId(),
                        ROW_HISTORY_ID_COLUMN,
                        it.getColumnType(),
                        it.getDataType(),
                        it.getOrdinalPosition(),
                        it.getRequestHandlerKey())
                ).orElseThrow(()->new IllegalStateException(HISTORY_ID_COLUMN+" Column not found!"));

        this.groupJsonBase64Column = groupTable.getColumns().stream()
                .filter(it -> it.getColumnName().equals(ROW_GROUP_JSON_BASE64_VARIABLE))
                .map(it->new CyodaColumnHandle(
                        it.getConnectorId(),
                        it.getColumnName(),
                        VarcharType.VARCHAR, // This is because Presto cannot deal with UUID, even if there is a UuidType.
                        STRING,
                        it.getOrdinalPosition(),
                        it.getRequestHandlerKey(),
                        it.getIsNullable())
                ).findAny()
                .orElseThrow(()->new IllegalStateException(ROW_GROUP_JSON_BASE64_VARIABLE +" Column not found!"));

        groupTable.getColumns().stream()
                .filter(it -> it.getColumnName().equals(ROW_GROUP_JSON_BASE64_VARIABLE))
                .map(it->new CyodaColumnHandle(
                        it.getConnectorId(),
                        it.getColumnName(),
                        VarcharType.VARCHAR, // This is because Presto cannot deal with UUID, even if there is a UuidType.
                        STRING,
                        it.getOrdinalPosition(),
                        it.getRequestHandlerKey(),
                        it.getIsNullable())
                ).findAny()
                .orElseThrow(()->new IllegalStateException(ROW_GROUP_JSON_BASE64_VARIABLE +" Column not found!"));
    }

    @Override
    protected Map<String, List<ColumnDefinition>> setupFieldDefs() {
        ImmutableMap.Builder<String,List<ColumnDefinition>> builder = ImmutableMap.builder();
        StreamSupport.stream(handleIterable.spliterator(), true)
                .forEach( item -> {
                    String reportName = item.getReportName();
                    String tableName = reportNameToTableName(reportName);

                    List<CyodaColumnHandle> columns = item.getColumns();
                    List<ColumnDefinition> coldefs = columns.stream()
                            .map(it->{
                                Preconditions.checkArgument(!RESERVED_COLUMN_NAMES.contains(it.getColumnName()),"Report %s is using a reserved column name: %s." +
                                        " Reserved names are: %s",reportName,it.getColumnName(), Joiner.on(", ").join(RESERVED_COLUMN_NAMES));
                                return newColumnDefinition(it);
                            })
                            .collect(Collectors.toList());

                    ImmutableList.Builder<ColumnDefinition> colBuilder = ImmutableList.builder();
                    colBuilder.add(newColumnDefinition(historyIdColumn));
                    colBuilder.add(newColumnDefinition(groupJsonBase64Column));
                    colBuilder.addAll(coldefs);


                    builder.put(tableName,colBuilder.build());
                });
        return builder.build();
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
            List<CyodaColumnHandle> projectedColumns,
            @Nonnull GroupingHandle handle
    ) {
        String historyId = handle.historyId;
        String groupValueJsonBase64 = handle.groupHeader.getGroupValuesJsonBase64();

        if ( groupValueJsonBase64 == null ) return Collections::emptyIterator;

        Slice historyIdSlice = SupportedDataType.of(historyId,String.class).asSlice(VarcharType.VARCHAR);
        Slice groupingValueSlice = SupportedDataType.of(groupValueJsonBase64,String.class).asSlice(VarcharType.VARCHAR);
        List<PredicateNode<?>> members = Arrays.asList(
                LeafPredicateNode.leaf(PredicateBuilder.createEqualsPredicate(historyIdColumn, historyIdSlice)),
                LeafPredicateNode.leaf(PredicateBuilder.createEqualsPredicate(groupJsonBase64Column, groupingValueSlice))
        );
        PredicateNode<Any> predicates = CompoundPredicateNode.of(members, Connective.AND);

        return () -> internalReportRowsApiHandler.getResponseIterator(pageSize, projectedColumns, predicates);
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull RowHandle field, CyodaColumnHandle columnHandle) {
        if ( ROW_HISTORY_ID_COLUMN.equals(columnHandle.getColumnName()) ) {
            return field.historyId;
        }
        if ( ROW_GROUP_JSON_BASE64_VARIABLE.equals(columnHandle.getColumnName()) ) {
            return field.groupJsonBase64;
        }
        return field.reportRow.get(columnHandle.getColumnName());
    }

    @Override
    protected @Nonnull Object mapFieldValue(@Nonnull final Object value, CyodaColumnHandle columnHandle) {
        return super.mapFieldValue(value,columnHandle);
    }


    @Override
    public Iterator<RowHandle> getResponseIterator(
            int pageSize,
            List<CyodaColumnHandle> projectedColumns,
            PredicateNode<Any> predicates
    ) {
        Iterable<GroupingHandle> statsIterable = () -> groupsApiHandler
                .getResponseIterator(pageSize, projectedColumns, predicates);

        return StreamSupport.stream(statsIterable.spliterator(),true)
                .flatMap(it->
                        StreamSupport.stream(groupsIterator(pageSize,projectedColumns,it).spliterator(), true)
                ).iterator();
    }


}

