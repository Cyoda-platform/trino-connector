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

import io.airlift.configuration.Config;

@SuppressWarnings("UnstableApiUsage")
public class CyodaConfig {

    private static final String DEFAULT_REPORTING_SCHEMA_NAME = "reporting";
    private static final String DEFAULT_MAINTENANCE_SCHEMA_NAME = "maintenance";
    private static final int DEFAULT_REQUEST_PAGE_SIZE = 10;

    private boolean logApiCallStats;
    private boolean logApiCallResponse;
    private long apiCallStatsMaxRecords;
    private long pushdownLogMaxRecords;
    private String reportingSchemaName;
    private String maintenanceSchemaName;
    private boolean testingMode;
    private int rowRequestPageSize;
    private long cacheUserAuthSecAfterWrite;
    private long cacheReportHistorySecAfterWrite;
    private long cacheReportMetaHoursAfterAccess;
    private long cacheReportPagesHoursAfterAccess;
    private long cacheReportGroupsHoursAfterAccess;

    private boolean anonymousLogin;
    private String anonymousUserId;
    private int predicatePushdownThreshold;

    private String rSocketBindAddress;
    private int rSocketPort;

    public CyodaConfig() {
        setDefaults();
    }


    private void setDefaults() {
        reportingSchemaName = DEFAULT_REPORTING_SCHEMA_NAME;
        maintenanceSchemaName = DEFAULT_MAINTENANCE_SCHEMA_NAME;
        testingMode = false;
        rowRequestPageSize = DEFAULT_REQUEST_PAGE_SIZE;
        logApiCallStats = false;
        logApiCallResponse = true; //does not matter if logApiCallStats = false
        apiCallStatsMaxRecords = 10000;
        pushdownLogMaxRecords = 10000;
        predicatePushdownThreshold = 10000;
        anonymousLogin = false;
        anonymousUserId = null;
        cacheUserAuthSecAfterWrite = 5;
        cacheReportHistorySecAfterWrite = 30;
        cacheReportMetaHoursAfterAccess = 24;
        cacheReportPagesHoursAfterAccess = 24;
        cacheReportGroupsHoursAfterAccess = 24;
        rSocketBindAddress="localhost";
        rSocketPort=7000;
    }


    public boolean getLogApiCallStats() {
        return logApiCallStats;
    }

    @Config("cyoda.connector.log-api-call-stats")
    public void setLogApiCallStats(boolean logApiCallStats) {
        this.logApiCallStats = logApiCallStats;
    }

    public boolean getLogApiCallResponse() {
        return logApiCallResponse;
    }

    @Config("cyoda.connector.log-api-call-response")
    public void setLogApiCallResponse(boolean logApiCallResponse) {
        this.logApiCallResponse = logApiCallResponse;
    }

    public long getApiCallStatsMaxRecords() {
        return apiCallStatsMaxRecords;
    }

    @Config("cyoda.connector.api-call-stats-max-records")
    public void setApiCallStatsMaxRecords(long apiCallStatsMaxRecords) {
        this.apiCallStatsMaxRecords = apiCallStatsMaxRecords;
    }

    public long getPushdownLogMaxRecords() {
        return pushdownLogMaxRecords;
    }

    @Config("cyoda.connector.condition-pushdown-log-max-records")
    public void setPushdownLogMaxRecords(long pushdownLogMaxRecords) {
        this.pushdownLogMaxRecords = pushdownLogMaxRecords;
    }

    public String getReportingSchemaName() {
        return reportingSchemaName;
    }

    @Config("cyoda.connector.reporting-schema-name")
    public CyodaConfig setReportingSchemaName(String reportingSchemaName) {
        this.reportingSchemaName = reportingSchemaName;
        return this;
    }

    public String getMaintenanceSchemaName() {
        return maintenanceSchemaName;
    }

