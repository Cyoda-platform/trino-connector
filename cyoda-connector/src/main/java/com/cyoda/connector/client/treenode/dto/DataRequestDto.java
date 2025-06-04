package com.cyoda.connector.client.treenode.dto;

import com.cyoda.connector.client.treenode.dto.conditions.AbstractTrinoConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.complex.AbstractConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.complex.GroupConditionDto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataRequestDto {
    private final UUID metaClassId;
    private final String userId;
    private final Map<String,Map<String, AbstractTrinoConditionDto>> domainCondition;
    private final GroupConditionDto expressionCondition;
    private final String path;
    private final List<String> selectedFields;

    @JsonCreator
    public DataRequestDto(@JsonProperty("metaClassId") UUID metaClassId,
                          @JsonProperty("userId") String userId,
                          @JsonProperty("path") String path,
                          @JsonProperty("domainCondition") Map<String,Map<String,AbstractTrinoConditionDto>> domainCondition,
                          @JsonProperty("expressionCondition") GroupConditionDto expressionCondition,
                          @JsonProperty("selectedFields") List<String> selectedFields
    ) {
        this.metaClassId = metaClassId;
        this.userId = userId;
        this.expressionCondition = expressionCondition;
        this.domainCondition = domainCondition;
        this.path = path;
        this.selectedFields = selectedFields;
    }

    @JsonProperty
    public UUID getMetaClassId() {
        return metaClassId;
    }

    @JsonProperty
    public String getUserId() {
        return userId;
    }

    @JsonProperty
    public Map<String,Map<String,AbstractTrinoConditionDto>> getDomainCondition() {
        return domainCondition;
    }

    @JsonProperty
    public GroupConditionDto getExpressionCondition() {
        return expressionCondition;
    }

    @JsonProperty
    public String getPath() {
        return path;
    }

    @JsonProperty
    public List<String> getSelectedFields() {
        return selectedFields;
    }


    public Map<String, String>  toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("metaClassId", metaClassId.toString());
        map.put("userId", userId);
        map.put("path", path);
        if (domainCondition != null) {
            for (Map.Entry<String, Map<String, AbstractTrinoConditionDto>> entry1 : domainCondition.entrySet()) {
                for (Map.Entry<String, AbstractTrinoConditionDto> entry2 : entry1.getValue().entrySet()) {
                    map.put(entry1.getKey() + "." + entry2.getKey(), entry2.getValue().toString());
                }
            }
        }
        map.put("expressionCondition", expressionCondition.toString());
        return map;
    }
}
