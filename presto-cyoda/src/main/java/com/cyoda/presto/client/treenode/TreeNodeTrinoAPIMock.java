package com.cyoda.presto.client.treenode;

import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.treenode.dto.EntityContentDto;
import com.cyoda.presto.client.treenode.dto.list.EntityKeyDto;
import com.cyoda.presto.client.treenode.dto.list.EntityListDto;
import com.cyoda.presto.client.treenode.dto.schema.FieldConfigDto;
import com.cyoda.presto.client.treenode.dto.schema.SchemaConfigDto;
import com.cyoda.presto.client.treenode.dto.schema.TableConfigDto;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TreeNodeTrinoAPIMock {
    private UUID metaClassId = UUID.randomUUID();
    private UUID eId1 = UUID.randomUUID();
    private UUID eId2 = UUID.randomUUID();
    public List<SchemaConfigDto> getSchemas(AuthContext user){
        return Collections.singletonList(
                new SchemaConfigDto("tree_node_schema", //maybe hashmap
                        Collections.singletonList(new TableConfigDto("table", metaClassId,
                                null, null))) //fields are filled at other point
        );
    }
    public TableConfigDto getMetadata(UUID metadataClassId){
        return new TableConfigDto(null, metaClassId, null,
                Arrays.asList(new FieldConfigDto("strfield", "field1", "STRING"),
                        new FieldConfigDto("intfield", "field2", "INTEGER")));
    }
    public EntityListDto getEntityList(UUID metadataClassId, long lastLoadTime){
        return new EntityListDto(new Date(), Arrays.asList(
                new EntityKeyDto(eId1, new Date()),
                new EntityKeyDto(eId2, new Date())
        ));
    }

    public List<EntityContentDto> getEntities(UUID metadataClassId, List<UUID> ids){
        Map<String, String> mockContents = new HashMap<>();
        mockContents.put("field1", "val1");
        mockContents.put("field2", "123");
        return Arrays.asList(
                new EntityContentDto(eId1, null, new Date(), mockContents),
                new EntityContentDto(eId2, null, new Date(), mockContents)
        );
    }
}
