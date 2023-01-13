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

import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.HostAddress;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class CyodaSplit implements ConnectorSplit {
    private final String queryId;
    private final List<HostAddress> addresses;

    private final String tableName;

    private final String reportConfigId;

    private final String reportId;

    private final UUID groupingVersion;

    private final String groupJsonBase64;


    private final int page;

    private final int size;


    @JsonCreator
    public CyodaSplit(String queryId, List<HostAddress> addresses, String tableName, String reportConfigId, String reportId, UUID groupingVersion, String groupJsonBase64, int page, int size) {
        this.queryId = queryId;
        this.addresses = addresses;
        this.tableName = tableName;
        this.reportConfigId = reportConfigId;
        this.reportId = reportId;
        this.groupingVersion = groupingVersion;
        this.groupJsonBase64 = groupJsonBase64;
        this.page = page;
        this.size = size;
    }

    public CyodaSplit(String tableName, String reportConfigId, String reportId, UUID groupingVersion, String groupJsonBase64, String queryId){
        this(queryId, Collections.emptyList(), tableName, reportConfigId, reportId, groupingVersion, groupJsonBase64, 0, Integer.MAX_VALUE-1);
    }

    public static CyodaSplit emptySplit(String tableName){
        return new CyodaSplit(tableName, null, null, null, null, null);
    }

    @JsonProperty
    public String getQueryId() {
        return queryId;
    }

    @JsonProperty
    public String getTableName() {
        return tableName;
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
    public int getSize() {
        return size;
    }

    @Override
    public boolean isRemotelyAccessible() {
        return true;
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
                .toString();
    }
}
