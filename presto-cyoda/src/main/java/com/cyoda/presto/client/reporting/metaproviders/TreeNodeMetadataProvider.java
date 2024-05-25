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

import jakarta.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TreeNodeMetadataProvider extends TableMetadataProvider {

    protected static final SupplierLogger LOG = SupplierLogger.get(DynamicReportMetadataProvider.class);
    protected final ContentIdLoadingCache<String, List<String>> userSchemaCache;
    protected final ContentIdLoadingCache<String, Map<SchemaTableName, CyodaTableMeta>> schemaCache;

    private final CyodaRSocketClient rSocketClient;
    @Inject
    public TreeNodeMetadataProvider(TypeManager typeManager, CyodaConfig config, CyodaConnectorId connectorId, CyodaCacheMonitor cacheMonitor, CyodaRSocketClient rSocketClient) {
        super(typeManager, config, connectorId);
        schemaCache = new ContentIdLoadingCache<>(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(10))
                .recordStats()
                .build(schemaName -> {
                    LOG.debug("Loading single schema " + schemaName);
                    return loadSchema(schemaName);
                }));
        userSchemaCache = new ContentIdLoadingCache<>(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(10))
                .recordStats()
                .build(userId -> {
                    LOG.debug("Loading schemas for " + userId);
                    return loadSchemas(userId);
                }
        ));
        this.rSocketClient = rSocketClient;
        cacheMonitor.register("TDB_USER", userSchemaCache, Function.identity(), x->1);
        cacheMonitor.register("TDB_SCHEMA", schemaCache, Function.identity(), x->1);
    }

    public Map<SchemaTableName, CyodaTableMeta> loadSchema(String schemaName){
        SchemaConfigDto schemaConfigDto = rSocketClient.treeNodeClient.schemaRequester.retrieveData("META", schemaName).block();
        return mapSchema(schemaConfigDto);
    }

    public Map<SchemaTableName, CyodaTableMeta> mapSchema(SchemaConfigDto schemaConfigDto){
        String schemaName = schemaConfigDto.getSchemaName();
        return schemaConfigDto.getTables().stream().collect(Collectors.toMap(table -> new SchemaTableName(schemaName, table.getTableName()),
                table -> createTableMeta(schemaName, table)));
    }

    public List<String> loadSchemas(String userId){
        List<String> result = new ArrayList<>();
        List<SchemaConfigDto> schemas = rSocketClient.treeNodeClient.schemaListRequester.retrieveData("META", userId).collectList().block();
        schemas.forEach(schemaConfigDto -> {
            String schemaName = schemaConfigDto.getSchemaName();
            result.add(schemaName);
            schemaCache.put(schemaName, mapSchema(schemaConfigDto));
        });
        return result;
    }

    @Override
    public List<CyodaTableHandle> listTables(AuthContext authContext) {
        List<String> schemaNames = userSchemaCache.get(authContext.getUserId());
        return schemaNames.stream().map(schemaCache::get)
                .flatMap(map -> map.entrySet().stream())
                .map(entry -> {
                    CyodaTableMeta tableMeta = entry.getValue();
                    return new CyodaTableHandle(entry.getKey().getSchemaName(), tableMeta.getTableName(), CyodaTableType.TREE_NODE_TABLE,
                            tableMeta.getReportConfigId(), 0,0, TupleDomain.all());
                }).toList();

    }

    protected CyodaTableMeta createTableMeta(String schemaName, TableConfigDto tableConfigDto) {
        AtomicInteger counter = new AtomicInteger(3);
        List<CyodaColumnHandle> columns = new ArrayList<>();
        CompoundDataType uuidType = new CompoundDataType("id", DataType.UUID_TYPE);
        CompoundDataType indexType = new CompoundDataType("index", DataType.INTEGER);
        columns.add(new CyodaColumnHandle("id", uuidType.toPrestoType(typeManager), uuidType, 1, false));
        columns.add(new CyodaColumnHandle("root", uuidType.toPrestoType(typeManager), uuidType, 2, true));
        columns.add(new CyodaColumnHandle("parent", uuidType.toPrestoType(typeManager), uuidType, 2, true));
        columns.add(new CyodaColumnHandle("index", indexType.toPrestoType(typeManager), indexType, 3, true));
        columns.addAll(tableConfigDto.getFields().stream().map(dto -> {
            String dtoDataType = dto.getDataType();
            CompoundDataType dataType;
            if (dtoDataType.startsWith("*")) {
                dataType = new CompoundDataType(dto.getFieldName(), DataType.LIST, DataType.valueOf(dtoDataType.substring(1)));
            } else {
                dataType = new CompoundDataType(dto.getFieldName(), DataType.valueOf(dtoDataType));
            }
            return new CyodaColumnHandle(dto.getFieldName(), dto.getFieldKey(), dto.getValuePath(), dataType.toPrestoType(typeManager), dataType, counter.incrementAndGet(), true);
        }).toList());
        String tableId = tableConfigDto.getMetadataClassId().toString() + "|" + tableConfigDto.getUniformedPath();
        return new CyodaTableMeta(schemaName, tableConfigDto.getTableName(),
                columns, CyodaTableType.TREE_NODE_TABLE, tableId, "Tree node table description", false, false);
    }

    @Override
    public CyodaTableMeta getTableMeta(CyodaTableHandle tableHandle) {
        Map<SchemaTableName, CyodaTableMeta> schemaMap = schemaCache.get(tableHandle.getSchemaName());
        return schemaMap.get(tableHandle.toSchemaTableName());
    }


}
