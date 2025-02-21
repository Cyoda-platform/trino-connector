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

package com.cyoda.connector.auth;

import com.cyoda.connector.logging.SupplierLogger;
import io.trino.spi.security.AccessDeniedException;
import io.trino.spi.security.BasicPrincipal;
import io.trino.spi.security.PasswordAuthenticator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class CyodaAuthenticator extends RestAuthenticator implements PasswordAuthenticator {

    private static final SupplierLogger LOG = SupplierLogger.get(CyodaAuthenticator.class);

    private static final Pattern UUID_PATTERN = Pattern.compile(
            UUID_PATTERN_STRING
    );

    private final URI loginUri;
    private final String testTokenUrl;

    public CyodaAuthenticator(Map<String, String> config) {
        super(config);
        this.loginUri = URI.create(
                    requireNonNull(config.get("cyoda.login.uri"), "Property cyoda.login.uri is required in password-authenticator.properties"));
        this.testTokenUrl = config.get("cyoda.test-token.uri");
    }

    @Override
    public Principal createAuthenticatedPrincipal(String user, String password) {
        if (testTokenUrl!= null && (user == null || user.isEmpty() || UUID_PATTERN.matcher(user).matches())) { // hijack user/password to deliver token
            return parseProvidedToken(authRestTemplate, testTokenUrl, password);
        } else {
            Login payload = new Login(user, password);
            HttpEntity<Login> requestEntity = new HttpEntity<>(payload, HEADERS);
            ResponseEntity<AuthContextWithToken> response =
                    authRestTemplate.getRestTemplate()
                            .exchange(loginUri, HttpMethod.POST, requestEntity, AuthContextWithToken.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return new BasicPrincipal(response.getBody().getUserId());
            } else {
                LOG.warn("access denied to " + user + " with reason: " + response.toString());
                throw new AccessDeniedException("Unauthorized");
            }
        }
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
