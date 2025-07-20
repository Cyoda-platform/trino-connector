/*
 * Copyright (C) 2025 Cyoda Ltd.
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

public class OAuth2TokenResponse {

    final String token;
    final String tokenType;
    final Long expiresIn;
    final String scope;

    @JsonCreator
    public OAuth2TokenResponse(
    @JsonProperty("access_token") String token,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") Long expiresIn,
    @JsonProperty("scope") String scope
    ) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.scope = scope;
    }



}
