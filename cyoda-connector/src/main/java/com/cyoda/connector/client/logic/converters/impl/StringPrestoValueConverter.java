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

package com.cyoda.connector.client.logic.converters.impl;

import com.cyoda.connector.client.logic.converters.structure.StringTypeValueConverter;
import com.cyoda.connector.client.types.DataType;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;

import java.math.BigDecimal;

public class StringPrestoValueConverter extends StringTypeValueConverter<String> {

    @Inject
    public StringPrestoValueConverter() {
        super(DataType.STRING);
    }

    @Override
    protected String fromStr(String value) {
        return value;
    }

    @Override
    protected String toStr(String value) {
        return value;
    }


    @Override
    public String fromOtherCyodaType(Object value, String columnName) {
        return switch (value) {
            // getting rid of exponent to ensure consistent serialization
            case BigDecimal bd -> bd.stripTrailingZeros().toPlainString();
            case Double d -> BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
            //a reasonable shortcut to make this type a failsafe for objects
            default -> value.toString();
        };
    }
}
