/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.v3po.translate.util.read.registry;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class TypeHierarchy {
    private final DirectedAcyclicGraph<InstanceIdentifier<?>, Parent> hierarchy;

    private TypeHierarchy(@Nonnull final DirectedAcyclicGraph<InstanceIdentifier<?>, Parent> hierarchy) {
        this.hierarchy = hierarchy;
    }

    Set<InstanceIdentifier<?>> getAllChildren(InstanceIdentifier<?> id) {
        final HashSet<InstanceIdentifier<?>> instanceIdentifiers = new HashSet<>();
        for (InstanceIdentifier<?> childId : getDirectChildren(id)) {
            instanceIdentifiers.add(childId);
            instanceIdentifiers.addAll(getAllChildren(childId));
        }
        return instanceIdentifiers;
    }

    Set<InstanceIdentifier<?>> getDirectChildren(InstanceIdentifier<?> id) {
        checkArgument(hierarchy.vertexSet().contains(id),
                "Unknown reader: %s. Known readers: %s", id, hierarchy.vertexSet());

        return hierarchy.outgoingEdgesOf(id).stream()
                .map(hierarchy::getEdgeTarget)
                .collect(Collectors.toSet());
    }

    Set<InstanceIdentifier<?>> getRoots() {
        return hierarchy.vertexSet().stream()
                .filter(vertex -> hierarchy.incomingEdgesOf(vertex).size() == 0)
                .collect(Collectors.toSet());
    }

    /**
     * Create reader hierarchy from a flat set of instance identifiers.
     *
     * @param allIds Set of unkeyed instance identifiers
     */
    static TypeHierarchy create(@Nonnull Set<InstanceIdentifier<?>> allIds) {
        final DirectedAcyclicGraph<InstanceIdentifier<?>, Parent>
                readersHierarchy = new DirectedAcyclicGraph<>((sourceVertex, targetVertex) -> new Parent());

        for (InstanceIdentifier<?> allId : allIds) {
            checkArgument(!Iterables.isEmpty(allId.getPathArguments()), "Empty ID detected");

            if (Iterables.size(allId.getPathArguments()) == 1) {
                readersHierarchy.addVertex(allId);
            }

            List<InstanceIdentifier.PathArgument> pathArgs = new LinkedList<>();
            pathArgs.add(allId.getPathArguments().iterator().next());

            for (InstanceIdentifier.PathArgument pathArgument : Iterables.skip(allId.getPathArguments(), 1)) {
                final InstanceIdentifier<?> previous = InstanceIdentifier.create(pathArgs);
                pathArgs.add(pathArgument);
                final InstanceIdentifier<?> current = InstanceIdentifier.create(pathArgs);

                readersHierarchy.addVertex(previous);
                readersHierarchy.addVertex(current);

                try {
                    readersHierarchy.addDagEdge(previous, current);
                } catch (DirectedAcyclicGraph.CycleFoundException e) {
                    throw new IllegalArgumentException("Loop in hierarchy detected", e);
                }
            }
        }

        return new TypeHierarchy(readersHierarchy);
    }

    private static final class Parent{}
}
