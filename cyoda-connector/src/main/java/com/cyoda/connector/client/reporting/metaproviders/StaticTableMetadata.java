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

package com.cyoda.connector.client.reporting.metaproviders;

import com.cyoda.core.reports.DistributedReportInfoDto;
import com.cyoda.connector.client.jodabeans.StandardColumnDefinition;
import com.cyoda.connector.client.reporting.ColumnDefinition;
import com.cyoda.connector.client.types.CompoundDataType;
import com.cyoda.connector.client.types.DataType;
import com.cyoda.connector.handles.CyodaTableType;
import com.cyoda.service.api.beans.GroupHeader;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cyoda.connector.client.types.DataType.INTEGER;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_CREATE_TIME_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_GROUPING_COLUMNS_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_GROUPING_VERSION_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_HIERARHY_ENABLE_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_TYPE_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_USER_NAME_COLUMN;
import static com.cyoda.connector.client.reporting.meta.ReportDefinitionHandle.REPORT_COLUMNS_COLUMN;
import static com.cyoda.connector.client.reporting.meta.ReportDefinitionHandle.REPORT_CREATION_DATE_COLUMN;
import static com.cyoda.connector.client.reporting.meta.ReportDefinitionHandle.REPORT_DESCRIPTION_COLUMN;
import static com.cyoda.connector.client.reporting.meta.ReportDefinitionHandle.REPORT_ERROR_COLUMN;
import static com.cyoda.connector.client.reporting.meta.ReportDefinitionHandle.REPORT_ID_COLUMN;
import static com.cyoda.connector.client.reporting.meta.ReportDefinitionHandle.REPORT_JSON_COLUMN;
import static com.cyoda.connector.client.reporting.meta.ReportDefinitionHandle.REPORT_SCHEMA_NAME_COLUMN;
import static com.cyoda.connector.client.reporting.meta.ReportDefinitionHandle.REPORT_TABLE_NAME_COLUMN;
import static com.cyoda.connector.client.reporting.meta.ReportDefinitionHandle.REPORT_TYPE_COLUMN;
import static com.cyoda.connector.client.reporting.meta.ReportDefinitionHandle.REPORT_UPDATE_DATE_COLUMN;
import static com.cyoda.connector.client.reporting.meta.ReportDefinitionHandle.REPORT_USER_ID_COLUMN;
import static com.cyoda.connector.client.types.DataType.BOOLEAN;
import static com.cyoda.connector.client.types.DataType.DATE;
import static com.cyoda.connector.client.types.DataType.LIST;
import static com.cyoda.connector.client.types.DataType.LOCAL_DATE_TIME;
import static com.cyoda.connector.client.types.DataType.LONG;
import static com.cyoda.connector.client.types.DataType.MAP;
import static com.cyoda.connector.client.types.DataType.OBJECT;
import static com.cyoda.connector.client.types.DataType.STRING;
import static com.cyoda.connector.client.types.DataType.UUID_TYPE;

public enum StaticTableMetadata {
    API_CALL_STATS("Contains records on every API call to cyoda, made by connector. You can use SQL DELETE to clear it.",
            Arrays.asList(ApiCallStatsColumnDef.values()), CyodaTableType.CALL_STATS, "api_call_stats"),
    CACHE_CONTENT("Displays existing cache contents, one record - one cache key. You can use SQL DELETE to clear it or remove unwanted items",
            Arrays.asList(CacheContentColumnDef.values()), CyodaTableType.CACHE_CONTENT, "cache_content"),
    CACHE_STATS("Cache statistics, provided by Caffeine cache engine",
            Arrays.asList(CacheStatsColumnDef.values()), CyodaTableType.CACHE_STATS, "cache_stats"),
    LOG_TABLE("Mirrors log output until restart",
            Arrays.asList(LogTableColumnDef.values()), CyodaTableType.LOG_TABLE, "log"),
    REPORTS("List of reports, available for current user",
            Arrays.asList(ReportsColumnDef.values()), CyodaTableType.REPORTS, "reports"),
    REPORT_GROUPS("Records with group keys for reports of current report configuration",
            StandardColumnDefinition.builder()
            .add(new StandardColumnDefinition(0, StaticReportFields.HISTORY_REPORT_ID_COLUMN, STRING))
            .add(new StandardColumnDefinition(1, StaticReportFields.GROUPING_VERSION_COLUMN, UUID_TYPE))
            .add(GroupHeader.meta())
            .build(), CyodaTableType.GROUP),
    REPORT_HISTORIES("List of reports of current report configuration", Arrays.asList(ReportHistoryColumnDef.values()), CyodaTableType.HISTORY),
    REPORT_ROWS("Report contents", StandardColumnDefinition.builder()
            .add(new StandardColumnDefinition(0, StaticReportFields.ROW_REPORT_ROW_NUMBER_COLUMN, LONG))
            .add(new StandardColumnDefinition(1, StaticReportFields.ROW_REPORT_ID_COLUMN, STRING))
            .add(new StandardColumnDefinition(2, StaticReportFields.GROUPING_VERSION_COLUMN, UUID_TYPE))
            .add(new StandardColumnDefinition(3, StaticReportFields.ROW_GROUP_JSON_BASE64_VARIABLE, STRING))
            .build(), CyodaTableType.DATA),
    REPORT_STATS("List of all reports in system. Using in queries is NOT recommended", StandardColumnDefinition.builder()
            .add(DistributedReportInfoDto.meta())
            .build(), CyodaTableType.STATS, "report_stats"),
    TDB_RAW_DATA("Raw access to TDB, for development and testing", Arrays.asList(RawEntityContentColumnDef.values()), CyodaTableType.TDB_RAW_DATA, "tree_node_data");

