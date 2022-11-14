/*
 * Copyright (C) 2022 Cyoda Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.cyoda.presto;

import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.predicate.TupleDomain;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.HostAddress;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import java.net.URI;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class CyodaSplit implements ConnectorSplit {
    private final URI uri;
    private final List<HostAddress> addresses;
    private final TupleDomain<ColumnHandle> constraint;
    private final CyodaTableHandle tableHandle;

    @JsonCreator
    public CyodaSplit(
            @JsonProperty("tableHandle") CyodaTableHandle tableHandle,
            @JsonProperty("uri") URI uri,
            @JsonProperty("constraint") TupleDomain<ColumnHandle> constraint) {
        this.tableHandle = requireNonNull(tableHandle, "tableHandle name is null");
        this.uri = requireNonNull(uri, "uri is null");

        addresses = ImmutableList.of(HostAddress.fromUri(uri));
        this.constraint = requireNonNull(constraint, "constraint name is null");
    }


    @JsonProperty
    public CyodaTableHandle getTableHandle() {
        return tableHandle;
    }

    @JsonProperty
    public URI getUri() {
        return uri;
    }

    @JsonProperty
    public TupleDomain<ColumnHandle> getConstraint() {
        return constraint;
    }

    @Override
    public boolean isRemotelyAccessible() {
        return true;
    }
    public List<HostAddress> getAddresses() {
        return addresses;
    }

    @Override
    public Object getInfo() {
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("uri", uri)
                .add("addresses", addresses)
                .add("constraint", constraint)
                .add("tableHandle", tableHandle)
                .toString();
    }
}
