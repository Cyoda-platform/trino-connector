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

import com.cyoda.connector.client.logic.converters.structure.SliceJsonValueConverter;
import com.cyoda.connector.client.types.DataType;

import java.util.Locale;

public class LocalePrestoValueConverter extends SliceJsonValueConverter<Locale> {


    public LocalePrestoValueConverter() {
        super(DataType.LOCALE);
    }

    @Override
    public Locale fromOtherCyodaType(Object value, String columnName) {
        String[] spl = ((String)value).split(",");
        switch (spl.length){
            case 1: return new Locale((String)value);
            case 2: return new Locale(spl[0].trim(), spl[1].trim());
            default:return new Locale(spl[0].trim(), spl[1].trim(), spl[2].trim());
        }
    }
}
