package com.cyoda.connector.client.treenode.dto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RawEntityContentDto {
    private final UUID id;
    private final UUID entityModelClassId;
    private final String entityModelName;
    private final int entityModelVersion;
    private final UUID rootId;
    private final UUID parentId;
    private final String path;
    private final Integer depth;
    private final List<Integer> index;
    private final String uniformedPath;
    private final Date lastUpdateDate;
    private final Map<String, String> contents;
    private final Map<String, String> typeReference;

    @JsonCreator
    public RawEntityContentDto(
            @JsonProperty("id") UUID id,
            @JsonProperty("entityModelClassId") UUID entityModelClassId,
            @JsonProperty("entityModelName") String entityModelName,
            @JsonProperty("entityModelVersion") int entityModelVersion,
            @JsonProperty("rootId") UUID rootId,
            @JsonProperty("parentId") UUID parentId,
            @JsonProperty("path") String path,
            @JsonProperty("depth") Integer depth,
            @JsonProperty("index") List<Integer> index,
            @JsonProperty("uniformedPath") String uniformedPath,
            @JsonProperty("lastUpdateDate") Date lastUpdateDate,
            @JsonProperty("contents") Map<String, String> contents,
            @JsonProperty("typeReference") Map<String, String> typeReference
    ) {
        this.id = id;
        this.entityModelClassId = entityModelClassId;
        this.entityModelName = entityModelName;
        this.entityModelVersion = entityModelVersion;
        this.rootId = rootId;
        this.parentId = parentId;
        this.path = path;
        this.depth = depth;
        this.index = index;
        this.uniformedPath = uniformedPath;
        this.lastUpdateDate = lastUpdateDate;
        this.contents = contents;
        this.typeReference = typeReference;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEntityModelClassId() {
        return entityModelClassId;
    }

    public String getEntityModelName() {
        return entityModelName;
    }

    public int getEntityModelVersion() {
        return entityModelVersion;
    }

    public UUID getRootId() {
        return rootId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public String getPath() {
        return path;
    }

    public Integer getDepth() {
        return depth;
    }

    public List<Integer> getIndex() {
        return index;
    }

    public String getUniformedPath() {
        return uniformedPath;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public Map<String, String> getContents() {
        return contents;
    }

    public Map<String, String> getTypeReference() {
        return typeReference;
    }
}
