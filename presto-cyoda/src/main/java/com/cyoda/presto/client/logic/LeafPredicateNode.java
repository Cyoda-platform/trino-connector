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
import com.google.common.base.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class LeafPredicateNode<T extends Comparable<? super T>> implements ColumnPredicateNode<T> {

    private final ColumnPredicate<T> columnPredicate;
    private final CompoundPredicateNode parent;

    private LeafPredicateNode(@Nullable CompoundPredicateNode parent, @Nonnull ColumnPredicate<T> columnPredicate) {
        this.columnPredicate = requireNonNull(columnPredicate, "predicate is null");
        this.parent = parent;
    }

    public static ColumnPredicateNode<Any> all(CompoundPredicateNode parent, CyodaColumnHandle handle) {
        return new LeafPredicateNode<>(parent, ColumnPredicateUtils.all(handle));
    }

    public static <T extends Comparable<T>> ColumnPredicateNode<T> rootNodeWithNothing() {
        return new LeafPredicateNode<>(null, ColumnPredicateUtils.<T>nothing());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeafPredicateNode<?> that = (LeafPredicateNode<?>) o;
        return Objects.equal(columnPredicate, that.columnPredicate) && Objects.equal(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(columnPredicate, parent);
    }

    public static <T extends Comparable<? super T>> LeafPredicateNode<T> leaf(CompoundPredicateNode parent, ColumnPredicate<T> columnPredicate) {
        return new LeafPredicateNode<>(parent, columnPredicate);
    }

    @Override
    public boolean isLeaf() {
        return columnPredicate != null;
    }

    @Nonnull
    @Override
    public Optional<ColumnPredicate<T>> getPredicate() {
        return Optional.of(columnPredicate);
    }

    public ColumnPredicate<T> forceGet() {
        return columnPredicate;
    }

    @Override
    public @Nonnull PredicateNodeType getPredicateNodeType() {
        return PredicateNodeType.LEAF;
    }

    @Nonnull
    @Override
    public Connective getConnective() {
        return Connective.NONE;
    }

    @Nonnull
    @Override
    public Optional<Collection<ColumnPredicateNode<?>>> getMembers() {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<CyodaColumnHandle> getColumn() {
        return Optional.ofNullable(columnPredicate.getColumn());
    }

    @Nonnull
    @Override
    public Optional<CompoundPredicateNode> getParent() {
        return Optional.ofNullable(parent);
    }

    @Nonnull
    @Override
    public ColumnPredicateNode<T> withParent(CompoundPredicateNode parent) {
        return new LeafPredicateNode<>(parent, columnPredicate);
    }
}
