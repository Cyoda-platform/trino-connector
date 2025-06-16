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

import io.trino.spi.security.PasswordAuthenticator;

import java.net.URI;
import java.security.Principal;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class CyodaAuthenticator extends RestAuthenticator implements PasswordAuthenticator {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            CyodaAuthorizationManager.UUID_PATTERN_STRING
    );

    private final URI loginUri;
    private final String testTokenUrl;
    private final CyodaAuthorizationManager authHandler;

    public CyodaAuthenticator(Map<String, String> config, CyodaAuthorizationManager authHandler) {
        super(config);
        this.loginUri = URI.create(
                    requireNonNull(config.get("cyoda.login.uri"), "Property cyoda.login.uri is required in password-authenticator.properties"));
        this.testTokenUrl = requireNonNull(config.get("cyoda.test-token.uri"), "Property cyoda.test-token.uri is required in password-authenticator.properties");
        this.authHandler = authHandler;
    }

    @Override
    public Principal createAuthenticatedPrincipal(String user, String password) {
        if (user == null || user.isEmpty() || UUID_PATTERN.matcher(user).matches()) { // hijack user/password to deliver token
            return authHandler.authToken(authRestTemplate, testTokenUrl, password);
        } else {
            return authHandler.authPassword(authRestTemplate, loginUri, testTokenUrl, user, password);
        }
    }


}
