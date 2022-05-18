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

package com.cyoda.presto.client.logic.converters.impl;

import com.facebook.presto.common.type.VarcharType;
import io.airlift.slice.Slice;

@SuppressWarnings({"ALL","java:S3740"})
public class ClassPrestoValueConverter extends SliceValueConverter<Class> {
    @Override
    Class<Class> getClazz() {
        return Class.class;
    }

    @Override
    public Class toObject(Object nativeValue) {
        return fromSlice(VarcharType.VARCHAR,(Slice) nativeValue);
    }
}
