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

package com.cyoda.presto.client.logic.converters;

import com.facebook.presto.common.type.Type;
import io.airlift.slice.Slice;

import javax.annotation.Nonnull;

public interface PrestoValueConverter<T> {
    /****** conversion to/from long *******/
    default long toLong(@Nonnull T value) {
        throw new UnsupportedOperationException("not implemented or supported for "+value.getClass().getName());
    }

    default @Nonnull T fromLong(long value) {
        throw new UnsupportedOperationException("not implemented or supported");
    }

    default long minValueOfIntType() {
        throw new UnsupportedOperationException("not implemented or supported");
    }

    default long maxValueOfIntType() {
        throw new UnsupportedOperationException("not implemented or supported");
    }

    /****** conversion to/from Slice *******/
    default Slice toSlice(@Nonnull Type type, @Nonnull T value) {
        throw new UnsupportedOperationException("not implemented or supported");
    }

    default @Nonnull T fromSlice(@Nonnull Type type, Slice value) {
        throw new UnsupportedOperationException("not implemented or supported");
    }

    T toObject(Object nativeValue);
}
