/*
 * Copyright (c) 2024 Cyoda Limited. All rights reserved.
 * This software is the confidential and proprietary information of Cyoda Limited ("Confidential Information").
 * Unauthorized use, disclosure, distribution, or reproduction is prohibited. Any use or access to this software
 * is subject to the terms of the applicable agreements and prior written consent from Cyoda Limited.
 */

package com.cyoda.core.model.reports;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


import java.util.UUID;


public class DRExecutionKey {


    private final String reportId;

    private final UUID groupingVersion;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DRExecutionKey(@JsonProperty(value = "reportId",required = true) String reportId,
                          @JsonProperty(value = "groupingVersion") UUID groupingVersion) {
        this.reportId = reportId;
        this.groupingVersion = groupingVersion;
    }

    public String getReportId() {
        return reportId;
    }

    public UUID getGroupingVersion() {
        return groupingVersion;
    }

}
