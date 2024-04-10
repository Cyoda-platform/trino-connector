package com.cyoda.presto.client.treenode.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
public class EntityContentDto {
    private final UUID id;
    private final UUID parentId;
    private final Integer index;
    private final Date lastUpdateDate;
    private final Map<String, Object> contents;

    @JsonCreator
    public EntityContentDto(@JsonProperty("id") UUID id,
                            @JsonProperty("parentId") UUID parentId,
                            @JsonProperty("index") Integer index,
                            @JsonProperty("lastUpdateDate") Date lastUpdateDate,
                            @JsonProperty("contents") Map<String, Object> contents) {
        this.id = id;
        this.parentId = parentId;
        this.index = index;
        this.lastUpdateDate = lastUpdateDate;
        this.contents = contents;
    }

    public UUID getId() {
        return id;
    }

    public UUID getParentId() {
        return parentId;
    }

    public Integer getIndex() {
        return index;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public Map<String, Object> getContents() {
        return contents;
    }
}
