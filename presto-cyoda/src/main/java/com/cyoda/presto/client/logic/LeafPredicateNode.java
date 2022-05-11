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

public class LeafPredicateNode<T extends Comparable<? super T>> implements PredicateNode<T> {

    private final Predicate<T> predicate;
    private final CompoundPredicateNode parent;

    private LeafPredicateNode(@Nullable CompoundPredicateNode parent, @Nonnull Predicate<T> predicate) {
        this.predicate = requireNonNull(predicate, "predicate is null");
        this.parent = parent;
    }

    public static PredicateNode<Any> all(CompoundPredicateNode parent,CyodaColumnHandle handle) {
        return new LeafPredicateNode<>(parent,PredicateUtils.all(handle));
    }

    public static <T extends Comparable<T>> PredicateNode<T> rootNodeWithNothing() {
        return new LeafPredicateNode<>(null,PredicateUtils.<T>nothing());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeafPredicateNode<?> that = (LeafPredicateNode<?>) o;
        return Objects.equal(predicate, that.predicate) && Objects.equal(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(predicate, parent);
    }

    public static <T extends Comparable<? super T>> LeafPredicateNode<T> leaf(CompoundPredicateNode parent, Predicate<T> predicate) {
        return new LeafPredicateNode<>(parent,predicate);
    }

    @Override
    public boolean isLeaf() {
        return predicate != null;
    }

    @Nonnull
    @Override
    public Optional<Predicate<T>> getPredicate() {
        return Optional.of(predicate);
    }

    public Predicate<T> forceGet() {
        return predicate;
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
    public Optional<Collection<PredicateNode<?>>> getMembers() {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<CyodaColumnHandle> getColumn() {
        return Optional.ofNullable(predicate.getColumn());
    }

    @Nonnull
    @Override
    public Optional<CompoundPredicateNode> getParent() {
        return Optional.ofNullable(parent);
    }

    @Nonnull
    @Override
    public PredicateNode<T> withParent(CompoundPredicateNode parent) {
        return new LeafPredicateNode<>(parent,predicate);
    }
}
