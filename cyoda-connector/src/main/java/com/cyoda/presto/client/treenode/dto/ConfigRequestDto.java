package com.cyoda.presto.client.treenode.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class ConfigRequestDto {
    private final String configId;
    private final String userId;

    @JsonCreator
    public ConfigRequestDto(@JsonProperty("configId") String configId,
                            @JsonProperty("userId") String userId) {
        this.configId = configId;
        this.userId = userId;
    }

    public String getConfigId() {
        return configId;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, String> toMap() {
        Map<String, String> res = new HashMap<>();
        res.put("userId", getUserId());
        res.put("configId", getConfigId());
        return res;
    }

}
