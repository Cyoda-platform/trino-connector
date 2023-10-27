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

package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaFilteringPageSource;
import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import com.google.common.collect.ImmutableMap;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.connector.ConnectorPageSource;
import io.trino.spi.connector.Constraint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class TableDataProvider<T> {

    static boolean acceptVal(CyodaColumnHandle column, Object value, Constraint constraint){
        return constraint
                .predicate()
                .map(p -> p.test(ImmutableMap.of(column, column.getConverter().toNullableValue(column.getColumnType(), value))))
                .orElse(true);
    }

    static boolean hasConstraint(CyodaColumnHandle column, Constraint constraint){
        return constraint
                .getPredicateColumns()
                .map(set -> set.contains(column))
                .orElse(false); // no constraint
    }

    public abstract List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint);
    public abstract Iterable<T> getIterable(CyodaTableMeta tableHandle, CyodaSplit split);

    protected abstract @Nullable Object getFieldValueFromEntity(@Nonnull T entity, CyodaColumnHandle columnHandle);

    public ConnectorPageSource getPageSource(CyodaTableMeta tableHandle,
                                             List<CyodaColumnHandle> cyodaColumns,
                                             CyodaSplit split){
        return new CyodaFilteringPageSource<>(
                this,
                tableHandle,
                cyodaColumns, split);
    }

    public final void writeValue(@Nullable T entity, CyodaColumnHandle columnHandle, BlockBuilder blockBuilder) {
        if (entity == null) {
            blockBuilder.appendNull();
            return;
        }
        Object value = getFieldValueFromEntity(entity, columnHandle);
        columnHandle.writeValue(blockBuilder, value);
    }

}
