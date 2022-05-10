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

package com.cyoda.presto.handles;

import com.facebook.presto.common.predicate.TupleDomain;
import com.facebook.presto.spi.ConnectorTableLayoutHandle;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class CyodaTableLayoutHandle implements ConnectorTableLayoutHandle {

    private final CyodaTableHandle table;
    private final TupleDomain<CyodaColumnHandle> constraint;

    @JsonCreator
    public CyodaTableLayoutHandle(
            @JsonProperty("table") CyodaTableHandle table,
            @JsonProperty("constraint") TupleDomain<CyodaColumnHandle> constraint
    ) {
        this.table = table;
        this.constraint = constraint;
    }


    @JsonProperty
    public CyodaTableHandle getTable() {
        return table;
    }

    @JsonProperty
    public TupleDomain<CyodaColumnHandle> getConstraint() {
        return constraint;
    }

    @Override
    public String toString() {
        return "CyodaTableLayoutHandle{" +
                "table=" + table +
                ", constraint=" + constraint +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CyodaTableLayoutHandle that = (CyodaTableLayoutHandle) o;
        return table.equals(that.table) && constraint.equals(that.constraint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, constraint);
    }

}
