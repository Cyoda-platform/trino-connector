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

import com.google.common.net.HostAndPort;
import io.airlift.configuration.Config;
import io.airlift.configuration.ConfigurationFactory;

import javax.validation.constraints.NotNull;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
public class CyodaConfig {

    private static final int DEFAULT_HTTP_MAX_IDLE = 20;
    private static final int DEFAULT_HTTP_KEEP_ALIVE = 20;
    private static final int DEFAULT_HTTP_CONNECTION_TIMEOUT = 30;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
    private static final boolean DEFAULT_HTTPS_OVERRIDE = false;
    private static final String DEFAULT_SCHEMA_NAME = "reporting";
    private static final int DEFAULT_REQUEST_PAGE_SIZE = 10;
    private static final String DEFAULT_LOGIN_ENDPOINT = "api/auth/login";
    private static final String DEFAULT_REFERSH_ENDPOINT = "api/auth/token";

    private URL serverUrl;
    private CyodaAuthenticationType cyodaAuthenticationType = CyodaAuthenticationType.NONE;
    private String basicAuthenticationUsername;
    private String basicAuthenticationPassword;
    private HostAndPort socksHostAndPort;
    private HostAndPort httpHostAndPort;
    private long httpConnectionTimeout;
    private TimeUnit timeUnit;
    private int maxHttpIdle;
    private long maxHttpKeepalive;
    private boolean httpsOverride;
    private boolean logApiCallStats;
    private boolean logApiCallResponse;
    private long apiCallStatsMaxRecords;
    private String schemaName;
    private int requestPageSize;
    private int rowRequestPageSize;
    private String userLoginEndpoint;
    private String refreshTokenEndpoint;
    private long cacheUserAuthSecAfterWrite;
    private long cacheReportHistorySecAfterWrite;
    private long cacheReportMetaHoursAfterAccess;
    private long cacheReportPagesHoursAfterAccess;
    private long cacheReportGroupsHoursAfterAccess;

    private boolean anonymousLogin;
    private String anonymousUserId;
    private String anonymousToken;
    private String anonymousRefreshToken;
    private String anonymousUserName;

    private int predicatePushdownThreshold;

    public CyodaConfig() {
        setDefaults();
    }


    private void setDefaults() {
        timeUnit = DEFAULT_TIME_UNIT;
        httpConnectionTimeout = DEFAULT_HTTP_CONNECTION_TIMEOUT;
        maxHttpIdle = DEFAULT_HTTP_MAX_IDLE;
        maxHttpKeepalive = DEFAULT_HTTP_KEEP_ALIVE;
        httpsOverride = DEFAULT_HTTPS_OVERRIDE;
        schemaName = DEFAULT_SCHEMA_NAME;
        requestPageSize = DEFAULT_REQUEST_PAGE_SIZE;
        rowRequestPageSize = DEFAULT_REQUEST_PAGE_SIZE;
        userLoginEndpoint = DEFAULT_LOGIN_ENDPOINT;
        refreshTokenEndpoint = DEFAULT_REFERSH_ENDPOINT;
        logApiCallStats = false;
        logApiCallResponse = true; //does not matter if logApiCallStats = false
        apiCallStatsMaxRecords = 10000;
        anonymousLogin = false;
        anonymousUserId = null;
        anonymousToken = null;
        anonymousRefreshToken = null;
        anonymousUserName = null;
        cacheUserAuthSecAfterWrite = 5;
        cacheReportHistorySecAfterWrite = 30;
        cacheReportMetaHoursAfterAccess = 24;
        cacheReportPagesHoursAfterAccess = 24;
        cacheReportGroupsHoursAfterAccess = 24;
    }

    @NotNull
    public URL getServerUrl() {
        return serverUrl;
    }

