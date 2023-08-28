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

package com.cyoda.presto;

import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.HostAddress;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class CyodaSplit implements ConnectorSplit {
    private final String queryId;
    private final List<HostAddress> addresses;

    private final boolean assignToCoordinator;

    private final String reportConfigId;

    private final String reportId;

    private final UUID groupingVersion;

    private final String groupJsonBase64;


    private final int page;

    private final int pageSize;

    private final Map<String, ?> customData;

    @JsonCreator
    public CyodaSplit(String queryId, List<HostAddress> addresses, boolean assignToCoordinator, String tableName, String reportConfigId, String reportId, UUID groupingVersion, String groupJsonBase64, int page, int pageSize, Map<String, ?> customData) {
        this.queryId = queryId;
        this.addresses = addresses;
        this.assignToCoordinator = assignToCoordinator;
        this.reportConfigId = reportConfigId;
        this.reportId = reportId;
        this.groupingVersion = groupingVersion;
        this.groupJsonBase64 = groupJsonBase64;
        this.page = page;
        this.pageSize = pageSize;
        this.customData = customData;
    }

    public CyodaSplit(String queryId, String tableName, String reportConfigId, String reportId, UUID groupingVersion, String groupJsonBase64){
        this(queryId,
                new ArrayList<>(),
                true,
                tableName,
                reportConfigId,
                reportId,
                groupingVersion,
                groupJsonBase64,
                0,
                Integer.MAX_VALUE, null);
    }

    public static CyodaSplit emptyCoordinatorSplit(String tableName, String queryId, Map<String, ?> customData){
        return new CyodaSplit(queryId,
                new ArrayList<>(),
                true,
                tableName,
                null,
                null,
                null,
                null,
                0,
                Integer.MAX_VALUE, customData);
    }
    public static CyodaSplit configSplit(String tableName, String reportConfigId, String queryId){
        return new CyodaSplit(queryId,
                new ArrayList<>(),
                false,
                tableName,
                reportConfigId,
                null,
                null,
                null,
                0, Integer.MAX_VALUE, null);
    }

    public static CyodaSplit addressedEmptySplit(CyodaTableHandle tableHandle, String queryId, URI nodeAddress){
        return new CyodaSplit(queryId,
                Collections.singletonList(HostAddress.fromUri(nodeAddress)),
                false,
                tableHandle.getTableName(),
                null,
                null,
                null,
                null,
                0, Integer.MAX_VALUE, null);
    }

    @JsonProperty
    public String getQueryId() {
        return queryId;
    }

    @JsonProperty
    public boolean isAssignToCoordinator() {
        return assignToCoordinator;
    }

    @JsonProperty
    public String getReportConfigId() {
        return reportConfigId;
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

    @JsonProperty
    public int getPageSize() {
        return pageSize;
    }

    @JsonProperty
    public Map<String, ?> getCustomData() {
        return customData;
    }

    @Override
    public boolean isRemotelyAccessible() {
        return addresses.isEmpty();
    }
    public List<HostAddress> getAddresses() {
        return addresses;
    }

    @Override
    public Object getInfo() {
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("addresses", addresses)
                .add("queryId", queryId)
                .add("addresses", addresses)
                .add("assignToCoordinator", assignToCoordinator)
                .add("reportConfigId", reportConfigId)
                .add("reportId", reportId)
                .add("groupingVersion", groupingVersion)
                .add("groupJsonBase64", groupJsonBase64)
                .add("page", page)
                .add("pageSize", pageSize)
                .toString();
    }
}
