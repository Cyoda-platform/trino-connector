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

package com.cyoda.connector;

import io.trino.spi.ErrorCode;
import io.trino.spi.ErrorCodeSupplier;
import io.trino.spi.ErrorType;

import static io.trino.spi.ErrorType.EXTERNAL;

public enum CyodaErrorCode
        implements ErrorCodeSupplier
{
    CYODA_BOOTSTRAPPING_FAILURE(0,EXTERNAL),
    CYODA_METADATA_ERROR(1, EXTERNAL),
    CYODA_UNSUPPORTED_TYPE_ERROR(2, EXTERNAL),
    CYODA_PUSHDOWN_UNSUPPORTED_EXPRESSION(3, EXTERNAL),
    CYODA_QUERY_GENERATOR_FAILURE(4, EXTERNAL),
    CYODA_RESULT_ERROR(5, EXTERNAL),
    CYODA_AMBIGUOUS_OBJECT_NAME(6, EXTERNAL),
    CYODA_INCORRECT_TYPE_ERROR(7, EXTERNAL),
    CYODA_PAGING_ERROR(8,EXTERNAL),
    CYODA_TOO_MANY_REQUESTS(9,EXTERNAL),
    CYODA_API_ERROR(10,EXTERNAL),
    CYODA_AUTHENTICATION_ERROR(11,EXTERNAL),
    CYODA_UNSUPPORTED_CONDITION(12, EXTERNAL);


    private final ErrorCode errorCode;

    CyodaErrorCode(int code, ErrorType type)
    {
        errorCode = new ErrorCode(code + 0x1104_0000, name(), type);
    }

    @Override
    public ErrorCode toErrorCode()
    {
        return errorCode;
    }
}

