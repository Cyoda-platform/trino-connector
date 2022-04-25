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

import com.cyoda.presto.CyodaConfig;
import com.facebook.presto.spi.PrestoException;
import com.google.common.base.CharMatcher;
import com.google.common.net.HostAndPort;
import okhttp3.ConnectionPool;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.MediaType;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.cyoda.presto.CyodaErrorCode.CYODA_AUTHENTICATION_ERROR;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static java.net.Proxy.Type.HTTP;
import static java.net.Proxy.Type.SOCKS;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("UnstableApiUsage")
public class RestTemplateCustomizer {
    private RestTemplateCustomizer() {
    }

    public static RestTemplate newRestTemplate(CyodaConfig config, MediaType... mediaTypes) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(Traverson.getDefaultMessageConverters(mediaTypes));

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        ConnectionPool okHttpConnectionPool = new ConnectionPool(config.getMaxHttpIdle(), config.getMaxHttpKeepalive(),
                config.getHttpTimeUnit());
        builder.connectionPool(okHttpConnectionPool);
        builder.connectTimeout(config.getHttpConnectionTimeout(), TimeUnit.SECONDS);
        builder.retryOnConnectionFailure(false);

        setupSocksProxy(builder, config);
        setupHttpProxy(builder, config);
        setupAuthentication(builder, config);

        restTemplate.setRequestFactory(new OkHttp3ClientHttpRequestFactory(builder.build()));

        return restTemplate;
    }

    private static void setupAuthentication(
            OkHttpClient.Builder clientBuilder,
            CyodaConfig config) {
        switch (config.getCyodaAuthenticationType()) {
            case BASIC: {
                setupBasicAuth(clientBuilder, config);
                break;
            }
            case JWT: {
                setupTokenAuth(clientBuilder, config);
                break;
            }
            default:
                break;
        }

    }

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

    private static void setupTokenAuth(
            OkHttpClient.Builder clientBuilder,
            CyodaConfig config) {

        final String accessToken = config.getAccessToken();
        if (accessToken != null) {
            if (!config.getHttpsOverride()) {
                checkArgument(config.getServerUrl().getProtocol().equalsIgnoreCase("https"),
                        "Authentication using an access token requires HTTPS to be enabled");
            }
            clientBuilder.addInterceptor(tokenAuth(accessToken));
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

    public static Interceptor tokenAuth(String accessToken) {
        requireNonNull(accessToken, "accessToken is null");
        checkArgument(CharMatcher.inRange((char) 33, (char) 126).matchesAllOf(accessToken));

        return chain -> chain.proceed(chain.request().newBuilder()
                .addHeader(AUTHORIZATION, "Bearer " + accessToken)
                .build());
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
