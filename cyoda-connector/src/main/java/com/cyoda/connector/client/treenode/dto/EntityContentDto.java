package com.cyoda.connector.client.treenode.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
public class EntityContentDto {
    private final UUID rootId;
    private final List<Integer> index;
    private final Date pointTime;
    private final Map<String, Object> rootFields;
    private final Map<String, Object> contents;
    private final String reconstructedEntity;

    @JsonCreator
    public EntityContentDto(@JsonProperty("rootId") UUID rootId,
                            @JsonProperty("index") List<Integer> index,
                            @JsonProperty("pointTime") Date pointTime,
                            @JsonProperty("rootFields") Map<String, Object> rootFields,
                            @JsonProperty("contents") Map<String, Object> contents,
                            @JsonProperty("reconstructedEntity") String reconstructedEntity) {
        this.rootId = rootId;
        this.index = index;
        this.pointTime = pointTime;
        this.rootFields = rootFields;
        this.contents = contents;
        this.reconstructedEntity = reconstructedEntity;
    }

    @JsonProperty("rootId")
    public UUID getRootId() {
        return rootId;
    }

    @JsonProperty("index")
    public List<Integer> getIndex() {
        return index;
    }

    @JsonProperty("pointTime")
    public Date getPointTime() {
        return pointTime;
    }

    @JsonProperty("rootFields")
    public Map<String, Object> getRootFields() {
        return rootFields;
    }

    @JsonProperty("contents")
    public Map<String, Object> getContents() {
        return contents;
    }

    @JsonProperty("reconstructedEntity")
    public String getReconstructedEntity() {
        return reconstructedEntity;
    }

    @Override
    public String toString() {
        return "Entity(ID=" + rootId + ")";
    }
}
