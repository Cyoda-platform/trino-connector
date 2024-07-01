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

public class RefreshContext {
    private final ZonedDateTime tokenExpiry;
    private final String token;

    @JsonCreator
    public RefreshContext(
            @JsonProperty("tokenExpiry") ZonedDateTime tokenExpiry,
            @JsonProperty("token") String token
    ) {
        this.tokenExpiry = tokenExpiry;
        this.token = token;
    }

    @JsonProperty
    public ZonedDateTime getTokenExpiry() {
        return tokenExpiry;
    }

    @JsonProperty
    public String getToken() {
        return token;
    }
}
