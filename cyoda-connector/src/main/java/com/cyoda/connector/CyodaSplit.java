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

import com.cyoda.connector.handles.CyodaTableHandle;
import com.cyoda.connector.handles.ReportSplitHandle;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.HostAddress;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class CyodaSplit implements ConnectorSplit {
    private final String queryId;
    private final String userId;
    private final List<HostAddress> addresses;
    private final boolean assignToCoordinator;
    private final CyodaTableHandle tableHandle;
    private final ReportSplitHandle reportHandle;

    @JsonCreator
    public CyodaSplit(String queryId,
                      String userId,
                      List<HostAddress> addresses,
                      boolean assignToCoordinator,
                      CyodaTableHandle tableHandle,
                      ReportSplitHandle reportHandle) {
        this.queryId = queryId;
        this.userId = userId;
        this.addresses = addresses;
        this.assignToCoordinator = assignToCoordinator;
        this.tableHandle = Objects.requireNonNull(tableHandle);
        this.reportHandle = reportHandle;
    }

    //groups and histories
    public CyodaSplit(String queryId,
                      String userId,
                      CyodaTableHandle tableHandle,
                      ReportSplitHandle reportHandle){
        this(queryId,
                userId, new ArrayList<>(), false, tableHandle, reportHandle);
    }
    //TREE NODE SPLITS
    public CyodaSplit(String queryId, String userId, CyodaTableHandle tableHandle){
        this(queryId, userId,
                new ArrayList<>(),
                false,
                tableHandle, null);
    }


    public static CyodaSplit addressedEmptySplit(String queryId, String userId, HostAddress nodeAddress, CyodaTableHandle tableHandle){
        return new CyodaSplit(queryId,
                userId, Collections.singletonList(nodeAddress),
                false,
                tableHandle,
                null);
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

    @Override
    public boolean isRemotelyAccessible() {
        return addresses.isEmpty();
    }
    public List<HostAddress> getAddresses() {
        return addresses;
    }
    @JsonProperty
    public CyodaTableHandle getTableHandle() {
        return tableHandle;
    }
    @JsonProperty
    public ReportSplitHandle getReportHandle() {
        return reportHandle;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("addresses", addresses)
                .add("queryId", queryId)
                .add("addresses", addresses)
                .add("assignToCoordinator", assignToCoordinator)
                .add("tableHandle", tableHandle)
                .add("reportHandle", reportHandle)
                .toString();
    }
}
