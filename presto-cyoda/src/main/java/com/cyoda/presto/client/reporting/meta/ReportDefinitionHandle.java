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

package com.cyoda.presto.client.reporting.meta;

import com.cyoda.presto.handles.CyodaColumnHandle;
import com.google.common.base.MoreObjects;

import java.util.List;

public class ReportDefinitionHandle {
    public static final String REPORT_ID_COLUMN = "id";
    public static final String REPORT_NAME_COLUMN = "reportName";
    public static final String REPORT_TABLE_NAME_COLUMN = "tableName";
    public static final String REPORT_TYPE_COLUMN = "type";
    public static final String REPORT_DESCRIPTION_COLUMN = "description";
    public static final String REPORT_USER_ID_COLUMN = "userId";
    public static final String REPORT_CREATION_DATE_COLUMN = "creationDate";
    public static final String REPORT_UPDATE_DATE_COLUMN = "lastUpdateDate";

    public static final String REPORT_COLUMNS_COLUMN = "columns";
    public static final String REPORT_JSON_COLUMN = "json";

    final String reportConfigId;
    final String reportName;
    final List<CyodaColumnHandle> columns;
    final String json;
    final String description;

    public ReportDefinitionHandle(String reportConfigId, String reportName, String description, List<CyodaColumnHandle> columns, String json) {
        this.reportConfigId = reportConfigId;
        this.reportName = reportName;
        this.description = description;
        this.columns = columns;
        this.json = json;
    }

    public String getReportConfigId() {
        return reportConfigId;
    }

    public String getReportName() {
        return reportName;
    }

    public String getDescription() {
        return description;
    }

    public List<CyodaColumnHandle> getColumns() {
        return columns;
    }

    public String getJson() {
        return json;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("reportConfigId", reportConfigId)
                .add("reportName", reportName)
                .add("description", description)
                .toString();
    }
}
