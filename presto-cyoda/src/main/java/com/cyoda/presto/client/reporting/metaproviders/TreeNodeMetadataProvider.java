package com.cyoda.presto.client.reporting.metaproviders;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.stats.ContentIdLoadingCache;
import com.cyoda.presto.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.presto.client.treenode.CyodaRSocketClient;
import com.cyoda.presto.client.treenode.dto.schema.SchemaConfigDto;
import com.cyoda.presto.client.treenode.dto.schema.TableConfigDto;
import com.cyoda.presto.client.types.CompoundDataType;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import com.cyoda.presto.handles.CyodaTableType;
import com.cyoda.presto.logging.SupplierLogger;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.predicate.TupleDomain;
import io.trino.spi.type.TypeManager;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class TreeNodeMetadataProvider extends TableMetadataProvider {

    protected static final SupplierLogger LOG = SupplierLogger.get(DynamicReportMetadataProvider.class);
    protected final ContentIdLoadingCache<TableMetaCacheKey, CyodaTableMeta> tableMetaCache;
    private final CyodaRSocketClient rSocketClient;
    @Inject
    public TreeNodeMetadataProvider(TypeManager typeManager, CyodaConfig config, CyodaConnectorId connectorId, CyodaCacheMonitor cacheMonitor, CyodaRSocketClient rSocketClient) {
        super(typeManager, config, connectorId);
        tableMetaCache = new ContentIdLoadingCache<>(Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(5))
                .recordStats()
                .build(key -> {
                    LOG.debug("Loading config " + key);
                    return getTableHandleFromCyoda(key);
                }));
        this.rSocketClient = rSocketClient;
        cacheMonitor.register("TDB_META", tableMetaCache, TableMetaCacheKey::toString, x->1);
    }

    @Override
    public List<CyodaTableHandle> listTables(AuthContext authContext) {
        List<CyodaTableHandle> result = new ArrayList<>();
        List<SchemaConfigDto> schemas = rSocketClient.treeNode().getSchemas(authContext.getUserId());
        schemas.forEach(schemaConfigDto -> {
            schemaConfigDto.getTables().forEach(tableConfigDto -> {
                String tableId = tableConfigDto.getMetadataClassId().toString() + "|" + tableConfigDto.getUniformedPath();
                result.add(new CyodaTableHandle(schemaConfigDto.getSchemaName(), tableConfigDto.getTableName(),
                        CyodaTableType.TREE_NODE_TABLE, tableId, 0, 0, TupleDomain.all()));
            });
        });
        return result;
    }

    protected CyodaTableMeta getTableHandleFromCyoda(TableMetaCacheKey cacheKey) {
        TableConfigDto tableConfigDto = rSocketClient.treeNode().getMetadata(cacheKey.metaClassId);
        AtomicInteger counter = new AtomicInteger(3);
        List<CyodaColumnHandle> columns = new ArrayList<>();
        CompoundDataType uuidType = new CompoundDataType("id", DataType.UUID_TYPE);
        CompoundDataType indexType = new CompoundDataType("index", DataType.INTEGER);
        columns.add(new CyodaColumnHandle("id", uuidType.toPrestoType(typeManager), uuidType, 1, false));
        columns.add(new CyodaColumnHandle("parent_id", uuidType.toPrestoType(typeManager), uuidType, 2, true));
        columns.add(new CyodaColumnHandle("index", indexType.toPrestoType(typeManager), indexType, 3, true));
        columns.addAll(tableConfigDto.getFields().stream().map(dto -> {
            CompoundDataType dataType = new CompoundDataType(dto.getFieldName(), DataType.valueOf(dto.getDataType()));
            return new CyodaColumnHandle(dto.getFieldName(), dto.getFieldKey(), dto.getValuePath(), dataType.toPrestoType(typeManager), dataType, counter.incrementAndGet(), true);
        }).toList());
        return new CyodaTableMeta(cacheKey.schemaTableName.getSchemaName(),
                cacheKey.schemaTableName.getTableName(),
                columns, CyodaTableType.TREE_NODE_TABLE, cacheKey.metaClassId, "Tree node table description", false, false);
    }

    @Override
    public CyodaTableMeta getTableMeta(CyodaTableHandle tableHandle) {
        return tableMetaCache.get(new TableMetaCacheKey(tableHandle.getTableMetaId(), tableHandle.toSchemaTableName()));
    }

    protected static class TableMetaCacheKey {
        protected final String metaClassId;

        private final SchemaTableName schemaTableName;

        protected TableMetaCacheKey(String metaClassId, SchemaTableName schemaTableName) {
            this.metaClassId = metaClassId;
            this.schemaTableName = schemaTableName;
        }

        @Override
        public int hashCode() {
            return metaClassId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            //skip instanceof because we know how to use this class
            TableMetaCacheKey other = (TableMetaCacheKey) obj;
            return metaClassId.equals(other.metaClassId);
        }

        @Override
        public String toString() {
            return schemaTableName.toString() + "(" + metaClassId + ")";
        }
    }
}
