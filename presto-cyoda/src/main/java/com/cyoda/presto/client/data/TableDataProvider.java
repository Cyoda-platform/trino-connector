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

import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.logic.ColumnPredicate;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.block.BlockBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

public abstract class TableDataProvider<T> {

    protected static <T extends Comparable<? super T>> boolean acceptValue(CyodaColumnHandle columnHandle, T value, Set<ColumnPredicate<T>> parsed) {
        ColumnPredicate<T> valuePredicate = new ColumnPredicate<>(ColumnPredicate.PredicateType.EQUALITY, columnHandle, value, null);
        return parsed.stream()
                .map(valuePredicate::merge)
                .map(predicate -> predicate.getType() != ColumnPredicate.PredicateType.NONE)
                .findAny().isPresent();
    }

    public abstract Iterable<T> getIterable(AuthContext authContext, CyodaTableHandle tableHandle, CompoundPredicateNode predicates);

    protected abstract @Nullable Object getFieldValueFromEntity(@Nonnull T entity, CyodaColumnHandle columnHandle);

    public void writeValue(@Nullable T entity, CyodaColumnHandle columnHandle, BlockBuilder blockBuilder) {
        if (entity == null) {
            blockBuilder.appendNull();
            return;
        }
        Object value = getFieldValueFromEntity(entity, columnHandle);
        columnHandle.writeValue(blockBuilder, value);
    }
}
