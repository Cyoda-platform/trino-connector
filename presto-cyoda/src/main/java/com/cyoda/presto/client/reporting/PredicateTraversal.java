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
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.trino.spi.TrinoException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PredicateTraversal<T extends Comparable<? super T>> {

    private final CompoundPredicateNode conjunctions;

    private PredicateTraversal(@Nonnull CompoundPredicateNode node) {
        this.conjunctions = node;
    }

    public static @Nonnull <T extends Comparable<? super T>> PredicateTraversal<T> of(@Nonnull CompoundPredicateNode predicateNodes, Class<T> clazz) {
        Preconditions.checkNotNull(predicateNodes, "conjunctions is null");
        Preconditions.checkArgument(predicateNodes.getConnective() == Connective.AND || predicateNodes.isEmpty(), "Not a conjunction");
        return new PredicateTraversal<>(predicateNodes);
    }

    /**
     * Analyse the predicates to extract the list of values for a column that should be filtered on.
     *
     * @param columnHandle to parse for
     * @return a list of sorted sets representing OR condition
     * <pre>
     * <ul>
     *      <li>{@code Optional.empty()} if <em>nothing</em> is to be <em>selected</em>, i.e. an empty result should be passed up the stack.</li>
     *      <li>an empty {@link Set} if <em>nothing</em> is to be <em>filtered</em>, i.e. take everything.</li>
     *      <li>the list of values to filter, i.e. semantically {@code WHERE columnName IN (....)}</li>
     * </ul>
     * </pre>
     */
    public List<Optional<SortedSet<T>>> assembleEqualsPredicateValues(CyodaColumnHandle columnHandle) {
        return this.parseFor(columnHandle).stream().map(this::getResult).collect(Collectors.toList());
    }

    public Optional<SortedSet<T>> assembleEqualsPredicateValuesFromAnd(CyodaColumnHandle columnHandle) {
        List<Optional<SortedSet<T>>> results = this.parseFor(columnHandle).stream().map(this::getResult).collect(Collectors.toList());
        Preconditions.checkArgument(results.size() == 1, "Embedded OR conditions in predicate. Not a pure AND predicate");
        return results.get(0);
    }

    private Optional<SortedSet<T>> getResult(ColumnPredicate<T> tColumnPredicate) {
        switch (tColumnPredicate.getType()) {
            case NONE:
                return Optional.empty();
            case ALL:
                return Optional.of(Collections.emptySortedSet());
            default:
                return Optional.of(this.extractFilterValues(tColumnPredicate).collect(Collectors.toCollection(TreeSet::new)));
        }
    }

    /**
     * Analyse the predicates to extract the effective predicate on the column that should be filtered on.
     *
     * @param columnHandle to parse for
     * @return a list of predicates representing OR condition. I will never return an empty list.
     * <pre>
     * <ul>
     *     <li>a column Predicate with Type {@link  ColumnPredicate.PredicateType#NONE } if <em>nothing</em> is to be <em>selected</em>, i.e. an empty result should be passed up the stack.</li>
     *     <li>an empty {@link ColumnPredicate.PredicateType#ALL} if <em>nothing</em> is to be <em>filtered</em>, i.e. take everything.</li>
     *     <li>the effective {@link ColumnPredicate } to filter on</li>
     * </ul>
     * </pre>
     */
    public Set<ColumnPredicate<T>> parseFor(CyodaColumnHandle columnHandle) {
        Set<ColumnPredicate<T>> columnPredicates = parseForInternal(columnHandle);
        if (columnPredicates.size() <= 1) return columnPredicates;
        Optional<ColumnPredicate<T>> allPredicate = columnPredicates.stream()
                .filter(it -> it.getType() == ColumnPredicate.PredicateType.ALL)
                .findAny();
        if (allPredicate.isPresent()) return Collections.singleton(allPredicate.get());

        // remove redundancies
        ImmutableSet.Builder<ColumnPredicate<T>> resultBuilder = ImmutableSet.builder();
        columnPredicates.forEach(tColumnPredicate -> {
            ColumnPredicate.PredicateType type = tColumnPredicate.getType();
            if (type != ColumnPredicate.PredicateType.NONE) {
                resultBuilder.add(tColumnPredicate);
            }
        });
        return resultBuilder.build();

    }

    private Set<ColumnPredicate<T>> parseForInternal(CyodaColumnHandle columnHandle) {

        if (this.conjunctions.isEmpty()) return Collections.singleton(ColumnPredicate.all(columnHandle));

        ImmutableSet.Builder<ColumnPredicate<T>> resultBuilder = ImmutableSet.builder();

        String columnName = columnHandle.getColumnName();

        final Deque<LeafPredicateNode<T>> leafQueue = new ArrayDeque<>();
        final CompoundPredicateNode root = conjunctions.deepCopy();

        removeOtherColumns(columnName, root);

        Deque<CompoundPredicateNode> conjunctionQueue = new ArrayDeque<>();
        Deque<CompoundPredicateNode> disjunctionsQueue = new ArrayDeque<>();

        conjunctionQueue.add(root);
        while (!conjunctionQueue.isEmpty()) {
            CompoundPredicateNode node = conjunctionQueue.pop();
            Collection<ColumnPredicateNode<?>> members = node.getMembers().orElse(Collections.emptyList());
            ImmutableList.Builder<ColumnPredicateNode<?>> newMembersBuilder = ImmutableList.builder();
            members.forEach(member -> {
                if (member instanceof CompoundPredicateNode) {
                    Connective connective = member.getConnective();
                    switch (connective) {
                        case OR:
                            disjunctionsQueue.add((CompoundPredicateNode) member);
                            while (!disjunctionsQueue.isEmpty()) {
                                CompoundPredicateNode disjunct = disjunctionsQueue.pop();
                                Collection<ColumnPredicateNode<?>> disjunctMembers = disjunct.getMembers().orElse(Collections.emptyList());
                                disjunctMembers.forEach(thisNode -> {
                                    if (thisNode instanceof LeafPredicateNode) {
                                        resultBuilder.add(((LeafPredicateNode) thisNode).forceGet());
                                    } else {
                                        Connective thisConnective = thisNode.getConnective();
                                        switch (thisConnective) {
                                            case OR:
                                                disjunctionsQueue.add((CompoundPredicateNode) thisNode);
                                            case AND:
                                                conjunctionQueue.add((CompoundPredicateNode) thisNode);
                                            default: // ignore
                                        }
                                    }
                                });
                            }
                            break;
                        case AND:
                            conjunctionQueue.add((CompoundPredicateNode) member);
                        default: // Ignore
                    }
                } else {
                    newMembersBuilder.add(member);
                }
            });
            ImmutableList<ColumnPredicateNode<?>> newMembers = newMembersBuilder.build();
            if (newMembers.size() > members.size()) {
                node.newMembers(newMembers);
                conjunctionQueue.add(node);
            }
        }


        // If there is nothing left, there is nothing to filter.
        if (root.getMembers().isPresent() && root.getMembers().get().isEmpty()) {
            resultBuilder.add(ColumnPredicate.all(columnHandle));
            return resultBuilder.build();
        }

        Deque<CompoundPredicateNode> nextCompoundQueue = new ArrayDeque<>();

        // Now we have a tree with only conjunctions. Merge them into the leaf queue
        nextCompoundQueue.add(root);
        while (!nextCompoundQueue.isEmpty()) {
            CompoundPredicateNode node = nextCompoundQueue.pop();
            Collection<ColumnPredicateNode<?>> members = node.getMembers().orElse(Collections.emptyList());
            members.forEach(member -> {
                if (member instanceof LeafPredicateNode) {
                    // TODO: To fix, we would need to create a generic CompoundPredicateNode that contains only members of type T.
                    leafQueue.add((LeafPredicateNode<T>) member);
                } else {
                    nextCompoundQueue.add((CompoundPredicateNode) member);
                }
            });
        }

        // If there is nothing left, there is nothing to filter.
        if (leafQueue.isEmpty()) {
            resultBuilder.add(ColumnPredicate.all(columnHandle));
            return resultBuilder.build();
        }

        ColumnPredicate<T> fromDeque = leafQueue.pop().forceGet();

        while (!leafQueue.isEmpty()) {
            LeafPredicateNode<T> predicate = leafQueue.pop();
            fromDeque = fromDeque.merge(predicate.forceGet());
        }
        // If there is nothing left, filter everything.
        if (fromDeque.getType() == ColumnPredicate.PredicateType.NONE) {
            resultBuilder.add(ColumnPredicate.none(columnHandle));
        } else {
            resultBuilder.add(fromDeque);
        }

        return resultBuilder.build();

    }

    private void removeOtherColumns(String columnName, CompoundPredicateNode root) {
        // Remove from the conjunctions all leaf nodes that are not for this columnName.
        final Deque<CompoundPredicateNode> cleanerQueue = new ArrayDeque<>();
        cleanerQueue.add(root);
        while (!cleanerQueue.isEmpty()) {
            CompoundPredicateNode node = cleanerQueue.pop();
            Collection<ColumnPredicateNode<?>> members = node.getMembers().orElse(Collections.emptyList());
            ImmutableList.Builder<ColumnPredicateNode<?>> cleanMembers = ImmutableList.builder();
            members.forEach(member -> {
                if (member instanceof LeafPredicateNode) {
                    if (member.getColumnName().isPresent() && member.getColumnName().get().equals(columnName)) {
                        cleanMembers.add(member);
                    }
                } else {
                    cleanMembers.add(member);
                    cleanerQueue.add((CompoundPredicateNode) member);
                }
            });
            node.newMembers(cleanMembers.build());
        }
    }

    private @Nonnull Stream<T> extractFilterValues(@Nullable ColumnPredicate<T> predicate) {
        if (predicate == null) return Stream.empty();
        ColumnPredicate.PredicateType type = predicate.getType();
        switch (type) {
            case IN_LIST: {
                return predicate.getInListValues().stream();
            }
            case EQUALITY: {
                return Stream.of(predicate.getLower());
            }
            case RANGE: {
                if (predicate.getUpper() == null) {
                    return Stream.of(predicate.getLower());
                } else if (predicate.getLower() == null) {
                    return Stream.of(predicate.getUpper());
                } else {
                    return Stream.of(
                            predicate.getLower(),
                            predicate.getUpper()
                    );
                }
            }
            default:
                throw new TrinoException(
                        CyodaErrorCode.CYODA_PUSHDOWN_UNSUPPORTED_EXPRESSION,
                        type + " is not supported here");
        }
    }
}
