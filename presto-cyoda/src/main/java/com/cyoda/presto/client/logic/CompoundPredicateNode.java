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

package com.cyoda.presto.client.logic;

import com.cyoda.presto.handles.CyodaColumnHandle;
import com.google.common.base.Joiner;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


public class CompoundPredicateNode implements PredicateNode<Any> {

    public static final CompoundPredicateNode EMPTY = new CompoundPredicateNode(Collections.emptyList(), Connective.NONE);
    /**
     * The members of the compound predicate.
     * If this collection is not empty, then the predicate member of this node should be null.
     */
    private final Collection<PredicateNode<?>> members;
    /**
     * The type of connective for the members, i.e. AND / OR
     */
    private final Connective connective;
    private final CyodaColumnHandle column;

    private CompoundPredicateNode(
            Collection<PredicateNode<?>> members,
            Connective connective) {

        this.members = Objects.requireNonNull(members,"members is null");
        Set<CyodaColumnHandle> columns = members.stream()
                .map(PredicateNode::getColumn)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
        if ( columns.size() > 1 ) {
            List<String> columnNames = columns.stream().map(CyodaColumnHandle::getColumnName).collect(Collectors.toList());
            throw new IllegalArgumentException("All members must be for the same Column: "+ Joiner.on(",").join(columnNames));
        }
        this.column = columns.size() == 1 ? ((PredicateNode<?>)members.toArray()[0]).getColumn().orElse(null) : null;
        this.connective = connective;
    }

    public static CompoundPredicateNode of(Collection<PredicateNode<?>> members, Connective connective) {
        return new CompoundPredicateNode(members, connective);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    @Nonnull
    public Optional<Collection<PredicateNode<?>>> getMembers() {
        return Optional.ofNullable(members);
    }

    @Override
    @Nonnull
    public Optional<CyodaColumnHandle> getColumn() {
        return Optional.ofNullable(column);
    }

    @Nonnull
    @Override
    public Connective getConnective() {
        return connective;
    }

    @Override
    public @Nonnull PredicateNodeType getPredicateNodeType() {
        return PredicateNodeType.COMPOUND;
    }

    @Nonnull
    @Override
    public Optional<Predicate<Any>> getPredicate() {
        return Optional.empty();
    }
}
