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

package com.cyoda.presto.auth;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.logging.SupplierLogger;
import io.trino.spi.TrinoException;
import io.trino.spi.security.AccessDeniedException;
import io.trino.spi.security.PasswordAuthenticator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Collections;

import static com.cyoda.presto.CyodaErrorCode.CYODA_BOOTSTRAPPING_FAILURE;

public class CyodaAuthenticator implements PasswordAuthenticator {

    private static final SupplierLogger LOG = SupplierLogger.get(CyodaAuthenticator.class);
    private static final HttpHeaders HEADERS = standardHeader();

    private final RestTemplateCustomizer restTemplateCustomizer;
    private final URI loginUri;

    @Inject
    public CyodaAuthenticator(CyodaConfig config, RestTemplateCustomizer restTemplateCustomizer) {
        this.restTemplateCustomizer = restTemplateCustomizer;
        try {
            this.loginUri = config.getServerUrl().toURI().resolve(config.getUserLoginEndpoint());
        } catch (URISyntaxException e) {
            throw new TrinoException(CYODA_BOOTSTRAPPING_FAILURE,"Cannot resolve URI",e);
        }

    }

    @Override
    public Principal createAuthenticatedPrincipal(String user, String password) {

        Login payload = new Login(user,password);
        HttpEntity<Login> requestEntity = new HttpEntity<>(payload, HEADERS);
        ResponseEntity<AuthContext> response =
                restTemplateCustomizer.getUnauthorizedRestTemplate()
                        .exchange(loginUri, HttpMethod.POST, requestEntity, AuthContext.class);
        if ( response.getStatusCode().is2xxSuccessful() ) {
            return new JWTPrinciple(response.getBody());
        } else {
            LOG.warn("access denied to "+user+" with reason: "+response.toString());
            throw new AccessDeniedException("Unauthorized");
        }
    }

    private static HttpHeaders standardHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    static class Login {
        private final String username;
        private final String password;

        @JsonCreator
        Login(@JsonProperty("username") String username, @JsonProperty("password") String password) {
            this.username = username;
            this.password = password;
        }

        @JsonProperty
        public String getUsername() {
            return username;
        }

        @JsonProperty
        public String getPassword() {
            return password;
        }
    }


}
