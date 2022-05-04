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
import com.cyoda.presto.client.logic.PredicateNodeType;
import com.cyoda.presto.logging.SupplierLogger;
import com.facebook.presto.spi.PrestoException;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PredicateTraversal {

    private static final SupplierLogger LOG = SupplierLogger.get(PredicateTraversal.class);

    private final Collection<PredicateNode<?>> conjunctions;

    private PredicateTraversal(Collection<PredicateNode<?>> conjunctions) {
        this.conjunctions = conjunctions;
    }

    public static PredicateTraversal of(Collection<PredicateNode<?>> conjunctions){
        return new PredicateTraversal(conjunctions);
    }


    /**
     *
     * @param columnName to search filterings for
     * @return empty if everything must be filtered, i.e. an empty result should be passed up the stack
     */
    public Optional<Set<String>> assembleFilterings(String columnName) {

        List<? extends Predicate<?>> leafPredicates = conjunctions.stream()
                .filter(it -> it.getPredicateNodeType() == PredicateNodeType.LEAF)
                .map(it -> (LeafPredicateNode<?>) it)
                .map(LeafPredicateNode::forceGet)
                .filter(it-> it.getColumn().getColumnName().equals(columnName))
                .collect(Collectors.toList());

        // two leaves in a conjunction on a string will always filter everything.
        if (leafPredicates.size() > 1) return Optional.empty();

        Set<String> leafFilterValues = leafPredicates.stream().flatMap(this::extractFilterValues).collect(Collectors.toSet());

        Deque<CompoundPredicateNode> queue = new ArrayDeque<>();
        ImmutableSet.Builder<String> filterValues = ImmutableSet.builder();

        // We only can filter by type, using equals or IN.
        conjunctions.stream()
                //.filter(it -> it.getColumn().isPresent())
                .filter(it->it.getPredicateNodeType() == PredicateNodeType.COMPOUND)
                //.filter(it-> it.getColumn().get().getColumnName().equals(columnName))
                .map(CompoundPredicateNode.class::cast)
                .filter(it->it.getConnective() == Connective.OR)
                .forEach(queue::add);

        while(!queue.isEmpty()) {
            CompoundPredicateNode node = queue.pop();
            // We assume it's an OR compound.
            Collection<PredicateNode<?>> members = node.getMembers().orElse(Collections.emptyList());
            members.forEach(member -> {
                if ( member instanceof LeafPredicateNode ) {
                    if ( member.getColumn().isPresent() && member.getColumn().get().getColumnName().equals(columnName)) {
                        extractFilterValues(((LeafPredicateNode<?>) member).forceGet())
                                .filter(it -> !leafFilterValues.contains(it))
                                .forEach(filterValues::add);
                    }
                } else {
                    queue.add((CompoundPredicateNode) member);
                }
            });
        }
        filterValues.addAll(leafFilterValues);
        ImmutableSet<String> result = filterValues.build();
        LOG.debug(Joiner.on(",").join(result));
        return Optional.of(result);

    }

    private Stream<String> extractFilterValues(Predicate<?> it) {
        Predicate.PredicateType type = it.getType();
        switch (type) {
            case IN_LIST: {
                return it.getInListValues().stream().map(item -> item.value.toString());
            }
            case EQUALITY: {
                return Stream.of(it.getLower().value.toString());
            }
            case RANGE: {
                return Stream.of(it.getLower().toString(), it.getUpper().value.toString());
            }
            default:
                throw new PrestoException(
                        CyodaErrorCode.CYODA_PUSHDOWN_UNSUPPORTED_EXPRESSION,
                        "Only support other things");
        }
    }
}
