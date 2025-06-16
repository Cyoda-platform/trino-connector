package com.cyoda.connector.client.reporting.metaproviders;

import com.cyoda.connector.CyodaConfig;
import com.cyoda.connector.auth.AuthContext;
import com.cyoda.connector.client.reporting.stats.ContentIdLoadingCache;
import com.cyoda.connector.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.connector.client.treenode.CyodaRSocketClient;
import com.cyoda.connector.client.treenode.dto.schema.FieldConfigDto;
import com.cyoda.connector.client.treenode.dto.schema.SchemaConfigDto;
import com.cyoda.connector.client.treenode.dto.schema.TableConfigDto;
import com.cyoda.connector.client.types.CompoundDataType;
import com.cyoda.connector.client.types.DataType;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.handles.CyodaTableCategory;
import com.cyoda.connector.handles.CyodaTableHandle;
import com.cyoda.connector.handles.CyodaTableMeta;
import com.cyoda.connector.handles.CyodaTableType;
import com.cyoda.connector.logging.SupplierLogger;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.predicate.TupleDomain;
import io.trino.spi.type.TypeManager;

import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TreeNodeMetadataProvider extends TableMetadataProvider {

    protected static final SupplierLogger LOG = SupplierLogger.get(DynamicReportMetadataProvider.class);
    protected final ContentIdLoadingCache<String, List<String>> userSchemaCache;
    protected final ContentIdLoadingCache<String, Map<SchemaTableName, CyodaTableMeta>> schemaCache;

    private final CyodaRSocketClient rSocketClient;
    @Inject
    public TreeNodeMetadataProvider(TypeManager typeManager, CyodaConfig config, CyodaCacheMonitor cacheMonitor, CyodaRSocketClient rSocketClient) {
        super(typeManager, config);
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
    public boolean isEnabled() {
        return CyodaTableCategory.TREE_NODE.isEnabled(config);
    }

    @Override
    public List<CyodaTableHandle> listTables(AuthContext authContext) {
        List<String> schemaNames = userSchemaCache.get(authContext.getUserId());
        return schemaNames.stream().map(schemaCache::get)
                .flatMap(map -> map.entrySet().stream())
                .map(entry -> {
                    CyodaTableMeta tableMeta = entry.getValue();
                    return new CyodaTableHandle(entry.getKey().getSchemaName(), tableMeta.getTableName(), CyodaTableType.TREE_NODE_TABLE,
                            tableMeta.getReportConfigId(), 0,0, TupleDomain.all(), null, null, null, null);
                }).toList();

    }

    protected CyodaTableMeta createTableMeta(String schemaName, TableConfigDto tableConfigDto) {
        AtomicInteger counter = new AtomicInteger(1);
        List<CyodaColumnHandle> columns = tableConfigDto.getFields().stream().flatMap(dto -> {
            if (dto.getArray() && dto.getFlatten()) {
                return dto.getArrayFields().stream().map(fieldConfigDto -> createColumnHandle(fieldConfigDto, counter));
            }
            return Stream.of(createColumnHandle(dto, counter));
        }).sorted(Comparator.comparingInt(CyodaColumnHandle::getOrdinalPosition)).toList();
        String tableId = tableConfigDto.getMetadataClassId().toString() + "|" + tableConfigDto.getUniformedPath();
        return new CyodaTableMeta(schemaName, tableConfigDto.getTableName(),
                columns, CyodaTableType.TREE_NODE_TABLE, tableId, "Tree node table description", false, false);
    }

    @NotNull
    private CyodaColumnHandle createColumnHandle(FieldConfigDto dto, AtomicInteger counter) {
        String dtoDataType = dto.getDataType();
        CompoundDataType dataType;
        if (dto.getArray()) {
            dataType = new CompoundDataType(dto.getFieldName(), DataType.LIST, DataType.valueOf(dtoDataType));
        } else {
            dataType = new CompoundDataType(dto.getFieldName(), DataType.valueOf(dtoDataType));
        }
        return new CyodaColumnHandle(dto.getFieldName(), dto.getFieldKey(), CyodaColumnHandle.ColumnCategory.valueOf(dto.getFieldCategory()),
                null, dataType.toPrestoType(typeManager), dataType, counter.incrementAndGet(), true);
    }

    @Override
    public CyodaTableMeta getTableMeta(CyodaTableHandle tableHandle) {
        Map<SchemaTableName, CyodaTableMeta> schemaMap = schemaCache.get(tableHandle.getSchemaName());
        return schemaMap.get(tableHandle.toSchemaTableName());
    }


}
