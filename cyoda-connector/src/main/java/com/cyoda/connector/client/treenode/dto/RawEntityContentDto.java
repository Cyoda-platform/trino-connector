package com.cyoda.connector.client.treenode.dto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
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
    private final int siblingIndex;
    private final String parentPath;
    private final String path;
    private final Integer depth;
    private final Integer index;
    private final String uniformedPath;
    private final Map<String, UUID> siblings;
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
            @JsonProperty("siblingIndex") int siblingIndex,
            @JsonProperty("parentPath") String parentPath,
            @JsonProperty("path") String path,
            @JsonProperty("depth") Integer depth,
            @JsonProperty("index") Integer index,
            @JsonProperty("uniformedPath") String uniformedPath,
            @JsonProperty("siblings") Map<String, UUID> siblings,
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
        this.siblingIndex = siblingIndex;
        this.parentPath = parentPath;
        this.path = path;
        this.depth = depth;
        this.index = index;
        this.uniformedPath = uniformedPath;
        this.siblings = siblings;
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

    public int getSiblingIndex() {
        return siblingIndex;
    }

    public String getParentPath() {
        return parentPath;
    }

    public String getPath() {
        return path;
    }

    public Integer getDepth() {
        return depth;
    }

    public Integer getIndex() {
        return index;
    }

    public String getUniformedPath() {
        return uniformedPath;
    }

    public Map<String, UUID> getSiblings() {
        return siblings;
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
