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

package com.cyoda.presto.client.reporting.metaproviders;

import com.cyoda.core.model.reports.DistributedReportInfoView;
import com.cyoda.presto.client.jodabeans.StandardColumnDefinition;
import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.client.types.CompoundDataType;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.service.api.beans.GroupHeader;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_CREATE_TIME_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_GROUPING_COLUMNS_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_GROUPING_VERSION_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_HIERARHY_ENABLE_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_STATUS_NAME_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_TYPE_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_USER_NAME_COLUMN;
import static com.cyoda.presto.client.reporting.BaseReportsApiHandler.REPORT_ENDPOINT;
import static com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler.REPORT_DEFS_ENDPOINT;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_COLUMNS_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_CREATION_DATE_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_DESCRIPTION_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_JSON_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_NAME_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_TABLE_NAME_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_TYPE_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_UPDATE_DATE_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_USER_ID_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler.REPORT_HISTORY_ENDPOINT;
import static com.cyoda.presto.client.types.DataType.BOOLEAN;
import static com.cyoda.presto.client.types.DataType.DATE;
import static com.cyoda.presto.client.types.DataType.INTEGER;
import static com.cyoda.presto.client.types.DataType.LIST;
import static com.cyoda.presto.client.types.DataType.LOCAL_DATE_TIME;
import static com.cyoda.presto.client.types.DataType.LONG;
import static com.cyoda.presto.client.types.DataType.OBJECT;
import static com.cyoda.presto.client.types.DataType.STRING;
import static com.cyoda.presto.client.types.DataType.UUID_TYPE;

public enum StaticReportTable implements TableDefinition {
    REPORTS(Arrays.asList(ReportsColumnDef.values()), REPORT_DEFS_ENDPOINT, CyodaTableHandle.TableType.REPORTS),
    REPORT_STATS(StandardColumnDefinition.builder()
            .add(DistributedReportInfoView.meta())
            .build(), REPORT_ENDPOINT, CyodaTableHandle.TableType.STATS),
    API_CALL_STATS(Arrays.asList(ApiCallStatsColumnDef.values()), null, CyodaTableHandle.TableType.CALL_STATS),
    CACHE_STATS(Arrays.asList(CacheStatsColumnDef.values()), null, CyodaTableHandle.TableType.CACHE_STATS),
    CACHE_CONTENT(Arrays.asList(CacheContentColumnDef.values()), null, CyodaTableHandle.TableType.CACHE_CONTENT),
    REPORT_HISTORIES(Arrays.asList(ReportHistoryColumnDef.values()), REPORT_HISTORY_ENDPOINT, CyodaTableHandle.TableType.HISTORY),
    REPORT_GROUPS(StandardColumnDefinition.builder()
            .add(new StandardColumnDefinition(0, StaticReportFields.HISTORY_REPORT_ID_COLUMN, STRING))
            .add(new StandardColumnDefinition(1, StaticReportFields.GROUPING_VERSION_COLUMN, UUID_TYPE))
            .add(GroupHeader.meta())
            .build(), REPORT_ENDPOINT, CyodaTableHandle.TableType.GROUP),
    REPORT_ROWS(StandardColumnDefinition.builder()
            .add(new StandardColumnDefinition(0, StaticReportFields.ROW_REPORT_ROW_NUMBER_COLUMN, LONG))
            .add(new StandardColumnDefinition(1, StaticReportFields.ROW_REPORT_ID_COLUMN, STRING))
            .add(new StandardColumnDefinition(2, StaticReportFields.GROUPING_VERSION_COLUMN, UUID_TYPE))
            .add(new StandardColumnDefinition(3, StaticReportFields.ROW_GROUP_JSON_BASE64_VARIABLE, STRING))
            .build(), REPORT_ENDPOINT, CyodaTableHandle.TableType.DATA);

    private final Map<String, ColumnDefinition> columns;
    private final String endpoint;

    private final CyodaTableHandle.TableType tableType;


    StaticReportTable(List<ColumnDefinition> columns, String endpoint, CyodaTableHandle.TableType tableType) {
        this.columns = columns.stream().collect(Collectors.toMap(ColumnDefinition::getFieldName, Function.identity()));
        this.endpoint = endpoint;
        this.tableType = tableType;
    }

    @Override
    public String getTableName() {
        return name().toLowerCase(Locale.ROOT);
    }


    @Override
    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public Collection<ColumnDefinition> getColumns() {
        return columns.values();
    }

    @Override
    public @Nonnull ColumnDefinition getColumn(String name) {
        return Optional.ofNullable(columns.get(name)).orElseThrow(() ->
                new NoSuchElementException(String.format("No column with name %s in %s metadata", name, name())));
    }

    @Override
    public CyodaTableHandle.TableType getTableType() {
        return tableType;
    }

    public List<String> getFieldList() {
        return ImmutableList.copyOf(getColumns().stream().map(ColumnDefinition::getFieldName).collect(Collectors.toList()));
    }