    public static final String LOG_TABLE_NAME = LOG_TABLE.staticTableName;
    private final Map<String, ColumnDefinition> columns;
    private final CyodaTableType tableType;
    private final String staticTableName;
    private final String description;


    StaticTableMetadata(String description, List<ColumnDefinition> columns, CyodaTableType tableType, String staticTableName) {
        this.columns = columns.stream().collect(Collectors.toMap(ColumnDefinition::getFieldName, Function.identity()));
        this.tableType = tableType;
        this.staticTableName = staticTableName;
        this.description = description;
    }

    StaticTableMetadata(String description, List<ColumnDefinition> columns, CyodaTableType tableType) {
        this(description, columns, tableType, null);
    }

    public String getStaticTableName() {
        return staticTableName;
    }

    public String getDescription() {
        return description;
    }

    public Collection<ColumnDefinition> getColumns() {
        return columns.values();
    }

    public @Nonnull ColumnDefinition getColumn(String name) {
        return Optional.ofNullable(columns.get(name)).orElseThrow(() ->
                new NoSuchElementException(String.format("No column with name %s in %s metadata", name, name())));
    }

    public CyodaTableType getTableType() {
        return tableType;
    }

    public List<String> getFieldList() {
        return ImmutableList.copyOf(getColumns().stream().map(ColumnDefinition::getFieldName).collect(Collectors.toList()));
    }

    public enum ReportsColumnDef implements ColumnDefinition {
        ID(0, REPORT_ID_COLUMN, STRING),
        SCHEMA_NAME(1, REPORT_SCHEMA_NAME_COLUMN, STRING),
        TABLE_NAME(2, REPORT_TABLE_NAME_COLUMN, STRING),
        DESCRIPTION(3, REPORT_DESCRIPTION_COLUMN, STRING),
        TYPE(4, REPORT_TYPE_COLUMN, STRING),
        USER_ID(5, REPORT_USER_ID_COLUMN, STRING),
        CREATION_DATE(6, REPORT_CREATION_DATE_COLUMN, LOCAL_DATE_TIME),
        UPDATE_DATE(7, REPORT_UPDATE_DATE_COLUMN, LOCAL_DATE_TIME),
        REPORT_COLUMNS(8, REPORT_COLUMNS_COLUMN, LIST, OBJECT),
        REPORT_JSON(9, REPORT_JSON_COLUMN, STRING),
        ERROR(10, REPORT_ERROR_COLUMN, STRING);

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
    public enum RawEntityContentColumnDef implements ColumnDefinition {
        ID(0, UUID_TYPE),
        ENTITY_MODEL_CLASS_ID(1, UUID_TYPE),
        ENTITY_MODEL_NAME(2, STRING),
        ENTITY_MODEL_VERSION(3, INTEGER),
        ROOT_ID(4, UUID_TYPE),
        PARENT_ID(5, UUID_TYPE),
        SIBLING_INDEX(6, INTEGER),
        PARENT_PATH(7, STRING),
        PATH(8, STRING),
        DEPTH(9, INTEGER),
        INDEX(10, INTEGER),
        UNIFORMED_PATH(11, STRING),
        SIBLINGS(12, MAP, STRING, UUID_TYPE),
        LAST_UPDATE_DATE(13, DATE),
        CONTENTS(14, MAP, STRING, STRING),
        TYPE_REFERENCE(15, MAP, STRING, STRING);

        private final int pos;
        private final String fieldName;
        private final CompoundDataType dataType;

        private static final Map<String, RawEntityContentColumnDef> byFieldName = new HashMap<>();

        RawEntityContentColumnDef(int pos, DataType mainType, DataType... typeParams) {
            this.pos = pos;
            this.fieldName = name().toLowerCase();
            this.dataType = new CompoundDataType(fieldName, mainType, typeParams);
        }

        public static RawEntityContentColumnDef getByFieldName(String fieldName){
            if (byFieldName.isEmpty()){
                synchronized (RawEntityContentColumnDef.class) {
                    if (byFieldName.isEmpty()){
                        for (RawEntityContentColumnDef value : RawEntityContentColumnDef.values()) {
                            byFieldName.put(value.fieldName, value);
                        }
                    }
                }
            }
            return byFieldName.get(fieldName);
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

    public enum ApiCallStatsColumnDef implements ColumnDefinition {
        QUERY_ID(0, STRING),
        NODE_ID(1, STRING),
        NODE_ADDRESS(2, STRING),
        CALL_TIME(3, DATE),
        DURATION_MILLIS(5, LONG),
        API_HANDLER(6, STRING),
        REQUEST_ROUTE(7, STRING),
        REQUEST(8, MAP, STRING, STRING),
        RESPONSE(9, OBJECT);

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
        CONTENT_ID(0, UUID_TYPE),
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

    public enum LogTableColumnDef implements ColumnDefinition {
        NODE_ID(1, STRING),
        NODE_ADDRESS(2, STRING),
        DATE(3, DataType.DATE),
        LEVEL(4, STRING),
        CLASS(5, STRING),
        MESSAGE(7, STRING),
        STACKTRACE(8, STRING);

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

        LogTableColumnDef(int pos, DataType mainType, DataType... typeParams) {
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
