package com.cyoda.presto.handles;

import com.cyoda.presto.DummyPageSource;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.CyodaApiRequestHandlerProvider;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.types.CompoundDataType;
import com.cyoda.presto.client.types.DataType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ConnectorPageSource;
import io.trino.spi.type.VarcharType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DummyTableHandle extends CyodaTableHandle {

    private final Map<String, String> content;

    @JsonCreator
    public DummyTableHandle(@JsonProperty("connectorId") String connectorId,
                            @JsonProperty("schemaName") String schemaName,
                            @JsonProperty("tableName") String tableName,
                            @JsonProperty("content") Map<String, String> content) {
        super(connectorId, schemaName, tableName,
                createDummyFields(content.keySet()),
                "NULL", null, null, null);
        this.content = content;
    }

    private static List<CyodaColumnHandle> createDummyFields(Set<String> fields){
        return fields.stream().map(f->new CyodaColumnHandle(f,
                VarcharType.VARCHAR,
                new CompoundDataType(f, DataType.STRING),
                0, true)).collect(Collectors.toList());
    }

    @JsonProperty
    public Map<String, String> getContent() {
        return content;
    }

    @Override
    public ConnectorPageSource getPageSource(AuthContext authContext, CyodaApiRequestHandlerProvider handlerProvider, List<CyodaColumnHandle> cyodaColumns, CompoundPredicateNode predicates) {
        return new DummyPageSource(this);
    }


}
