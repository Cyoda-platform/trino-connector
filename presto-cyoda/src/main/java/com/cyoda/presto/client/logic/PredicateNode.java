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

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public interface PredicateNode<T extends Comparable<T>> {
    /**
     * Start with real things
     *
     * @param predicates the root node
     * @return the members or if the root node is a leaf, the leaf as a singleton collection
     */
    static Collection<PredicateNode<?>> conjunctions(PredicateNode<Any> predicates) {
        return Optional.ofNullable(predicates).orElse(LeafPredicateNode.nothing(Any.class)).getMembers()
                .orElse(Collections.singletonList(LeafPredicateNode.nothing(Any.class)));
    }

    boolean isLeaf();

    PredicateNodeType getNodeType();

    Optional<Predicate<T>> getPredicate();

    Connective getConnective();

    Optional<Collection<PredicateNode<?>>> getMembers();
}
