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

import com.fasterxml.jackson.databind.DeserializationFeature;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;


@SuppressWarnings("UnstableApiUsage")
public class AuthRestTemplate {

    private static final String DEFAULT_HTTP_MAX_IDLE = "20";
    private static final String DEFAULT_HTTP_KEEP_ALIVE = "20";
    private static final String DEFAULT_HTTP_CONNECTION_TIMEOUT = "30";
    private final int httpMaxIdle;
    private final long httpKeepAlive;
    private final long httpConnectionTimeout;
    private final RestTemplate restTemplate;

    public AuthRestTemplate(Map<String, String> config) {
        httpMaxIdle = Integer.parseInt(config.getOrDefault("http.max-idle", DEFAULT_HTTP_MAX_IDLE));
        httpKeepAlive = Long.parseLong(config.getOrDefault("http.keep-alive", DEFAULT_HTTP_KEEP_ALIVE));
        httpConnectionTimeout = Long.parseLong(config.getOrDefault("http.connection-timeout", DEFAULT_HTTP_CONNECTION_TIMEOUT));
        this.restTemplate = newRestTemplate();
    }


    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    private RestTemplate newRestTemplate() {

        RestTemplate template = new RestTemplate();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        ConnectionPool okHttpConnectionPool = new ConnectionPool(httpMaxIdle, httpKeepAlive,
                TimeUnit.SECONDS);
        builder.connectionPool(okHttpConnectionPool);
        builder.connectTimeout(httpConnectionTimeout, TimeUnit.SECONDS);
        builder.retryOnConnectionFailure(false);


        template.setRequestFactory(new OkHttp3ClientHttpRequestFactory(builder.build()));


        MappingJackson2HttpMessageConverter converter = (MappingJackson2HttpMessageConverter) template.getMessageConverters().stream().filter(it -> it instanceof MappingJackson2HttpMessageConverter).findAny()
                .orElseThrow(() -> new RuntimeException("Cannot find converter"));
        converter.getObjectMapper().enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);

        return template;
    }


}
