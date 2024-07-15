package com.cyoda.connector.handles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.SchemaTableName;
import com.google.common.base.Joiner;
import io.trino.spi.predicate.TupleDomain;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class CyodaTableHandle implements ConnectorTableHandle {
    private final String schemaName;
    private final String tableName;
    private final CyodaTableType tableType;
    private final String tableMetaId;
    private final long createDate;
    private final long lastUpdateDate;
    private TupleDomain<ColumnHandle> constraint;
    private List<String> selectedFields;
    private List<String> sortingFields;
    private Long limit;

    @JsonCreator
    public CyodaTableHandle(
            @JsonProperty("schemaName") String schemaName,
            @JsonProperty("tableName") String tableName,
            @JsonProperty("tableType") CyodaTableType tableType,
            @JsonProperty("tableMetaId") String tableMetaId,
            @JsonProperty("createDate") long createDate,
            @JsonProperty("lastUpdateDate") long lastUpdateDate,
            @JsonProperty("constraint")TupleDomain<ColumnHandle> constraint,
            @JsonProperty("selectedFields") List<String> selectedFields,
            @JsonProperty("sortingFields") List<String> sortingFields,
            @JsonProperty("limit") Long limit) {
        this.schemaName = requireNonNull(schemaName, "schemaName is null");
        this.tableName = requireNonNull(tableName, "tableName is null");
        this.tableMetaId = tableMetaId;
        this.tableType = tableType;
        this.createDate = createDate;
        this.lastUpdateDate = lastUpdateDate;
        this.constraint = constraint;
        this.selectedFields = selectedFields;
        this.sortingFields = sortingFields;
        this.limit = limit;
    }
    //for static tables
    public CyodaTableHandle(String schemaName, String tableName, CyodaTableType tableType){
        this(schemaName, tableName, tableType, null, 0,0, null, null, null, null);
    }

    public CyodaTableHandle withConstraint(TupleDomain<ColumnHandle> newConstraint) {
        return new CyodaTableHandle(
                this.schemaName,
                this.tableName,
                this.tableType,
                this.tableMetaId,
                this.createDate,
                this.lastUpdateDate,
                newConstraint,
                this.selectedFields,
                this.sortingFields,
                this.limit
        );
    }


    @JsonProperty
    public String getSchemaName() {
        return schemaName;
    }

    @JsonProperty
    public String getTableName() {
        return tableName;
    }

    @JsonProperty
    public CyodaTableType getTableType() {
        return tableType;
    }

    @JsonProperty
    public String getTableMetaId() {
        return tableMetaId;
    }
//
//    @JsonProperty
//    public String getSubTableId() {
//        return subTableId;
//    }

    @JsonProperty
    public long getCreateDate() {
        return createDate;
    }

    @JsonProperty
    public long getLastUpdateDate() {
        return lastUpdateDate;
    }

    @JsonProperty
    public TupleDomain<ColumnHandle> getConstraint() {
        return constraint;
    }
    public void setConstraint(TupleDomain<ColumnHandle> constraint) {
        this.constraint = constraint;
    }

    @JsonProperty
    public List<String> getSelectedFields() {
        return selectedFields;
    }

    public void setSelectedFields(List<String> selectedFields) {
        this.selectedFields = selectedFields;
    }

    @JsonProperty
    public List<String> getSortingFields() {
        return sortingFields;
    }

    public void setSortingFields(List<String> sortingFields) {
        this.sortingFields = sortingFields;
    }

    @JsonProperty
    public Long getLimit() {
        return limit;
    }

    public void setLimit(Long limit) {
        this.limit = limit;
    }

    public SchemaTableName toSchemaTableName() {
        return new SchemaTableName(schemaName, tableName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CyodaTableHandle that = (CyodaTableHandle) o;
        return  schemaName.equals(that.schemaName)
                && tableName.equals(that.tableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaName, tableName);
    }

    @Override
    public String toString() {
        return Joiner.on(".").join(schemaName, tableName, tableType) + " (MetaID="+tableMetaId+")";
    }

}
