package com.cyoda.presto.client.reporting.groups;

import com.cyoda.core.model.reports.DistributedReportInfoView;

import javax.annotation.Nonnull;
import java.util.UUID;

public record GroupsRequestKey(String reportId, UUID groupingVersion) {
    public GroupsRequestKey(@Nonnull String reportId,
                            @Nonnull UUID groupingVersion) {
        this.reportId = reportId;
        this.groupingVersion = groupingVersion;
    }

    public static GroupsRequestKey of(DistributedReportInfoView stats) {
        return new GroupsRequestKey(stats.getId(), stats.getGroupingVersion());
    }

    @Override
    public int hashCode() {
        return reportId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return reportId.equals(((GroupsRequestKey) obj).reportId);
    }

    @Override
    public String toString() {
        return reportId;
    }
}
