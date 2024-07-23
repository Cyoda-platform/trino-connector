package com.cyoda.connector.handles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class ReportSplitHandle {
    private final String reportId;

    private final UUID groupingVersion;

    private final String groupJsonBase64;

    private final int page;

    @JsonCreator
    public ReportSplitHandle(String reportId, UUID groupingVersion, String groupJsonBase64, int page) {
        this.reportId = reportId;
        this.groupingVersion = groupingVersion;
        this.groupJsonBase64 = groupJsonBase64;
        this.page = page;
    }

    @JsonProperty
    public String getReportId() {
        return reportId;
    }

    @JsonProperty
    public UUID getGroupingVersion() {
        return groupingVersion;
    }

    @JsonProperty
    public String getGroupJsonBase64() {
        return groupJsonBase64;
    }

    @JsonProperty
    public int getPage() {
        return page;
    }


    @Override
    public int hashCode() {
        return Objects.hash(getReportId(), getGroupJsonBase64(), getPage());
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "ReportSplitHandle{" +
                "reportId='" + reportId + '\'' +
                ", groupingVersion=" + groupingVersion +
                ", groupJsonBase64='" + groupJsonBase64 + '\'' +
                ", page=" + page +
                '}';
    }
}
