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

import com.cyoda.presto.client.logic.ColumnPredicate;
import com.cyoda.presto.client.logic.converters.PrestoValueConverter;
import com.cyoda.presto.client.types.CompoundDataType;
import com.cyoda.presto.client.types.DataType;
import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.predicate.DiscreteValues;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.ColumnMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

public class CyodaColumnHandle implements ColumnHandle {
    private final String connectorId;
    private final String columnName;
    private final Type columnType;
    private final int ordinalPosition;
    private final String requestHandlerKey;
    private final CompoundDataType dataType;
    private final boolean isNullable;

    private transient PrestoValueConverter<?> converter = null;

    @JsonCreator
    public CyodaColumnHandle(
            @JsonProperty("connectorId") String connectorId,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("columnType") Type columnType,
            @JsonProperty("dataType") CompoundDataType dataType,
            @JsonProperty("ordinalPosition") int ordinalPosition,
            @JsonProperty("requestHandlerKey") String requestHandlerKey,
            @JsonProperty("isNullable") boolean isNullable
    ) {
        this.connectorId = requireNonNull(connectorId, "connectorId is null");
        this.columnName = requireNonNull(columnName, "columnName is null");
        this.columnType = requireNonNull(columnType, "columnType is null");
        this.dataType = dataType;
        this.ordinalPosition = ordinalPosition;
        this.requestHandlerKey = requireNonNull(requestHandlerKey, "requestHandlerKey is null");
        this.isNullable = isNullable;
    }

    @Deprecated
    public CyodaColumnHandle(
            String connectorId,
            String columnName,
            Type columnType,
            DataType dataType,
            int ordinalPosition,
            String requestHandlerKey
    ) {
        this(connectorId, columnName, columnType, new CompoundDataType(columnName,dataType), ordinalPosition, requestHandlerKey, true);
    }

    public PrestoValueConverter<?> getConverter(){
        if (converter == null)
            converter = dataType.getConverter();
        return converter;
    }

    public void writeValue(BlockBuilder blockBuilder, Object cyodaNative){
        getConverter().writeCyodaNative(columnType, blockBuilder, cyodaNative, columnName);
    }

    public ColumnPredicate<?> newComparisonPredicateFromNative(ColumnPredicate.ComparisonOp op, Object nativeValue){
        return getConverter().newComparisonPredicateFromNative(this, op, nativeValue);
    }

    public <T extends Comparable<T>> ColumnPredicate<T> newEqualsPredicateFromJava(T javaValue){
        return getConverter().newComparisonPredicateFromJava(this, ColumnPredicate.ComparisonOp.EQUAL, javaValue);
    }

    public ColumnPredicate<?> newInListPredicateFromDiscrete(DiscreteValues discreteValues){
        return getConverter().newInListPredicate(this, discreteValues);
    }

    @JsonProperty
    public String getConnectorId() {
        return connectorId;
    }

    @JsonProperty
    public String getColumnName() {
        return columnName;
    }

    @JsonProperty
    public Type getColumnType() {
        return columnType;
    }

    @JsonProperty
    public CompoundDataType getDataType(){
        return dataType;
    }

    @JsonProperty
    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    @JsonProperty
    public String getRequestHandlerKey() {
        return requestHandlerKey;
    }

    @JsonProperty
    public boolean getIsNullable() {
        return isNullable;
    }

    @JsonIgnore
    public ColumnMetadata getColumnMetadata() {
        return new ColumnMetadata(columnName, columnType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CyodaColumnHandle that = (CyodaColumnHandle) o;
        return ordinalPosition == that.ordinalPosition && connectorId.equals(that.connectorId) && columnName.equals(that.columnName) && columnType.equals(that.columnType) && requestHandlerKey.equals(that.requestHandlerKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectorId, columnName, columnType, ordinalPosition, requestHandlerKey);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("connectorId", connectorId)
                .add("columnName", columnName)
                .add("columnType", columnType)
                .add("ordinalPosition", ordinalPosition)
                .toString();
    }
}
