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

package com.cyoda.connector.handles;

import com.cyoda.connector.client.logic.ColumnPredicate;
import com.cyoda.connector.client.logic.converters.PrestoValueConverter;
import com.cyoda.connector.client.reporting.data.RowValueGrabber;
import com.cyoda.connector.client.types.CompoundDataType;
import com.cyoda.connector.client.types.DataType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.predicate.DiscreteValues;
import io.trino.spi.type.Type;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

public class CyodaColumnHandle implements ColumnHandle {
    private final String columnName;
    private final String columnKey;
    private final ColumnCategory columnCategory;
    private final String externalName;
    private final Type columnType;
    private final int ordinalPosition;
    private final CompoundDataType dataType;
    private final boolean isNullable;

    private transient PrestoValueConverter<?> converter = null;

    private transient volatile RowValueGrabber grabber = null;

    @JsonCreator
    public CyodaColumnHandle(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("columnKey") String columnKey,
            @JsonProperty("columnCategory") ColumnCategory columnCategory,
            @JsonProperty("externalName") String externalName,
            @JsonProperty("columnType") Type columnType,
            @JsonProperty("dataType") CompoundDataType dataType,
            @JsonProperty("ordinalPosition") int ordinalPosition,
            @JsonProperty("isNullable") boolean isNullable
    ) {
        this.columnName = requireNonNull(columnName, "columnName is null");
        this.columnKey = columnKey;
        this.columnCategory = columnCategory;
        this.externalName = externalName;
        this.columnType = requireNonNull(columnType, "columnType is null");
        this.dataType = dataType;
        this.ordinalPosition = ordinalPosition;
        this.isNullable = isNullable;
    }

    public CyodaColumnHandle(
            String columnName,
            Type columnType,
            CompoundDataType dataType,
            int ordinalPosition,
            boolean isNullable){
        this(columnName, columnName, null, columnName, columnType, dataType, ordinalPosition, isNullable);
    }

    @Deprecated
    public CyodaColumnHandle(
            String columnName,
            Type columnType,
            DataType dataType,
            int ordinalPosition
    ) {
        this(columnName, null, null, null, columnType, new CompoundDataType(columnName,dataType), ordinalPosition, true);
    }

    public PrestoValueConverter<?> getConverter(){
        if (converter == null)
            converter = dataType.getConverter();
        return converter;
    }

    public void writeValue(BlockBuilder blockBuilder, Object cyodaNative, SchemaTableName tableName){
        getConverter().writeCyodaNative(columnType, blockBuilder, cyodaNative, tableName.toString() + "." + columnName);
    }

    @JsonIgnore
    public Object getValue(Map<String, Object> row){
        Object result = row.get(columnName);
        if (result != null) {
            return result;
        } else {
            return grabRowValue(row);
        }
    }
    private Object grabRowValue(Object source){
        if (grabber == null){
            synchronized (this){
                if (grabber == null){
                    grabber = new RowValueGrabber(columnName);
                }
            }
        }
        return grabber.grab(source);
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
    public String getColumnName() {
        return columnName;
    }

    @JsonProperty
    public String getColumnKey() {
        return columnKey;
    }

    @JsonProperty
    public String getExternalName() {
        return externalName;
    }

    @JsonProperty
    public ColumnCategory getColumnCategory() {
        return columnCategory;
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
        return ordinalPosition == that.ordinalPosition && columnName.equals(that.columnName) && columnType.equals(that.columnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnName, columnType, ordinalPosition);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("columnName", columnName)
                .add("columnType", columnType)
                .add("ordinalPosition", ordinalPosition)
                .toString();
    }

    public enum ColumnCategory {
        DATA, ROOT, SPECIAL, REPORT, INDEX
    }
    public enum SpecialColumn {
        ENTITY_ID, POINT_TIME
    }

}