    public enum ReportsColumnDef implements ColumnDefinition {
        ID(0, REPORT_ID_COLUMN, STRING),
        NAME(1, REPORT_NAME_COLUMN, STRING),
        TABLE_NAME(2, REPORT_TABLE_NAME_COLUMN, STRING),
        DESCRIPTION(3, REPORT_DESCRIPTION_COLUMN, STRING),
        TYPE(4, REPORT_TYPE_COLUMN, STRING),
        USER_ID(5, REPORT_USER_ID_COLUMN, STRING),
        CREATION_DATE(6, REPORT_CREATION_DATE_COLUMN, LOCAL_DATE_TIME),
        UPDATE_DATE(7, REPORT_UPDATE_DATE_COLUMN, LOCAL_DATE_TIME),
        REPORT_COLUMNS(8, REPORT_COLUMNS_COLUMN, LIST, OBJECT),
        REPORT_JSON(9, REPORT_JSON_COLUMN, STRING);;

        private final int pos;
        private final String fieldName;
        private final CompoundDataType dataType;

        ReportsColumnDef(int pos, String fieldName, DataType mainType, DataType... typeParams) {
            this.pos = pos;
            this.fieldName = fieldName;
            this.dataType = new CompoundDataType(fieldName, mainType, typeParams);
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
        public CompoundDataType getDataType() {
            return dataType;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("pos", pos)
                    .add("fieldName", fieldName)
                    .add("dataType", dataType)
                    .toString();
        }
    }

    public enum ApiCallStatsColumnDef implements ColumnDefinition {
        QUERY_ID(0, STRING),
        NODE_ID(1, STRING),
        NODE_ADDRESS(2, STRING),
        CALL_TIME(3, DATE),
        CALL_MILLIS(4, INTEGER),
        DURATION_MILLIS(5, LONG),
        API_HANDLER(6, STRING),
        REQUEST_PARAMS(7, DataType.MAP, STRING, STRING),
        REQUEST_URL(8, STRING),
        RESPONSE(9, STRING);
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("pos", pos)
                    .add("fieldName", fieldName)
                    .add("dataType", dataType)
                    .toString();
        }
        private final int pos;
        private final String fieldName;
        private final CompoundDataType dataType;

        ApiCallStatsColumnDef(int pos, DataType mainType, DataType... typeParams) {
            this.pos = pos;
            this.fieldName = name().toLowerCase();
            this.dataType = new CompoundDataType(fieldName, mainType, typeParams);
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
        public CompoundDataType getDataType() {
            return dataType;
        }
    }

    public enum CacheStatsColumnDef implements ColumnDefinition {
        NODE_ID(1, STRING),
        NODE_ADDRESS(2, STRING),
        CACHE_NAME(3, STRING),
        HIT_COUNT(4, LONG),
        MISS_COUNT(5, LONG),
        LOAD_SUCCESS_COUNT(6, LONG),
        LOAD_FAILURE_COUNT(7, LONG),
        TOTAL_LOAD_TIME(8, LONG),
        EVICTION_COUNT(9, LONG),
        EVICTION_WEIGHT(10, LONG);
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("pos", pos)
                    .add("fieldName", fieldName)
                    .add("dataType", dataType)
                    .toString();
        }
        private final int pos;
        private final String fieldName;
        private final CompoundDataType dataType;

        CacheStatsColumnDef(int pos, DataType mainType, DataType... typeParams) {
            this.pos = pos;
            this.fieldName = name().toLowerCase();
            this.dataType = new CompoundDataType(fieldName, mainType, typeParams);
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
        public CompoundDataType getDataType() {
            return dataType;
        }
    }

    public enum CacheContentColumnDef implements ColumnDefinition {
        NODE_ID(1, STRING),
        NODE_ADDRESS(2, STRING),
        CACHE_NAME(3, STRING),
        KEY(4, STRING),
        SIZE(5, LONG);
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("pos", pos)
                    .add("fieldName", fieldName)
                    .add("dataType", dataType)
                    .toString();
        }
        private final int pos;
        private final String fieldName;
        private final CompoundDataType dataType;

        CacheContentColumnDef(int pos, DataType mainType, DataType... typeParams) {
            this.pos = pos;
            this.fieldName = name().toLowerCase();
            this.dataType = new CompoundDataType(fieldName, mainType, typeParams);
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
        public CompoundDataType getDataType() {
            return dataType;
        }
    }


    public enum ReportHistoryColumnDef implements ColumnDefinition {
        ID(1, StaticReportFields.HISTORY_REPORT_ID_COLUMN, STRING),
        CREATION_DATE(2, HISTORY_CREATE_TIME_COLUMN, LOCAL_DATE_TIME),
        TYPE(3, HISTORY_TYPE_COLUMN, STRING),
        STATUS(4, HISTORY_STATUS_NAME_COLUMN, STRING),
        HIERARCHY_ENABLE(5, HISTORY_HIERARHY_ENABLE_COLUMN, BOOLEAN),
        GROUPING_VERSION(6, HISTORY_GROUPING_VERSION_COLUMN, UUID_TYPE),
        GROUPING_COLUMNS(7, HISTORY_GROUPING_COLUMNS_COLUMN, LIST, STRING),
        USER_NAME(8, HISTORY_USER_NAME_COLUMN, STRING);


        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("pos", pos)
                    .add("fieldName", fieldName)
                    .add("dataType", dataType)
                    .toString();
        }

        private final int pos;
        private final String fieldName;
        private final CompoundDataType dataType;

        ReportHistoryColumnDef(int pos, String fieldName, DataType mainType, DataType... typeParams) {
            this.pos = pos;
            this.fieldName = fieldName;
            this.dataType = new CompoundDataType(fieldName, mainType, typeParams);
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
        public CompoundDataType getDataType() {
            return dataType;
        }
    }
}
