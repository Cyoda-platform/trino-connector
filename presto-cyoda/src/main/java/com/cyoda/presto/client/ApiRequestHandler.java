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

package com.cyoda.presto.client;

import com.cyoda.presto.CyodaTable;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.types.DataTypeValue;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.facebook.presto.spi.SchemaTableName;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public interface ApiRequestHandler<T> {
    String getHandlerKey();

    boolean hasTable(SchemaTableName tableName);

    List<CyodaTable> getTables();

    @SuppressWarnings("java:S1452")
    DataTypeValue<?> getValue(@Nullable T entity, CyodaColumnHandle field);

    Iterator<T> getResponseIterator(int pageSize, CyodaTableHandle tableHandle, CompoundPredicateNode predicates);
}
