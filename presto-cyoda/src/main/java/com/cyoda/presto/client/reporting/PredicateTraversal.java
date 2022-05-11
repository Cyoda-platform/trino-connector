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

package com.cyoda.presto.client.reporting;

import com.cyoda.presto.CyodaErrorCode;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.logic.Connective;
import com.cyoda.presto.client.logic.LeafPredicateNode;
import com.cyoda.presto.client.logic.Predicate;
import com.cyoda.presto.client.logic.PredicateNode;
import com.facebook.presto.spi.PrestoException;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO: Check if Predicate#merge can do this
 */
public class PredicateTraversal {

    private final CompoundPredicateNode conjunctions;

    private PredicateTraversal(@Nonnull Collection<PredicateNode<?>> conjunctions) {
        CompoundPredicateNode.Builder builder = CompoundPredicateNode.builder(Connective.AND);
        conjunctions.forEach(builder::addMember);
        this.conjunctions = builder.build();
    }

    public static @Nonnull PredicateTraversal of(@Nonnull Collection<PredicateNode<?>> conjunctions){
        Preconditions.checkNotNull(conjunctions,"conjunctions is null");
        return new PredicateTraversal(conjunctions);
    }

    private static class Predicated<T> {
        private final T element;
        private final Connective connective;

        private Predicated(T element, Connective connective) {
            this.element = element;
            this.connective = connective;
        }
    }
    /**
     *
     * @param columnName to search filterings for
     * @return empty if nothing is to be selected, i.e. an empty result should be passed up the stack.
     *
     */
    public Optional<Set<String>> assembleFilterings(String columnName) {

        Deque<CompoundPredicateNode> compoundQueue = new ArrayDeque<>();
        Deque<Predicated<LeafPredicateNode<?>>> leafQueue = new ArrayDeque<>();

        compoundQueue.add(conjunctions);

        while(!compoundQueue.isEmpty()) {
            CompoundPredicateNode node = compoundQueue.pop();
            Collection<PredicateNode<?>> members = node.getMembers().orElse(Collections.emptyList());
            members.forEach(member -> {
                if (member instanceof LeafPredicateNode) {
                    if (member.getColumn().isPresent() && member.getColumn().get().getColumnName().equals(columnName)) {
                        leafQueue.add(new Predicated<>((LeafPredicateNode<?>)member, node.getConnective()));
                    }
                } else {
                    compoundQueue.add((CompoundPredicateNode) member);
                }
            });
        }

        Deque<Predicated<String>> resultBuilder = new ArrayDeque<>();
        if ( !leafQueue.isEmpty() ) {
            Predicated<LeafPredicateNode<?>> predicated = leafQueue.pop();
            extractFilterValues(predicated.element.forceGet())
                    .map(it->new Predicated<>(it,predicated.connective))
                    .forEach(resultBuilder::add);
        }
        while (!leafQueue.isEmpty()) {
            Predicated<LeafPredicateNode<?>> predicated = leafQueue.pop();
            if ( predicated.element.forceGet().getColumn().getColumnName().equals(columnName) ) {
                Predicated<String> last = resultBuilder.peekLast();
                if (last == null) throw new IllegalStateException("This should not happen");
                Connective previousConnective = last.connective;
                if (previousConnective == Connective.OR) {
                    extractFilterValues(predicated.element.forceGet())
                            .map(it -> new Predicated<>(it, predicated.connective))
                            .forEach(resultBuilder::add);

                } else {
                    Set<String> values = extractFilterValues(predicated.element.forceGet()).collect(Collectors.toSet());
                    if (!values.contains(last.element) ) {
                        return Optional.empty();
                    }
                }
            }
        }
        return Optional.of(resultBuilder.stream().map(it->it.element).collect(Collectors.toSet()));

    }

    private Stream<String> extractFilterValues(Predicate<?> it) {
        Predicate.PredicateType type = it.getType();
        switch (type) {
            case IN_LIST: {
                return it.getInListValues().stream()
                        .map(item -> Optional.ofNullable(item.value)
                                .map(Object::toString)
                                .orElse(null)
                        );
            }
            case EQUALITY: {
                return Stream.of(
                        Optional.ofNullable(it.getLower().value)
                                .map(Object::toString)
                                .orElse(null)
                );
            }
            case RANGE: {
                return Stream.of(
                        Optional.ofNullable(it.getLower()).map(Object::toString).orElse(null),
                        Optional.ofNullable(it.getUpper()).map(Object::toString).orElse(null)
                );
            }
            default:
                throw new PrestoException(
                        CyodaErrorCode.CYODA_PUSHDOWN_UNSUPPORTED_EXPRESSION,
                        type + " is not supported here");
        }
    }
}