    /**
     * The URL of the Cyoda API to be connected to.
     * <p>
     * About the {@link Config} annotation:
     * A {@link ConfigurationFactory} instantiated with the Map of properties
     * <pre>
     *      Map<String, String> properties = new ImmutableMap.Builder<String, String>()
     *                 .put("baseUrl", "https://demo.cyoda.com")
     *                 .put("accessToken", "bla-bla-bla")
     *                 .build();
     * </pre>
     * will build an Example config with these properties injected.
     * <pre>
     *         ConfigurationFactory configurationFactory = new ConfigurationFactory(properties);
     *         return configurationFactory.build(configClass);
     *
     * @param serverUrl the server url
     * @return the CyodaConfig
     */
    @Config("cyoda.presto.server.url")
    public CyodaConfig setServerUrl(URL serverUrl) {
        this.serverUrl = serverUrl;
        return this;
    }

    @NotNull
    public CyodaAuthenticationType getCyodaAuthenticationType() {
        return cyodaAuthenticationType;
    }

    @Config("cyoda.presto.authentication.type")
    public CyodaConfig setCyodaAuthenticationType(CyodaAuthenticationType cyodaAuthenticationType) {
        if (cyodaAuthenticationType != null) {
            this.cyodaAuthenticationType = cyodaAuthenticationType;
        }
        return this;
    }

    public String getBasicAuthenticationUsername() {
        return basicAuthenticationUsername;
    }

    @Config("cyoda.presto.basic-authentication.username")
    public CyodaConfig setBasicAuthenticationUsername(String basicAuthenticationUsername) {
        this.basicAuthenticationUsername = basicAuthenticationUsername;
        return this;
    }

    public String getBasicAuthenticationPassword() {
        return basicAuthenticationPassword;
    }

    @Config("cyoda.presto.basic-authentication.password")
    public CyodaConfig setBasicAuthenticationPassword(String basicAuthenticationPassword) {
        this.basicAuthenticationPassword = basicAuthenticationPassword;
        return this;
    }

    public HostAndPort getSocksProxy() {
        return socksHostAndPort;
    }

    @Config("cyoda.presto.socks-proxy")
    public CyodaConfig setSocksProxy(HostAndPort socksHostAndPort) {
        this.socksHostAndPort = socksHostAndPort;
        return this;
    }

    public HostAndPort getHttpProxy() {
        return httpHostAndPort;
    }

    @Config("cyoda.presto.http-proxy")
    public CyodaConfig setHttpProxy(HostAndPort httpHostAndPort) {
        this.httpHostAndPort = httpHostAndPort;
        return this;
    }

    public int getMaxHttpIdle() {
        return maxHttpIdle;
    }

    @Config("cyoda.presto.max-idle")
    public CyodaConfig setMaxHttpIdle(int maxHttpIdle) {
        this.maxHttpIdle = maxHttpIdle;
        return this;
    }

    public long getMaxHttpKeepalive() {
        return maxHttpKeepalive;
    }

    @Config("cyoda.presto.keep-alive")
    public CyodaConfig setMaxHttpKeepalive(long maxHttpKeepalive) {
        this.maxHttpKeepalive = maxHttpKeepalive;
        return this;
    }

    public TimeUnit getHttpTimeUnit() {
        return timeUnit;
    }

