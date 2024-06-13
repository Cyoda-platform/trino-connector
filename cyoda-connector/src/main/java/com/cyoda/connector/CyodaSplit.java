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

package com.cyoda.connector;

import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.HostAddress;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.trino.spi.predicate.TupleDomain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class CyodaSplit implements ConnectorSplit {
    private final String queryId;
    private final String userId;
    private final List<HostAddress> addresses;

    private final boolean assignToCoordinator;

    private final String cyodaTableMetaId;

    private final String reportId;

    private final UUID groupingVersion;

    private final String groupJsonBase64;


    private final int page;

    private final int pageSize;

    private final Map<String, ?> customData;
    private TupleDomain<ColumnHandle> constraint;

    @JsonCreator
    public CyodaSplit(String queryId, String userId, List<HostAddress> addresses, boolean assignToCoordinator, String cyodaTableMetaId, String reportId, UUID groupingVersion, String groupJsonBase64, int page, int pageSize, Map<String, ?> customData, TupleDomain<ColumnHandle> constraint) {
        this.queryId = queryId;
        this.userId = userId;
        this.addresses = addresses;
        this.assignToCoordinator = assignToCoordinator;
        this.cyodaTableMetaId = cyodaTableMetaId;
        this.reportId = reportId;
        this.groupingVersion = groupingVersion;
        this.groupJsonBase64 = groupJsonBase64;
        this.page = page;
        this.pageSize = pageSize;
        this.customData = customData;
        this.constraint = constraint;
    }

    public CyodaSplit(String queryId, String userId, String cyodaTableMetaId, String reportId, UUID groupingVersion, String groupJsonBase64){
        this(queryId,
                userId, cyodaTableMetaId,
                reportId,
                groupingVersion,
                groupJsonBase64, null);
    }
    public CyodaSplit(String queryId, String userId, String cyodaTableMetaId, String reportId, UUID groupingVersion, String groupJsonBase64, TupleDomain<ColumnHandle> constraint){
        this(queryId, userId,
                new ArrayList<>(),
                true,
                cyodaTableMetaId,
                reportId,
                groupingVersion,
                groupJsonBase64,
                0,
                Integer.MAX_VALUE, null, constraint);
    }

    public static CyodaSplit emptyCoordinatorSplit(String queryId, String userId, Map<String, ?> customData){
        return new CyodaSplit(queryId,
                userId, new ArrayList<>(),
                true,
                null,
                null,
                null,
                null,
                0,
                Integer.MAX_VALUE, customData, null);
    }
    public static CyodaSplit configSplit(String reportConfigId, String userId, String queryId){
        return new CyodaSplit(queryId,
                userId, new ArrayList<>(),
                false,
                reportConfigId,
                null,
                null,
                null,
                0, Integer.MAX_VALUE, null, null);
    }

    public static CyodaSplit addressedEmptySplit(String queryId, String userId, HostAddress nodeAddress){
        return new CyodaSplit(queryId,
                userId, Collections.singletonList(nodeAddress),
                false,
                null,
                null,
                null,
                null,
                0, Integer.MAX_VALUE, null, null);
    }

    @JsonProperty
    public String getQueryId() {
        return queryId;
    }

    @JsonProperty
    public String getUserId() {
        return userId;
    }

    @JsonProperty
    public boolean isAssignToCoordinator() {
        return assignToCoordinator;
    }

    @JsonProperty
    public String getCyodaTableMetaId() {
        return cyodaTableMetaId;
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

    @JsonProperty
    public TupleDomain<ColumnHandle> getConstraint() {
        return constraint;
    }

    public void setConstraint(TupleDomain<ColumnHandle> constraint) {
        this.constraint = constraint;
    }

    @Override
    public boolean isRemotelyAccessible() {
        return addresses.isEmpty();
    }
    public List<HostAddress> getAddresses() {
        return addresses;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("addresses", addresses)
                .add("queryId", queryId)
                .add("addresses", addresses)
                .add("assignToCoordinator", assignToCoordinator)
                .add("reportConfigId", cyodaTableMetaId)
                .add("reportId", reportId)
                .add("groupingVersion", groupingVersion)
                .add("groupJsonBase64", groupJsonBase64)
                .add("page", page)
                .add("pageSize", pageSize)
                .toString();
    }
}
