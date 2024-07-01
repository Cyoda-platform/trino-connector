///*
// * Copyright (C) 2022 Cyoda Ltd.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// */
//
//package com.cyoda.connector.handles;
//
//import io.trino.spi.predicate.TupleDomain;
//import io.trino.spi.ConnectorTableLayoutHandle;
//import com.fasterxml.jackson.annotation.JsonCreator;
//import com.fasterxml.jackson.annotation.JsonProperty;
//
//import java.util.Objects;
//
////TODO Interface is gone. Need to find out if this logic is necessary
//public class CyodaTableLayoutHandle implements ConnectorTableLayoutHandle {
//
//    private final CyodaTableMeta table;
//    private final TupleDomain<CyodaColumnHandle> constraint;
//
//    @JsonCreator
//    public CyodaTableLayoutHandle(
//            @JsonProperty("table") CyodaTableMeta table,
//            @JsonProperty("constraint") TupleDomain<CyodaColumnHandle> constraint
//    ) {
//        this.table = table;
//        this.constraint = constraint;
//    }
//
//
//    @JsonProperty
//    public CyodaTableMeta getTable() {
//        return table;
//    }
//
//    @JsonProperty
//    public TupleDomain<CyodaColumnHandle> getConstraint() {
//        return constraint;
//    }
//
//    @Override
//    public String toString() {
//        return "CyodaTableLayoutHandle{" +
//                "table=" + table +
//                ", constraint=" + constraint +
//                '}';
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        CyodaTableLayoutHandle that = (CyodaTableLayoutHandle) o;
//        return table.equals(that.table) && constraint.equals(that.constraint);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(table, constraint);
//    }
//
//}
