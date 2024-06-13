package com.cyoda.connector.client.reporting.data;

import com.cyoda.connector.CyodaSplit;
import com.cyoda.connector.handles.CyodaTableMeta;

import java.util.Objects;
import java.util.UUID;

public final class DataRequestKey {
    // for logging
    private transient final String queryId;

    // actual payload
    private transient final CyodaTableMeta tableHandle;
    private transient final CyodaSplit split;

    // extracted key fields of split to use in equals & hashCode
    private final String reportConfigId;
    private final String reportId;
    private final UUID groupingVersion;
    private final String groupJsonBase64;
    private final int page;
    private final int pageSize;

    private DataRequestKey(
            String queryId,
            CyodaTableMeta tableHandle, CyodaSplit split, String reportConfigId,
            String reportId,
            UUID groupingVersion,
            String groupJsonBase64,
            int page,
            int pageSize) {
        this.queryId = queryId;
        this.tableHandle = tableHandle;
        this.split = split;
        this.reportConfigId = reportConfigId;
        this.reportId = reportId;
        this.groupingVersion = groupingVersion;
        this.groupJsonBase64 = groupJsonBase64;
        this.page = page;
        this.pageSize = pageSize;
    }

    public DataRequestKey(CyodaSplit split, CyodaTableMeta tableHandle) {
        this(
                split.getQueryId(),
                tableHandle,
                split,
                split.getCyodaTableMetaId(),
                split.getReportId(),
                split.getGroupingVersion(),
                split.getGroupJsonBase64(),
                split.getPage(),
                split.getPageSize());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        DataRequestKey other = (DataRequestKey) obj;
        return Objects.equals(reportConfigId, other.reportConfigId) &&
                Objects.equals(reportId, other.reportId) &&
                Objects.equals(groupJsonBase64, other.groupJsonBase64) &&
                page == other.page &&
                pageSize == other.pageSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportConfigId, reportId, groupJsonBase64, page);
    }

    public String getQueryId() {
        return queryId;
    }

    public CyodaTableMeta getTableHandle() {
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
