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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthContextWithToken extends AuthContext {
    private final AuthPayload payload;

    @JsonCreator
    public AuthContextWithToken(
            @JsonProperty("userId") String userId,
            @JsonProperty("payload") AuthPayload payload
    ) {
        super(userId);
        this.payload = payload;
    }

    @JsonProperty
    public AuthPayload getPayload() {
        return payload;
    }

    @JsonIgnore
    public AuthContextWithToken withContext(RefreshContext refreshContext) {
        return new AuthContextWithToken(
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