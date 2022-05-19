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
import com.cyoda.presto.client.logic.ColumnPredicate;
import com.cyoda.presto.client.logic.ColumnPredicateNode;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.logic.Connective;
import com.cyoda.presto.client.logic.LeafPredicateNode;
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

public class PredicateTraversal {

    private final CompoundPredicateNode conjunctions;

    private PredicateTraversal(@Nonnull CompoundPredicateNode predicateNodes) {
        CompoundPredicateNode.Builder builder = CompoundPredicateNode.builder(Connective.AND);
        ColumnPredicateNode.members(predicateNodes).forEach(builder::addMember);
        this.conjunctions = builder.build();
    }

    public static @Nonnull PredicateTraversal of(@Nonnull CompoundPredicateNode predicateNodes){
        Preconditions.checkNotNull(predicateNodes,"conjunctions is null");
        return new PredicateTraversal(predicateNodes);
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
     * Analyse the predicates to extract the list of values for a column that should be filtered on.
     *
     * @param columnName to search for filterings
     * @return
     * <pre>
     * <ul>
     *     <li>{@code Optional.empty()} if <em>nothing</em> is to be <em>selected</em>, i.e. an empty result should be passed up the stack.</li>
     *     <li>an empty {@link Set} if <em>nothing</em> is to be <em>filtered</em>, i.e. take everything.</li>
     *     <li>the list of values to filter, i.e. semantically {@code WHERE columnName IN (....)}</li>
     * </ul>
     * </pre>
     *
     *
     *
     */
    public Optional<Set<String>> assembleFilterings(String columnName) {

        Deque<CompoundPredicateNode> compoundQueue = new ArrayDeque<>();
        Deque<Predicated<LeafPredicateNode<?>>> leafQueue = new ArrayDeque<>();

        compoundQueue.add(conjunctions);

        while(!compoundQueue.isEmpty()) {
            CompoundPredicateNode node = compoundQueue.pop();
            Collection<ColumnPredicateNode<?>> members = node.getMembers().orElse(Collections.emptyList());
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

    private Stream<String> extractFilterValues(ColumnPredicate<?> it) {
        ColumnPredicate.PredicateType type = it.getType();
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
