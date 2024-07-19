package com.cyoda.connector.client.treenode.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataRequestDto {
    private final UUID metaClassId;
    private final String userId;
    private final String condition;
    private final String uniformedPath;
    private final Date pointTime;

    @JsonCreator
    public DataRequestDto(@JsonProperty("metaClassId") UUID metaClassId,
                          @JsonProperty("userId") String userId,
                          @JsonProperty("uniformedPath") String uniformedPath,
                          @JsonProperty("condition") String condition,
                          @JsonProperty("pointTime") Date pointTime) {
        this.metaClassId = metaClassId;
        this.userId = userId;
        this.condition = condition;
        this.uniformedPath = uniformedPath;
        this.pointTime = pointTime;
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

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("metaClassId", metaClassId.toString());
        map.put("userId", userId);
        map.put("uniformedPath", uniformedPath);
        map.put("condition", condition);
        return map;
    }
}