    @Config("cyoda.presto.time-unit")
    public CyodaConfig setHttpTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        return this;
    }

    public long getHttpConnectionTimeout() {
        return httpConnectionTimeout;
    }

    @Config("cyoda.presto.connection-timeout")
    public CyodaConfig setHttpConnectionTimeout(long connectionTimeout) {
        this.httpConnectionTimeout = connectionTimeout;
        return this;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean getHttpsOverride() {
        return httpsOverride;
    }

    /**
     * For testing without https
     *
     * @return if you want the RestTemplate to allow authentication over http
     */
    @Config("cyoda.presto.https-override")
    public CyodaConfig setHttpsOverride(boolean httpsOverride) {
        this.httpsOverride = httpsOverride;
        return this;
    }

    public boolean getLogApiCallStats() {
        return logApiCallStats;
    }

    @Config("cyoda.presto.log-api-call-stats")
    public void setLogApiCallStats(boolean logApiCallStats) {
        this.logApiCallStats = logApiCallStats;
    }

    public boolean getLogApiCallResponse() {
        return logApiCallResponse;
    }

    @Config("cyoda.presto.log-api-call-response")
    public void setLogApiCallResponse(boolean logApiCallResponse) {
        this.logApiCallResponse = logApiCallResponse;
    }

    public long getApiCallStatsMaxRecords() {
        return apiCallStatsMaxRecords;
    }

    @Config("cyoda.presto.api-call-stats-max-records")
    public void setApiCallStatsMaxRecords(long apiCallStatsMaxRecords) {
        this.apiCallStatsMaxRecords = apiCallStatsMaxRecords;
    }

    public String getSchemaName() {
        return schemaName;
    }

    @Config("cyoda.presto.schema-name")
    public CyodaConfig setSchemaName(String schemaName) {
        this.schemaName = schemaName;
        return this;
    }

    @Config("cyoda.presto.request-page-size")
    public CyodaConfig setRequestPageSize(int requestPageSize) {
        this.requestPageSize = requestPageSize;
        return this;
    }

    public int getRequestPageSize() {
        return requestPageSize;
    }

    public int getRowRequestPageSize() {
        return rowRequestPageSize;
    }

    @Config("cyoda.presto.row-request-page-size")
    public void setRowRequestPageSize(int rowRequestPageSize) {
        this.rowRequestPageSize = rowRequestPageSize;
    }

    @Config("cyoda.presto.user-login-endpoint")
    public CyodaConfig setUserLoginEndpoint(String userLoginEndpoint) {
        this.userLoginEndpoint = userLoginEndpoint;
        return this;
    }

    public String getUserLoginEndpoint() {
        return userLoginEndpoint;
    }

    @Config("cyoda.presto.refresh-token-endpoint")
    public CyodaConfig setRefreshTokenEndpoint(String refreshTokenEndpoint) {
        this.refreshTokenEndpoint = refreshTokenEndpoint;
        return this;
    }

    public String getRefreshTokenEndpoint() {
        return refreshTokenEndpoint;
    }


    @Config("cyoda.presto.allow-anonymous-login")
    public void setAnonymousLogin(boolean anonymousLogin) {
        this.anonymousLogin = anonymousLogin;
    }

    public boolean isAnonymousLogin() {
        return anonymousLogin;
    }

    @Config("cyoda.presto.anonymous-user-id")
    public void setAnonymousUserId(String anonymousUserId) {
        this.anonymousUserId = anonymousUserId;
    }

    public String getAnonymousUserId() {
        return anonymousUserId;
    }

    @Config("cyoda.presto.anonymous-token")
    public void setAnonymousToken(String anonymousToken) {
        this.anonymousToken = anonymousToken;
    }

    public String getAnonymousToken() {
        return anonymousToken;
    }

    @Config("cyoda.presto.anonymous-refresh-token")
    public void setAnonymousRefreshToken(String anonymousRefreshToken) {
        this.anonymousRefreshToken = anonymousRefreshToken;
    }

    public String getAnonymousRefreshToken() {
        return anonymousRefreshToken;
    }

    @Config("cyoda.presto.anonymous-username")
    public void setAnonymousUserName(String anonymousUserName) {
        this.anonymousUserName = anonymousUserName;
    }

    public String getAnonymousUserName() {
        return anonymousUserName;
    }

    public enum CyodaAuthenticationType {
        NONE,
        BASIC,
        JWT
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

    @Config("cyoda.presto.tree-node.pushdown-threshold")
    public void setPredicatePushdownThreshold(int predicatePushdownThreshold) {
        this.predicatePushdownThreshold = predicatePushdownThreshold;
    }
}
