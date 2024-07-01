package com.cyoda.connector.client.treenode.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReportRequestDto {
    private final String reportId;
    private final UUID groupingVersion;
    private final String groupJsonBase64;
    private final Long startRow;
    private final Long endRow;

    @JsonCreator
    public ReportRequestDto(@JsonProperty("reportId") String reportId,
                            @JsonProperty("groupingVersion") UUID groupingVersion,
                            @JsonProperty("groupJsonBase64") String groupJsonBase64,
                            @JsonProperty("startRow") Long startRow,
                            @JsonProperty("endRow") Long endRow) {
        this.reportId = reportId;
        this.groupingVersion = groupingVersion;
        this.groupJsonBase64 = groupJsonBase64;
        this.startRow = startRow;
        this.endRow = endRow;
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
    public long getStartRow() {
        return startRow;
    }

    @JsonProperty
    public long getEndRow() {
        return endRow;
    }

    public Map<String, String> toMap() {
        Map<String, String> res = new HashMap<>();
        res.put("reportId", getReportId());
        res.put("groupingVersion", getGroupingVersion() == null ? null : getGroupingVersion().toString());
        res.put("groupJsonBase64", getGroupJsonBase64());
        res.put("startRow", String.valueOf(getStartRow()));
        res.put("endRow", String.valueOf(getEndRow()));
        return res;
    }
}
