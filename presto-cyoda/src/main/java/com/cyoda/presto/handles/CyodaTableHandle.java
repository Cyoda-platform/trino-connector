package com.cyoda.presto.handles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.SchemaTableName;
import com.google.common.base.Joiner;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class CyodaTableHandle implements ConnectorTableHandle {
    private final String schemaName;
    private final String tableName;
    private final CyodaTableType tableType;
    private final String reportConfigId;
    private final long createDate;
    private final long lastUpdateDate;


    //    public static CyodaTableHandle of(SchemaTableName schemaTableName){
//        return new CyodaTableHandle(schemaTableName.getSchemaName(), schemaTableName.getTableName());
//    }
    @JsonCreator
    public CyodaTableHandle(
            @JsonProperty("schemaName") String schemaName,
            @JsonProperty("tableName") String tableName,
            @JsonProperty("tableType") CyodaTableType tableType,
            @JsonProperty("reportConfigId") String reportConfigId,
            @JsonProperty("createDate") long createDate,
            @JsonProperty("lastUpdateDate") long lastUpdateDate) {
        this.schemaName = requireNonNull(schemaName, "schemaName is null");
        this.tableName = requireNonNull(tableName, "tableName is null");
        this.reportConfigId = reportConfigId;
        this.tableType = tableType;
        this.createDate = createDate;
        this.lastUpdateDate = lastUpdateDate;
    }
    public CyodaTableHandle(String schemaName, String tableName, CyodaTableType tableType){
        this(schemaName, tableName, tableType, null, 0,0);
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
    public String getReportConfigId() {
        return reportConfigId;
    }

    @JsonProperty
    public long getCreateDate() {
        return createDate;
    }

    @JsonProperty
    public long getLastUpdateDate() {
        return lastUpdateDate;
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
        return Joiner.on(".").join(schemaName, tableName);
    }

}
