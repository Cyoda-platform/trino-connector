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

package com.cyoda.presto.client;

import com.cyoda.presto.CyodaClient;
import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.auth.AuthPayload;
import com.cyoda.presto.auth.RefreshContext;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.security.AccessDeniedException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.net.HostAndPort;
import okhttp3.ConnectionPool;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.cyoda.presto.CyodaErrorCode.CYODA_AUTHENTICATION_ERROR;
import static com.cyoda.presto.CyodaErrorCode.CYODA_BOOTSTRAPPING_FAILURE;
import static com.cyoda.presto.client.RestTemplateCustomizer.TemplateType.ACCESS;
import static com.cyoda.presto.client.RestTemplateCustomizer.TemplateType.REFRESH;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static java.net.Proxy.Type.HTTP;
import static java.net.Proxy.Type.SOCKS;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("UnstableApiUsage")
public class RestTemplateCustomizer {

    private static final SupplierLogger LOG = SupplierLogger.get(CyodaClient.class);
    public static final Duration TOKEN_EXPIRY_OFFSET = Duration.ofSeconds(10);

    private final CyodaConfig config;
    private final LoadingCache<AuthContext,RestTemplate> restTemplateCache;
    private final LoadingCache<AuthContext,RestTemplate> refreshRestTemplateCache;
    private final RestTemplate unauthorizedRestTemplate;
    private final URI refreshUri;

