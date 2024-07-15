package com.cyoda.connector.client.treenode.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataRequestDto {
    private final UUID metaClassId;
    private final String userId;
    private final String condition;
    private final String uniformedPath;
    private final Date pointTime;
    private final List<String> selectedFields;
    private final List<String> sortingFields;
    private final Long limit;

    @JsonCreator
    public DataRequestDto(@JsonProperty("metaClassId") UUID metaClassId,
                          @JsonProperty("userId") String userId,
                          @JsonProperty("uniformedPath") String uniformedPath,
                          @JsonProperty("condition") String condition,
                          @JsonProperty("pointTime") Date pointTime,
                          @JsonProperty("selectedFields") List<String> selectedFields,
                          @JsonProperty("sortingFields") List<String> sortingFields,
                          @JsonProperty("limit") Long limit) {
        this.metaClassId = metaClassId;
        this.userId = userId;
        this.condition = condition;
        this.uniformedPath = uniformedPath;
        this.pointTime = pointTime;
        this.selectedFields = selectedFields;
        this.sortingFields = sortingFields;
        this.limit = limit;
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
    public String getCondition() {
        return condition;
    }

    @JsonProperty
    public String getUniformedPath() {
        return uniformedPath;
    }

    @JsonProperty
    public Date getPointTime() {
        return pointTime;
    }

    @JsonProperty
    public List<String> getSelectedFields() {
        return selectedFields;
    }

    @JsonProperty
    public List<String> getSortingFields() {
        return sortingFields;
    }

    @JsonProperty
    public Long getLimit() {
        return limit;
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("metaClassId", metaClassId.toString());
        map.put("userId", userId);
        map.put("uniformedPath", uniformedPath);
        map.put("condition", condition);
        return map;
    }
}
