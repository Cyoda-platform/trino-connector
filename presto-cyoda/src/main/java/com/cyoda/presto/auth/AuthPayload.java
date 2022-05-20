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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AuthPayload {
    private final String refreshTokenExpiry;
    private final String idleTimeMs;
    private final List<String> roles;
    private final String tokenExpiry;
    private final String token;
    private final String refreshToken;
    private final String username;

    AuthPayload(
            @JsonProperty("refreshTokenExpiry") String refreshTokenExpiry,
            @JsonProperty("idleTimeMs") String idleTimeMs,
            @JsonProperty("roles") List<String> roles,
            @JsonProperty("tokenExpiry") String tokenExpiry,
            @JsonProperty("token") String token,
            @JsonProperty("refreshToken") String refreshToken,
            @JsonProperty("username") String username
    ) {
        this.refreshTokenExpiry = refreshTokenExpiry;
        this.idleTimeMs = idleTimeMs;
        this.roles = roles;
        this.tokenExpiry = tokenExpiry;
        this.token = token;
        this.refreshToken = refreshToken;
        this.username = username;
    }

    @JsonProperty
    public String getRefreshTokenExpiry() {
        return refreshTokenExpiry;
    }

    @JsonProperty
    public String getIdleTimeMs() {
        return idleTimeMs;
    }

    @JsonProperty
    public List<String> getRoles() {
        return roles;
    }

    @JsonProperty
    public String getTokenExpiry() {
        return tokenExpiry;
    }

    @JsonProperty
    public String getToken() {
        return token;
    }

    @JsonProperty
    public String getRefreshToken() {
        return refreshToken;
    }

    @JsonProperty
    public String getUsername() {
        return username;
    }

}