    private static final List<HttpMessageConverter<?>> HAL_CONVERTERS = Traverson.getDefaultMessageConverters(MediaTypes.HAL_JSON);
    static {
        HAL_CONVERTERS.stream()
                .filter(conv -> conv instanceof AbstractJackson2HttpMessageConverter)
                .forEach(conv -> ((AbstractJackson2HttpMessageConverter)conv).getObjectMapper().configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true));
    }
    @Inject
    public RestTemplateCustomizer(CyodaConfig config) {
        this.config = config;
        restTemplateCache = Caffeine.newBuilder()
                .maximumSize(100)
                .build(key -> {
                    LOG.debug(()->"creating RestTemplate for "+key.getPayload().getUsername());
                    return newRestTemplate(ACCESS,key,HAL_CONVERTERS);
                });

        refreshRestTemplateCache = Caffeine.newBuilder()
                .maximumSize(100)
                .build(key -> {
                    LOG.debug(()->"creating refresh RestTemplate for "+key.getPayload().getUsername());
                    return newRestTemplate(REFRESH,key,null);
                });

        this.unauthorizedRestTemplate = newRestTemplate(ACCESS,null,null);

        try {
            this.refreshUri = config.getServerUrl().toURI().resolve(config.getRefreshTokenEndpoint());
        } catch (URISyntaxException e) {
            throw new PrestoException(CYODA_BOOTSTRAPPING_FAILURE,"Cannot resolve URI",e);
        }

    }

    enum TemplateType {
        ACCESS,
        REFRESH
    }

    public RestTemplate getRestTemplate(AuthContext authContext) {
            return restTemplateCache.get(authContext);
    }

    public RestTemplate getUnauthorizedRestTemplate() {
        return unauthorizedRestTemplate;
    }

    private RestTemplate newRestTemplate(TemplateType templateType, AuthContext authContext,
                                         List<HttpMessageConverter<?>> messageConverters) {

        RestTemplate template = new RestTemplate();
        if ( messageConverters != null ) {
            template.setMessageConverters(messageConverters);
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        ConnectionPool okHttpConnectionPool = new ConnectionPool(config.getMaxHttpIdle(), config.getMaxHttpKeepalive(),
                config.getHttpTimeUnit());
        builder.connectionPool(okHttpConnectionPool);
        builder.connectTimeout(config.getHttpConnectionTimeout(), TimeUnit.SECONDS);
        builder.retryOnConnectionFailure(false);

        setupSocksProxy(builder, config);
        setupHttpProxy(builder, config);
        if ( authContext != null ) {
            setupAuthentication(templateType, authContext, builder, config);
        }

        template.setRequestFactory(new OkHttp3ClientHttpRequestFactory(builder.build()));

        MappingJackson2HttpMessageConverter converter = (MappingJackson2HttpMessageConverter) template.getMessageConverters().stream().filter(it -> it instanceof MappingJackson2HttpMessageConverter).findAny()
                .orElseThrow(() -> new RuntimeException("Cannot find converter"));
        converter.getObjectMapper().enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);

        return template;
    }

    private void setupAuthentication(
            TemplateType templateType,
            AuthContext authContext,
            OkHttpClient.Builder clientBuilder,
            CyodaConfig config) {
        switch (config.getCyodaAuthenticationType()) {
            case BASIC: {
                setupBasicAuth(clientBuilder, config);
                break;
            }
            case JWT: {
                setupTokenAuth(templateType, authContext,clientBuilder, config);
                break;
            }
            default:
                break;
        }

    }

    // We don't actually support basic auth or plan to support basic auth
    private static void setupBasicAuth(OkHttpClient.Builder clientBuilder, CyodaConfig config) {
        final String username = config.getBasicAuthenticationUsername();
        final String password = config.getBasicAuthenticationPassword();
        if (username != null && password != null) {
            if (!config.getHttpsOverride()) {
                checkArgument(config.getServerUrl().getProtocol().equalsIgnoreCase("https"),
                        "Authentication using username/password requires HTTPS to be enabled");
            }
            clientBuilder.addInterceptor(basicAuth(username, password));
        }
    }

    private void setupTokenAuth(
            TemplateType templateType,
            AuthContext authContext,
            OkHttpClient.Builder clientBuilder,
            CyodaConfig config) {

        if (authContext.getPayload().getRefreshToken() != null) {
            if (!config.getHttpsOverride()) {
                checkArgument(config.getServerUrl().getProtocol().equalsIgnoreCase("https"),
                        "Authentication using an access token requires HTTPS to be enabled");
            }
            clientBuilder.addInterceptor(tokenAuth(templateType,authContext));
        }
    }

    public static Interceptor basicAuth(String user, String password) {
        requireNonNull(user, "user is null");
        requireNonNull(password, "password is null");
        if (user.contains(":")) {
            throw new PrestoException(CYODA_AUTHENTICATION_ERROR,"[Cyoda] Illegal character ':' found in username");
        }

        String credential = Credentials.basic(user, password);
        return chain -> chain.proceed(chain.request().newBuilder()
                .header(AUTHORIZATION, credential)
                .build());
    }

    public Interceptor tokenAuth(TemplateType templateType, AuthContext authContext) {
        requireNonNull(authContext, "accessToken is null");

        switch(templateType) {
            case ACCESS: {
                return chain -> chain.proceed(chain.request().newBuilder()
                        .addHeader(AUTHORIZATION, "Bearer " + getAccessToken(authContext))
                        .build());
            }
            case REFRESH: {
                return chain -> chain.proceed(chain.request().newBuilder()
                        .addHeader(AUTHORIZATION, "Bearer " + getRefreshToken(authContext))
                        .build());
            }
            default:
                throw new IllegalStateException("Should not get here");
        }
    }

    private String getRefreshToken(AuthContext authContext) {
        ZonedDateTime refreshTokenExpiry = authContext.getPayload().getRefreshTokenExpiry();
        if (isTokenExpired(authContext.getPayload().getUsername(),refreshTokenExpiry)) {
            String message =
                    Optional.ofNullable(refreshTokenExpiry)
                            .map(it ->
                                    "Refresh token has expired on " +
                                            it.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) +
                                            " for user " +
                                            authContext.getPayload().getUsername() +
                                            ". Need to login again."
                            ).orElseThrow(()->new IllegalStateException("Should not happen"));
            throw new IllegalArgumentException(message);
        } else if ( authContext.getPayload().getRefreshToken() == null ) {
            throw new IllegalArgumentException("No refresh token for user " + authContext.getPayload().getUsername());
        } else {
            return authContext.getPayload().getRefreshToken();
        }
    }

    private String getAccessToken(AuthContext authContext) {
        if (needANewToken(authContext)) {
            RestTemplate restTemplate = Optional.ofNullable(refreshRestTemplateCache.get(authContext))
                    .orElseThrow(()->new IllegalArgumentException("Cannot get RestTemplate"));
            ResponseEntity<RefreshContext> response  = restTemplate.getForEntity(refreshUri, RefreshContext.class);
            if ( response.getStatusCode().is2xxSuccessful() ) {
                RefreshContext refreshContext = Optional.ofNullable(
                        response.getBody()
                ).orElseThrow(()->new IllegalStateException(
                        "No body returned from refresh token endpoint"+config.getRefreshTokenEndpoint())
                );
                AuthContext newAuthContext = authContext.withContext(refreshContext);
                restTemplateCache.refresh(newAuthContext);
                return refreshContext.getToken();
            } else {
                LOG.warn("access denied to "+authContext.getPayload().getUsername()+" with reason: "+response);
                throw new AccessDeniedException("Unauthorized");
            }
        } else {
            return authContext.getPayload().getToken();
        }
    }

    private boolean needANewToken(AuthContext authContext) {
        AuthPayload payload = authContext.getPayload();
        return payload.getToken() == null || isTokenExpired(payload.getUsername(), payload.getTokenExpiry());
    }

    private boolean isTokenExpired(String username, ZonedDateTime tokenExpiry) {
        if ( tokenExpiry == null ) {
            LOG.info("Perpetual token is circulating for "+username);
            return false;  // Perpetual
        }
        return tokenExpiry.isBefore(ZonedDateTime.now().minus(TOKEN_EXPIRY_OFFSET));
    }

    public static void setupSocksProxy(OkHttpClient.Builder clientBuilder, CyodaConfig config) {
        setupProxy(clientBuilder, config.getSocksProxy(), SOCKS);
    }

    public static void setupHttpProxy(OkHttpClient.Builder clientBuilder, CyodaConfig config) {
        setupProxy(clientBuilder, config.getHttpProxy(), HTTP);
    }

    public static void setupProxy(OkHttpClient.Builder clientBuilder, HostAndPort proxy, Proxy.Type type) {
        Optional.ofNullable(proxy).map(RestTemplateCustomizer::toUnresolvedAddress)
                .map(address -> new Proxy(type, address))
                .ifPresent(clientBuilder::proxy);
    }

    private static InetSocketAddress toUnresolvedAddress(HostAndPort address) {
        return InetSocketAddress.createUnresolved(address.getHost(), address.getPort());
    }
}
