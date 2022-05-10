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
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


public class CompoundPredicateNode implements PredicateNode<Any> {

    public static CompoundPredicateNode empty(CompoundPredicateNode parent) {
        return new CompoundPredicateNode(parent,Collections.emptyList(), Connective.NONE);
    }

    public static Builder builder(Connective connective) {
        return new Builder(null, connective);
    }

    /**
     * The members of the compound predicate.
     * If this collection is not empty, then the predicate member of this node should be null.
     */
    private final Collection<PredicateNode<?>> members;
    /**
     * The type of connective for the members, i.e. AND / OR
     */
    private final Connective connective;
    private final CompoundPredicateNode parent;

    private CompoundPredicateNode(
            CompoundPredicateNode parent,
            Collection<PredicateNode<?>> members,
            Connective connective) {

        this.members = Objects.requireNonNull(members,"members is null");
        this.connective = connective;
        this.parent = parent;
    }


    public Builder childBuilder(Connective connective) {
        return new Builder(this, connective);
    }

    public static CompoundPredicateNode of(CompoundPredicateNode parent,Collection<PredicateNode<?>> members, Connective connective) {
        return new CompoundPredicateNode(parent, members, connective);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    @Nonnull
    public Optional<Collection<PredicateNode<?>>> getMembers() {
        return Optional.of(members);
    }

    @Override
    @Nonnull
    public Optional<CyodaColumnHandle> getColumn() {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<CompoundPredicateNode> getParent() {
        return Optional.ofNullable(parent);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompoundPredicateNode that = (CompoundPredicateNode) o;
        return com.google.common.base.Objects.equal(members, that.members) && connective == that.connective;
    }

    @Override
    public int hashCode() {
        return com.google.common.base.Objects.hashCode(members, connective);
    }

    @Nonnull
    @Override
    public PredicateNode<Any> withParent(CompoundPredicateNode parent) {
        return new CompoundPredicateNode(
                parent,
                members.stream().map(it -> it.withParent(parent)).collect(Collectors.toList()),
                connective
        );
    }

    public static class Builder {

        private final Connective connective;
        private final CompoundPredicateNode parent;
        ImmutableList.Builder<PredicateNode<?>> membersBuilder;

        private Builder(CompoundPredicateNode parent, Connective connective) {
            this.connective = connective;
            this.parent = parent;
            this.membersBuilder = ImmutableList.builder();
        }

        public Builder addMember(PredicateNode<?> node) {
            membersBuilder.add(node.withParent(parent));
            return this;
        }

        public <S extends Comparable<? super S>> Builder addLeaf(Predicate<S> predicate) {
            membersBuilder.add(LeafPredicateNode.leaf(parent,predicate));
            return this;
        }

        public CompoundPredicateNode build() {
            return new CompoundPredicateNode(parent, membersBuilder.build(),connective);
        }
    }
}
