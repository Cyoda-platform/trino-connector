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

package com.cyoda.presto.client.logic;

import com.cyoda.presto.client.types.SupportedDataType;

import java.util.Optional;


public class RangePredicateInfo<T> {
    private final SupportedDataType<T> lowerBound;
    private final SupportedDataType<T> upperBound;

    private RangePredicateInfo(SupportedDataType<T> lowerBound, SupportedDataType<T> upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public Optional<SupportedDataType<T>> getLowerBound() {
        return Optional.of(lowerBound);
    }

    public Optional<SupportedDataType<T>> getUpperBound() {
        return Optional.of(upperBound);
    }

    public static final class Builder<T> {
        private SupportedDataType<T> lowerBound = null;
        private SupportedDataType<T> upperBound = null;

        private Builder() {
        }

        public void setLowerBound(SupportedDataType<T> lowerBound) {
            this.lowerBound = lowerBound;
        }

        public void setUpperBound(SupportedDataType<T> upperBound) {
            this.upperBound = upperBound;
        }

        public RangePredicateInfo<T> build() {
            return new RangePredicateInfo<>(lowerBound, upperBound);
        }
    }
}
