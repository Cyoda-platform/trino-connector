package com.cyoda.presto.client.reporting.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.handles.CyodaTableHandle;

import java.util.Objects;
import java.util.UUID;

public final class DataRequestKey {
    // for logging
    private transient final String queryId;

    // actual payload
    private transient final CyodaTableHandle tableHandle;
    private transient final CyodaSplit split;

    // extracted key fields of split to use in equals & hashCode
    private final String reportConfigId;
    private final String reportId;
    private final UUID groupingVersion;
    private final String groupJsonBase64;
    private final int page;

    private DataRequestKey(
            String queryId,
            CyodaTableHandle tableHandle, CyodaSplit split, String reportConfigId,
            String reportId,
            UUID groupingVersion,
            String groupJsonBase64,
            int page
    ) {
        this.queryId = queryId;
        this.tableHandle = tableHandle;
        this.split = split;
        this.reportConfigId = reportConfigId;
        this.reportId = reportId;
        this.groupingVersion = groupingVersion;
        this.groupJsonBase64 = groupJsonBase64;
        this.page = page;
    }

    public DataRequestKey(CyodaSplit split, CyodaTableHandle tableHandle) {
        this(
                split.getQueryId(),
                tableHandle,
                split,
                split.getReportConfigId(),
                split.getReportId(),
                split.getGroupingVersion(),
                split.getGroupJsonBase64(),
                split.getPage()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        DataRequestKey other = (DataRequestKey) obj;
        return Objects.equals(reportConfigId, other.reportConfigId) &&
                Objects.equals(reportId, other.reportId) &&
                Objects.equals(groupJsonBase64, other.groupJsonBase64) &&
                page == other.page;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportConfigId, reportId, groupJsonBase64, page);
    }

    public String getQueryId() {
        return queryId;
    }

    public CyodaTableHandle getTableHandle() {
        return tableHandle;
    }

    public CyodaSplit getSplit() {
        return split;
    }

    public String getReportConfigId() {
        return reportConfigId;
    }

    public String getReportId() {
        return reportId;
    }

    public UUID getGroupingVersion() {
        return groupingVersion;
    }

    public String getGroupJsonBase64() {
        return groupJsonBase64;
    }

    public int getPage() {
        return page;
    }

    @Override
    public String toString() {
        return "DataRequestKey[" +
                "queryId=" + queryId + ", " +
                "reportConfigId=" + reportConfigId + ", " +
                "reportId=" + reportId + ", " +
                "groupingVersion=" + groupingVersion + ", " +
                "groupJsonBase64=" + groupJsonBase64 + ", " +
                "page=" + page + ']';
    }

}
