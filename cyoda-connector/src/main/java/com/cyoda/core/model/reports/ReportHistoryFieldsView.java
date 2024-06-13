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

package com.cyoda.core.model.reports;

import com.cyoda.connector.client.reporting.metaproviders.StaticReportFields;

import java.util.Map;
import java.util.UUID;

public class ReportHistoryFieldsView {

    // TODO: Any field of the DistributedReport class can be selected. These are not all of them.
    public static final String HISTORY_REPORT_NAME_VARIABLE = "configName";

    public static final String HISTORY_CREATE_TIME_COLUMN = "createTime";
    public static final String HISTORY_TYPE_COLUMN = "type";
    public static final String HISTORY_HIERARHY_ENABLE_COLUMN = "hierarhyEnable";
    public static final String HISTORY_GROUPING_VERSION_COLUMN = "groupingVersion";
    public static final String HISTORY_GROUPING_COLUMNS_COLUMN = "groupingColumns";
    public static final String HISTORY_USER_NAME_COLUMN = "userId";

    private final Map<String, Object> reportHistoryFields;

    public ReportHistoryFieldsView(Map<String, Object> reportHistoryFields) {
        this.reportHistoryFields = reportHistoryFields;
    }

    public Map<String, Object> getReportHistoryFields() {
        return reportHistoryFields;
    }

    public String getReportId(){
        return (String) reportHistoryFields.get(StaticReportFields.HISTORY_REPORT_ID_COLUMN);
    }

    public UUID getGroupingVersion(){
        return UUID.fromString((String) reportHistoryFields.get(HISTORY_GROUPING_VERSION_COLUMN));

    }
}