    @Config("cyoda.connector.maintenance-schema-name")
    public CyodaConfig setMaintenanceSchemaName(String maintenanceSchemaName) {
        this.maintenanceSchemaName = maintenanceSchemaName;
        return this;
    }

    public boolean isTestingMode() {
        return testingMode;
    }
    @Config("cyoda.connector.testing-mode")
    public void setTestingMode(boolean testingMode) {
        this.testingMode = testingMode;
    }

    public int getRowRequestPageSize() {
        return rowRequestPageSize;
    }

    @Config("cyoda.connector.row-request-page-size")
    public void setRowRequestPageSize(int rowRequestPageSize) {
        this.rowRequestPageSize = rowRequestPageSize;
    }


    @Config("cyoda.connector.allow-anonymous-login")
    public void setAnonymousLogin(boolean anonymousLogin) {
        this.anonymousLogin = anonymousLogin;
    }

    public boolean isAnonymousLogin() {
        return anonymousLogin;
    }

    @Config("cyoda.connector.anonymous-user-id")
    public void setAnonymousUserId(String anonymousUserId) {
        this.anonymousUserId = anonymousUserId;
    }

    public String getAnonymousUserId() {
        return anonymousUserId;
    }

    public long getCacheUserAuthSecAfterWrite() {
        return cacheUserAuthSecAfterWrite;
    }

    @Config("cyoda.cache.auth.saw")
    public void setCacheUserAuthSecAfterWrite(long cacheUserAuthSecAfterWrite) {
        this.cacheUserAuthSecAfterWrite = cacheUserAuthSecAfterWrite;
    }

    public long getCacheReportHistorySecAfterWrite() {
        return cacheReportHistorySecAfterWrite;
    }

    @Config("cyoda.cache.history.saw")
    public void setCacheReportHistorySecAfterWrite(long cacheReportHistorySecAfterWrite) {
        this.cacheReportHistorySecAfterWrite = cacheReportHistorySecAfterWrite;
    }

    public long getCacheReportMetaHoursAfterAccess() {
        return cacheReportMetaHoursAfterAccess;
    }

    @Config("cyoda.cache.meta.haa")
    public void setCacheReportMetaHoursAfterAccess(long cacheReportMetaHoursAfterAccess) {
        this.cacheReportMetaHoursAfterAccess = cacheReportMetaHoursAfterAccess;
    }

    public long getCacheReportPagesHoursAfterAccess() {
        return cacheReportPagesHoursAfterAccess;
    }

    @Config("cyoda.cache.pages.haa")
    public void setCacheReportPagesHoursAfterAccess(long cacheReportPagesHoursAfterAccess) {
        this.cacheReportPagesHoursAfterAccess = cacheReportPagesHoursAfterAccess;
    }

    public long getCacheReportGroupsHoursAfterAccess() {
        return cacheReportGroupsHoursAfterAccess;
    }

    @Config("cyoda.cache.groups.haa")
    public void setCacheReportGroupsHoursAfterAccess(long cacheReportGroupsHoursAfterAccess) {
        this.cacheReportGroupsHoursAfterAccess = cacheReportGroupsHoursAfterAccess;
    }

    public int getPredicatePushdownThreshold() {
        return predicatePushdownThreshold;
    }

    @Config("cyoda.connector.tree-node.pushdown-threshold")
    public void setPredicatePushdownThreshold(int predicatePushdownThreshold) {
        this.predicatePushdownThreshold = predicatePushdownThreshold;
    }

    public String getRSocketBindAddress() {
        return this.rSocketBindAddress;
    }
    @Config("cyoda.connector.rsocket.bind-address")
    public void setRSocketBindAddress(String bindAddress) {
        this.rSocketBindAddress = bindAddress;
    }

    public int getRSocketPort() {
        return this.rSocketPort;
    }
    @Config("cyoda.connector.rsocket.port")
    public void setRSocketPort(int port) {
        this.rSocketPort = port;
    }

}
