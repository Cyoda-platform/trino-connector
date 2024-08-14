package com.cyoda.connector.client.treenode.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
public class EntityContentDto {
    private final UUID id;
    private final UUID rootId;
    private final UUID parentId;
    private final List<Integer> index;
    private final Date createDate;
    private final Date lastUpdateDate;
    private final Date pointTime;
    private final Map<String, Object> contents;

    @JsonCreator
    public EntityContentDto(@JsonProperty("id") UUID id,
                            @JsonProperty("rootId") UUID rootId,
                            @JsonProperty("parentId") UUID parentId,
                            @JsonProperty("index") List<Integer> index,
                            @JsonProperty("createDate") Date createDate,
                            @JsonProperty("lastUpdateDate") Date lastUpdateDate,
                            @JsonProperty("pointTime") Date pointTime,
                            @JsonProperty("contents") Map<String, Object> contents) {
        this.id = id;
        this.rootId = rootId;
        this.parentId = parentId;
        this.index = index;
        this.createDate = createDate;
        this.lastUpdateDate = lastUpdateDate;
        this.pointTime = pointTime;
        this.contents = contents;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRootId() {
        return rootId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public List<Integer> getIndex() {
        return index;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public Date getPointTime() {
        return pointTime;
    }

    public Map<String, Object> getContents() {
        return contents;
    }
}
