package com.cyoda.presto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class AuthContext {
    // This is the key, and determines equality.
    protected final String userId;

    public AuthContext(@JsonProperty("userId") String userId) {
        this.userId = userId;
    }

    @JsonProperty
    public String getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthContext that = (AuthContext) o;
        return Objects.equal(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}
