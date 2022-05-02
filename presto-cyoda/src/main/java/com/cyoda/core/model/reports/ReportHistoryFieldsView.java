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

import java.util.HashMap;
import java.util.Map;

public class ReportHistoryFieldsView {

    public static final String CREATE_TIME_COLUMN_NAME = "createTime";
    public static final String TYPE_COLUMN_NAME = "type";
    public static final String STATUS_NAME_COLUMN_NAME = "status";
    public static final String REPORT_ID_COLUMN_NAME = "reportId";
    public static final String HIERARHY_ENABLE_COLUMN_NAME = "hierarhyEnable";
    public static final String GROUPING_VERSION_COLUMN_NAME = "groupingVersion";
    public static final String GROUPING_COLUMNS_COLUMN_NAME = "groupingColumns";
    public static final String USER_NAME_COLUMN_NAME = "username";

    private final Map<String, Object> reportHistoryFields = new HashMap<>();

    public Map<String, Object> getReportHistoryFields() {
        return reportHistoryFields;
    }
}
