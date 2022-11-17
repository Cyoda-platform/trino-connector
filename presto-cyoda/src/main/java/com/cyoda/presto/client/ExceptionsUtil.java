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

package com.cyoda.presto.client;

import io.trino.spi.TrinoException;
import io.trino.spi.StandardErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URI;

import static com.cyoda.presto.CyodaErrorCode.CYODA_API_ERROR;
import static com.cyoda.presto.CyodaErrorCode.CYODA_TOO_MANY_REQUESTS;
import static com.google.common.base.MoreObjects.toStringHelper;
import static java.lang.String.format;

public class ExceptionsUtil {
    private ExceptionsUtil() {}

    @SuppressWarnings("SameParameterValue")
    public static RuntimeException requestFailedException(Object me, String task, HttpClientErrorException e, URI uri) {
        if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
            return new TrinoException(StandardErrorCode.PERMISSION_DENIED, "Authentication failed : " + e.getStatusText());
        }
        if (HttpStatus.TOO_MANY_REQUESTS.equals(e.getStatusCode())) {
            return new TrinoException(CYODA_TOO_MANY_REQUESTS, "Request throttled : " + e.getStatusText());
        }

        return new TrinoException(CYODA_API_ERROR,
                format("[Cyoda] Error %s at %s returned an invalid response: %s [Error: %s]",
                        task, uri.toASCIIString(), asString(me,e), e.getResponseBodyAsString()),
                e
        );
    }

    private static String asString(Object me, HttpClientErrorException e) {
        return toStringHelper(me)
                .add("statusCode", e.getStatusCode())
                .add("statusMessage", e.getStatusText())
                .add("headers", e.getResponseHeaders())
                .omitNullValues()
                .toString();
    }
}
