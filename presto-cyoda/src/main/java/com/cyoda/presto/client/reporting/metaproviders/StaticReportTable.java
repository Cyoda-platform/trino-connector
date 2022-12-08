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
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_REPORT_NAME_VARIABLE;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_STATUS_NAME_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_TYPE_COLUMN;
import static com.cyoda.core.model.reports.ReportHistoryFieldsView.HISTORY_USER_NAME_COLUMN;
import static com.cyoda.presto.client.reporting.BaseReportsApiHandler.REPORT_ENDPOINT;
import static com.cyoda.presto.client.reporting.data.ReportRowsApiHandler.ROW_GROUP_JSON_BASE64_VARIABLE;
import static com.cyoda.presto.client.reporting.data.ReportRowsApiHandler.ROW_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.data.ReportRowsApiHandler.ROW_REPORT_ROW_NUMBER_COLUMN;
import static com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler.GROUPING_VERSION_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler.REPORT_DEFS_ENDPOINT;
import static com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler.REPORT_DETAILS_ENDPOINT;
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
import static com.cyoda.presto.client.types.DataType.LIST;
import static com.cyoda.presto.client.types.DataType.LOCAL_DATE_TIME;
import static com.cyoda.presto.client.types.DataType.LONG;
import static com.cyoda.presto.client.types.DataType.OBJECT;
import static com.cyoda.presto.client.types.DataType.STRING;
import static com.cyoda.presto.client.types.DataType.UUID_TYPE;

public enum StaticReportTable implements TableDefinition {
    REPORTS(Arrays.asList(ReportsColumnDef.values()), REPORT_DEFS_ENDPOINT),
    REPORT_DETAILS(Arrays.asList(ReportDetailsColumnDef.values()), REPORT_DETAILS_ENDPOINT),
    REPORT_STATS(StandardColumnDefinition.builder()
            .add(DistributedReportInfoView.meta())
            .build(), REPORT_ENDPOINT),
    REPORT_HISTORIES(Arrays.asList(ReportHistoryColumnDef.values()), REPORT_HISTORY_ENDPOINT),
    REPORT_GROUPS(StandardColumnDefinition.builder()
            .add(new StandardColumnDefinition(0, HISTORY_REPORT_ID_COLUMN, STRING))
            .add(new StandardColumnDefinition(1, GROUPING_VERSION_COLUMN, UUID_TYPE))
            .add(new StandardColumnDefinition(2, HISTORY_REPORT_NAME_VARIABLE, STRING))
            .add(GroupHeader.meta())
            .build(), REPORT_ENDPOINT),
    REPORT_ROWS(StandardColumnDefinition.builder()
            .add(new StandardColumnDefinition(0, ROW_REPORT_ROW_NUMBER_COLUMN, LONG))
            .add(new StandardColumnDefinition(1, ROW_REPORT_ID_COLUMN, STRING))
            .add(new StandardColumnDefinition(2, GROUPING_VERSION_COLUMN, UUID_TYPE))
            .add(new StandardColumnDefinition(3, ROW_GROUP_JSON_BASE64_VARIABLE, STRING))
            .build(), REPORT_ENDPOINT);

    private final Map<String, ColumnDefinition> columns;
    private final String endpoint;


    StaticReportTable(List<ColumnDefinition> columns, String endpoint) {
        this.columns = columns.stream().collect(Collectors.toMap(ColumnDefinition::getFieldName, Function.identity()));
        this.endpoint = endpoint;
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
    public String getRequestHandlerKey() {
        return name();
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
        UPDATE_DATE(7, REPORT_UPDATE_DATE_COLUMN, LOCAL_DATE_TIME);

        private final int pos;
        private final String fieldName;
        private final CompoundDataType dataType;

        ReportsColumnDef(int pos, String fieldName, DataType dateType) {
            this.pos = pos;
            this.fieldName = fieldName;
            this.dataType = new CompoundDataType(fieldName, dateType);
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

    enum ReportDetailsColumnDef implements ColumnDefinition {
        ID(0, REPORT_ID_COLUMN, STRING),
        REPORT_NAME(1, REPORT_NAME_COLUMN, STRING),
        REPORT_COLUMNS(2, REPORT_COLUMNS_COLUMN, LIST, OBJECT),
        REPORT_JSON(3, REPORT_JSON_COLUMN, STRING);

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

        ReportDetailsColumnDef(int pos, String fieldName, DataType dateType, DataType... parType) {
            this.pos = pos;
            this.fieldName = fieldName;
            this.dataType = new CompoundDataType(fieldName, dateType, parType);
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
        ID(0, HISTORY_REPORT_ID_COLUMN, STRING),
        REPORT_NAME(1, HISTORY_REPORT_NAME_VARIABLE, STRING),
        CREATION_DATE(2, HISTORY_CREATE_TIME_COLUMN, LOCAL_DATE_TIME),
        TYPE(3, HISTORY_TYPE_COLUMN, STRING),
        STATUS(4, HISTORY_STATUS_NAME_COLUMN, STRING),
        HIERARCHY_ENABLE(5, HISTORY_HIERARHY_ENABLE_COLUMN, BOOLEAN),
        // For Trino this can be a UUID, but Presto wants VARCHAR.
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
