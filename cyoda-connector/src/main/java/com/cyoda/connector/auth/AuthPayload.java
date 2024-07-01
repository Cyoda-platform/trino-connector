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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.List;

public class AuthPayload {
    private final ZonedDateTime refreshTokenExpiry;
    private final Long idleTimeMs;
    private final List<String> roles;
    private final ZonedDateTime tokenExpiry;
    private final String token;
    private final String refreshToken;
    private final String username;

    @JsonCreator
    public AuthPayload(
            @JsonProperty("refreshTokenExpiry") ZonedDateTime refreshTokenExpiry,
            @JsonProperty("idleTimeMs") Long idleTimeMs,
            @JsonProperty("roles") List<String> roles,
            @JsonProperty("tokenExpiry") ZonedDateTime tokenExpiry,
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
    public ZonedDateTime getRefreshTokenExpiry() {
        return refreshTokenExpiry;
    }

    @JsonProperty
    public Long getIdleTimeMs() {
        return idleTimeMs;
    }

    @JsonProperty
    public List<String> getRoles() {
        return roles;
    }

    @JsonProperty
    public ZonedDateTime getTokenExpiry() {
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
