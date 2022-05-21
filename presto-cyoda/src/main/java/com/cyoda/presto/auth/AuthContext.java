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
import com.facebook.presto.spi.ConnectorSession;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.security.Principal;

public class AuthContext {
    // This is the key, and determines equality.
    private final String userId;
    private final AuthPayload payload;

    @JsonCreator
    public AuthContext(
            @JsonProperty("userId") String userId,
            @JsonProperty("payload") AuthPayload payload
    ) {
        this.userId = userId;
        this.payload = payload;
    }

    @JsonProperty
    public String getUserId() {
        return userId;
    }

    @JsonProperty
    public AuthPayload getPayload() {
        return payload;
    }

    public static @Nonnull
    AuthContext fromSession(@Nonnull ConnectorSession session, @Nonnull CyodaConfig config) {
        if ( !config.isAnonymousLogin() ) {
            Principal principal = session.getIdentity().getPrincipal()
                    .orElseThrow(() -> new IllegalArgumentException("principle is missing"));
            Preconditions.checkArgument(principal instanceof JWTPrinciple, "principle is not an instance of %s but %s", JWTPrinciple.class.getName(), principal.getClass().getName());
            return ((JWTPrinciple) principal).getAuthPayload();
        } else {
            return new AuthContext(
                    config.getAnonymousUserId(),
                    new AuthPayload(
                        null,
                        null,
                        null,
                        null,
                        config.getAnonymousToken(),
                        config.getAnonymousRefreshToken(),
                        config.getAnonymousUserName()
                    )
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthContext that = (AuthContext) o;
        return Objects.equal(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }

    @JsonIgnore
    public AuthContext withContext(RefreshContext refreshContext) {
        return new AuthContext(
                this.userId,
                new AuthPayload(
                        this.payload.getRefreshTokenExpiry(),
                        this.payload.getIdleTimeMs(),
                        this.payload.getRoles(),
                        refreshContext.getTokenExpiry(),
                        refreshContext.getToken(),
                        this.payload.getRefreshToken(),
                        this.payload.getUsername()
                )
        );
    }
}