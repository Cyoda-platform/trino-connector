package com.cyoda.presto.client.reporting.data;

import com.cyoda.core.model.reports.DistributedReportInfoView;
import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.client.reporting.groups.GroupingHandle;

import javax.annotation.Nonnull;
import java.util.UUID;

public record RowsRequestKey(String reportId, UUID groupingVersion, String groupJsonBase64) {
    public RowsRequestKey(@Nonnull String reportId,
                          @Nonnull UUID groupingVersion,
                          @Nonnull String groupJsonBase64) {
        this.reportId = reportId;
        this.groupingVersion = groupingVersion;
        this.groupJsonBase64 = groupJsonBase64;
    }

    public static RowsRequestKey of(CyodaSplit split) {
        return new RowsRequestKey(
                split.getReportId(),
                split.getGroupingVersion(),
                split.getGroupJsonBase64());
    }

    @Override
    public int hashCode() {
        return reportId.hashCode() + groupJsonBase64.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        RowsRequestKey other = (RowsRequestKey) obj;
        return reportId.equals(other.reportId) && groupJsonBase64.equals(other.groupJsonBase64);
    }

    @Override
    public String toString() {
        return reportId + "(" + groupJsonBase64 + ")";
    }
}
