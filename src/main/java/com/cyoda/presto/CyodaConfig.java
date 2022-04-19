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

import com.cyoda.presto.CyodaClient.CyodaAuthenticationType;
import com.facebook.airlift.configuration.Config;
import com.facebook.airlift.configuration.ConfigurationFactory;
import com.google.common.net.HostAndPort;

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

    private URL serverUrl;
    private CyodaAuthenticationType cyodaAuthenticationType = CyodaAuthenticationType.NONE;
    private String basicAuthenticationUsername;
    private String basicAuthenticationPassword;
    private HostAndPort socksHostAndPort;
    private HostAndPort httpHostAndPort;
    private String accessToken;
    private long httpConnectionTimeout;
    private TimeUnit timeUnit;
    private int maxHttpIdle;
    private long maxHttpKeepalive;
    private boolean httpsOverride;
    private String schemaName;

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

    public String getAccessToken() {
        return accessToken;
    }

    @Config("cyoda.presto.access-token")
    public CyodaConfig setAccessToken(String accessToken) {
        this.accessToken = accessToken;
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

    public String getSchemaName() {
        return schemaName;
    }

    @Config("cyoda.presto.schema-name")
    public CyodaConfig setSchemaName(String schemaName) {
        this.schemaName = schemaName;
        return this;
    }
}
