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

import com.cyoda.service.api.beans.ReportRow;
import com.google.common.base.MoreObjects;

public class RowHandle {

    final String reportId;
    final String groupingVersion;
    final String groupJsonBase64;
    final ReportRow reportRow;

    public RowHandle(String reportId, String groupingVersion, String groupJsonBase64, ReportRow reportRow) {
        this.reportId = reportId;
        this.groupingVersion = groupingVersion;
        this.groupJsonBase64 = groupJsonBase64;
        this.reportRow = reportRow;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("reportId", reportId)
                .add("groupingVersion", groupingVersion)
                .add("groupJsonBase64", groupJsonBase64)
                .add("reportRow", reportRow)
                .toString();
    }
}
