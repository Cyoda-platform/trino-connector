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

package com.cyoda.presto.client.neededatcyoda;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReportDefinitionsView {

    public static final String REPORT_ID_COLUMN_NAME = "id";
    public static final String REPORT_NAME_COLUMN_NAME = "reportName";
    public static final String REPORT_JSON_COLUMN_NAME = "json";

    public final String id;
    public final String reportName;
    public final String json;

    @JsonCreator
    public ReportDefinitionsView(
            @JsonProperty("id") String id,
            @JsonProperty("reportName") String reportName,
            @JsonProperty("json") String json
    ) {
        this.id = id;
        this.reportName = reportName;
        this.json = json;
    }

    @JsonProperty
    public String getId() {
        return id;
    }

    @JsonProperty
    public String getReportName() {
        return reportName;
    }

    @JsonProperty
    public String getJson() {
        return json;
    }
}
