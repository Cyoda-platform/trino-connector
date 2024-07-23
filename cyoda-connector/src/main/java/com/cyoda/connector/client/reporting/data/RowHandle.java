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

package com.cyoda.connector.client.reporting.data;

import com.cyoda.connector.handles.ReportSplitHandle;
import com.google.common.base.MoreObjects;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class RowHandle {
    private final String reportId;
    private final UUID groupingVersion;
    private final String groupJsonBase64;
    private final Map<String, Object> reportRow;
    private final long rowNum;

    public RowHandle(ReportSplitHandle splitHandle, Map<String, Object> reportRow,
                     long rowNum) {
        this.reportId = splitHandle.getReportId();
        this.groupingVersion = splitHandle.getGroupingVersion();
        this.groupJsonBase64 = splitHandle.getGroupJsonBase64();
        this.reportRow = reportRow;
        this.rowNum = rowNum;
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

    public String reportId() {
        return reportId;
    }

    public UUID groupingVersion() {
        return groupingVersion;
    }

    public String groupJsonBase64() {
        return groupJsonBase64;
    }

    public Map<String, Object> reportRow() {
        return reportRow;
    }

    public long rowNum() {
        return rowNum;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RowHandle) obj;
        return Objects.equals(this.reportId, that.reportId) &&
                Objects.equals(this.groupingVersion, that.groupingVersion) &&
                Objects.equals(this.groupJsonBase64, that.groupJsonBase64) &&
                Objects.equals(this.reportRow, that.reportRow) &&
                this.rowNum == that.rowNum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportId, groupingVersion, groupJsonBase64, reportRow, rowNum);
    }

}
