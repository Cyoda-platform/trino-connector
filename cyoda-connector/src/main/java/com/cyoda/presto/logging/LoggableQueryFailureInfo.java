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

package com.cyoda.presto.logging;

import io.trino.spi.ErrorCode;
import io.trino.spi.eventlistener.QueryFailureInfo;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public class LoggableQueryFailureInfo {
    private final ErrorCode errorCode;
    private final String failureType;
    private final String failureMessage;
    private final String failureTask;
    private final String failureHost;
    private final String failuresJson;

    public static LoggableQueryFailureInfo from(QueryFailureInfo info) {
        return new LoggableQueryFailureInfo(
                info.getErrorCode(),
                info.getFailureType().orElse(null),
                info.getFailureMessage().orElse(null),
                info.getFailureTask().orElse(null),
                info.getFailureHost().orElse(null),
                info.getFailuresJson()
        );
    }
    public LoggableQueryFailureInfo(
              ErrorCode errorCode,
             String failureType,
             String failureMessage,
             String failureTask,
             String failureHost,
             String failuresJson
    ) {
        this.errorCode = errorCode;
        this.failureType = failureType;
        this.failureMessage = failureMessage;
        this.failureTask = failureTask;
        this.failureHost = failureHost;
        this.failuresJson = failuresJson;
    }

    @JsonProperty
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    @JsonProperty
    public String getFailureType() {
        return failureType;
    }

    @JsonProperty
    public String getFailureMessage() {
        return failureMessage;
    }

    @JsonProperty
    public String getFailureTask() {
        return failureTask;
    }

    @JsonProperty
    public String getFailureHost() {
        return failureHost;
    }

    @JsonProperty
    public String getFailuresJson() {
        return failuresJson;
    }
}
