package com.cyoda.presto;

import com.cyoda.presto.CyodaClient.CyodaAuthenticationType;
import com.facebook.airlift.configuration.Config;
import com.facebook.airlift.configuration.ConfigurationFactory;
import com.google.common.net.HostAndPort;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class CyodaConfig {
    private URL serverUrl;
    private CyodaAuthenticationType cyodaAuthenticationType = CyodaAuthenticationType.NONE;
    private String basicAuthenticationUsername;
    private String basicAuthenticationPassword;
    private HostAndPort socksHostAndPort;
    private HostAndPort httpHostAndPort;
    private String accessToken;

    @NotNull
    public URL getServerUrl()
    {
        return serverUrl;
    }

    /**
     * The URL of the Cyoda API to be connected to.
     *
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
    public CyodaConfig withServerUrl(URL serverUrl)
    {
        this.serverUrl = serverUrl;
        return this;
    }

    @NotNull
    public CyodaAuthenticationType getCyodaAuthenticationType()
    {
        return cyodaAuthenticationType;
    }

    @Config("cyoda.presto.authentication.type")
    public CyodaConfig withcCyodaAuthenticationType(CyodaAuthenticationType cyodaAuthenticationType)
    {
        if (cyodaAuthenticationType != null) {
            this.cyodaAuthenticationType = cyodaAuthenticationType;
        }
        return this;
    }

    public Optional<String> getBasicAuthenticationUsername()
    {
        return Optional.ofNullable(basicAuthenticationUsername);
    }

    @Config("cyoda.presto.basic-authentication.username")
    public CyodaConfig withBasicAuthenticationUsername(String basicAuthenticationUsername)
    {
        this.basicAuthenticationUsername = basicAuthenticationUsername;
        return this;
    }

    public Optional<String> getBasicAuthenticationPassword()
    {
        return Optional.ofNullable(basicAuthenticationPassword);
    }

    @Config("cyoda.presto.basic-authentication.password")
    public CyodaConfig withBasicAuthenticationPassword(String basicAuthenticationPassword)
    {
        this.basicAuthenticationPassword = basicAuthenticationPassword;
        return this;
    }


    @Config("cyoda.presto.socks-proxy")
    public CyodaConfig withSocksProxy(HostAndPort socksHostAndPort)
    {
        this.socksHostAndPort = socksHostAndPort;
        return this;
    }
    public Optional<HostAndPort> getSocksProxy() {
        return Optional.ofNullable(socksHostAndPort);
    }

    @Config("cyoda.presto.http-proxy")
    public CyodaConfig withHttpProxy(HostAndPort httpHostAndPort)
    {
        this.httpHostAndPort = httpHostAndPort;
        return this;
    }
    public Optional<HostAndPort> getHttpProxy() {
        return Optional.ofNullable(httpHostAndPort);
    }

    @Config("cyoda.presto.access-token")
    public CyodaConfig withHttpProxy(String accessToken)
    {
        this.accessToken = accessToken;
        return this;
    }
    public Optional<String> getAccessToken() {
        return Optional.ofNullable(accessToken);
    }
}
