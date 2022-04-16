package com.cyoda.presto;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class CyodaConnectorId {
    private final String id;

    public CyodaConnectorId(String id)
    {
        this.id = requireNonNull(id, "id is null");
    }

    @Override
    public String toString()
    {
        return id;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        CyodaConnectorId other = (CyodaConnectorId) obj;
        return Objects.equals(this.id, other.id);
    }
}
