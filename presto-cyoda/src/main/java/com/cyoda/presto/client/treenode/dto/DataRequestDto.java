package com.cyoda.presto.client.treenode.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class DataRequestDto {
    private final UUID metaClassId;
    private final String userId;
    private final String condition;
    private final String uniformedPath;

    @JsonCreator
    public DataRequestDto(@JsonProperty("metaClassId") UUID metaClassId,
                          @JsonProperty("userId") String userId,
                          @JsonProperty("uniformedPath") String uniformedPath,
                          @JsonProperty("condition") String condition) {
        this.metaClassId = metaClassId;
        this.userId = userId;
        this.condition = condition;
        this.uniformedPath = uniformedPath;
    }

    public UUID getMetaClassId() {
        return metaClassId;
    }

    public String getUserId() {
        return userId;
    }

    public String getCondition() {
        return condition;
    }

    public String getUniformedPath() {
        return uniformedPath;
    }
}
