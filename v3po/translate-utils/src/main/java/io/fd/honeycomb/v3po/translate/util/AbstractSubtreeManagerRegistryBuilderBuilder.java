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

package io.fd.honeycomb.v3po.translate.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.fd.honeycomb.v3po.translate.ModifiableSubtreeManagerRegistryBuilder;
import io.fd.honeycomb.v3po.translate.SubtreeManager;
import io.fd.honeycomb.v3po.translate.SubtreeManagerRegistryBuilder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractSubtreeManagerRegistryBuilderBuilder<S extends SubtreeManager<? extends DataObject>, R>
        implements ModifiableSubtreeManagerRegistryBuilder<S>, SubtreeManagerRegistryBuilder<R>, AutoCloseable {

    // Using directed acyclic graph to represent the ordering relationships between writers
    private final DirectedAcyclicGraph<InstanceIdentifier<?>, Order>
            handlersRelations = new DirectedAcyclicGraph<>((sourceVertex, targetVertex) -> new Order());
    private final Map<InstanceIdentifier<?>, S> handlersMap = new HashMap<>();

    /**
     * Add handler without any special relationship to any other type.
     */
    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> add(@Nonnull final S handler) {
        // Make IID wildcarded just in case
        // + the way InstanceIdentifier.create + equals work for Identifiable items is unexpected, meaning updates would
        // not be matched to writers in registry
        final InstanceIdentifier<?> targetType = RWUtils.makeIidWildcarded(handler.getManagedDataObjectType());
        checkWriterNotPresentYet(targetType);
        handlersRelations.addVertex(targetType);
        handlersMap.put(targetType, handler);
        return this;
    }

    /**
     * Add handler without any special relationship to any other type.
     */
    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> subtreeAdd(@Nonnull final Set<InstanceIdentifier<?>> handledChildren,
                                                                         @Nonnull final S handler) {
        add(getSubtreeHandler(handledChildren, handler));
        return this;
    }

    private void checkWriterNotPresentYet(final InstanceIdentifier<?> targetType) {
        Preconditions.checkArgument(!handlersMap.containsKey(targetType),
                "Writer for type: %s already present: %s", targetType, handlersMap.get(targetType));
    }

    /**
     * Add handler with relationship: to be executed before handler handling relatedType.
     */
    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> addBefore(@Nonnull final S handler,
                                                                        @Nonnull final InstanceIdentifier<?> relatedType) {
        final InstanceIdentifier<?> targetType = RWUtils.makeIidWildcarded(handler.getManagedDataObjectType());
        final InstanceIdentifier<?> wildcardedRelatedType = RWUtils.makeIidWildcarded(relatedType);
        checkWriterNotPresentYet(targetType);
        handlersRelations.addVertex(targetType);
        handlersRelations.addVertex(wildcardedRelatedType);
        addEdge(targetType, wildcardedRelatedType);
        handlersMap.put(targetType, handler);
        return this;
    }

    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> addBefore(@Nonnull final S handler,
                                                                        @Nonnull final Collection<InstanceIdentifier<?>> relatedTypes) {
        final InstanceIdentifier<?> targetType = RWUtils.makeIidWildcarded(handler.getManagedDataObjectType());
        checkWriterNotPresentYet(targetType);
        handlersRelations.addVertex(targetType);
        relatedTypes.stream()
                .map(RWUtils::makeIidWildcarded)
                .forEach(handlersRelations::addVertex);
        relatedTypes.stream()
                .map(RWUtils::makeIidWildcarded)
                .forEach(type -> addEdge(targetType, type));
        handlersMap.put(targetType, handler);
        return this;
    }

    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> subtreeAddBefore(
            @Nonnull final Set<InstanceIdentifier<?>> handledChildren,
            @Nonnull final S handler,
            @Nonnull final InstanceIdentifier<?> relatedType) {
        return addBefore(getSubtreeHandler(handledChildren, handler), relatedType);
    }

    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> subtreeAddBefore(
            @Nonnull final Set<InstanceIdentifier<?>> handledChildren,
            @Nonnull final S handler,
            @Nonnull final Collection<InstanceIdentifier<?>> relatedTypes) {
        return addBefore(getSubtreeHandler(handledChildren, handler), relatedTypes);
    }

    protected abstract S getSubtreeHandler(@Nonnull final Set<InstanceIdentifier<?>> handledChildren,
                                           @Nonnull final S handler);

    /**
     * Add handler with relationship: to be executed after handler handling relatedType.
     */
    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> addAfter(@Nonnull final S handler,
                                                                       @Nonnull final InstanceIdentifier<?> relatedType) {
        final InstanceIdentifier<?> targetType = RWUtils.makeIidWildcarded(handler.getManagedDataObjectType());
        final InstanceIdentifier<?> wildcardedRelatedType = RWUtils.makeIidWildcarded(relatedType);
        checkWriterNotPresentYet(targetType);
        handlersRelations.addVertex(targetType);
        handlersRelations.addVertex(wildcardedRelatedType);
        // set edge to indicate before relationship, just reversed
        addEdge(wildcardedRelatedType, targetType);
        handlersMap.put(targetType, handler);
        return this;
    }

    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> addAfter(@Nonnull final S handler,
                                                                       @Nonnull final Collection<InstanceIdentifier<?>> relatedTypes) {
        final InstanceIdentifier<?> targetType = RWUtils.makeIidWildcarded(handler.getManagedDataObjectType());
        checkWriterNotPresentYet(targetType);
        handlersRelations.addVertex(targetType);
        relatedTypes.stream()
                .map(RWUtils::makeIidWildcarded)
                .forEach(handlersRelations::addVertex);
        // set edge to indicate before relationship, just reversed
        relatedTypes.stream()
                .map(RWUtils::makeIidWildcarded)
                .forEach(type -> addEdge(type, targetType));
        handlersMap.put(targetType, handler);
        return this;
    }

    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> subtreeAddAfter(
            @Nonnull final Set<InstanceIdentifier<?>> handledChildren,
            @Nonnull final S handler,
            @Nonnull final InstanceIdentifier<?> relatedType) {
        return addAfter(getSubtreeHandler(handledChildren, handler), relatedType);
    }

    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> subtreeAddAfter(
            @Nonnull final Set<InstanceIdentifier<?>> handledChildren,
            @Nonnull final S handler,
            @Nonnull final Collection<InstanceIdentifier<?>> relatedTypes) {
        return addAfter(getSubtreeHandler(handledChildren, handler), relatedTypes);
    }


    private void addEdge(final InstanceIdentifier<?> targetType,
                         final InstanceIdentifier<?> relatedType) {
        try {
            handlersRelations.addDagEdge(targetType, relatedType);
        } catch (DirectedAcyclicGraph.CycleFoundException e) {
            throw new IllegalArgumentException(String.format(
                    "Unable to add writer with relation: %s -> %s. Loop detected", targetType, relatedType), e);
        }
    }

    protected ImmutableMap<InstanceIdentifier<?>, S> getMappedHandlers() {
        final ImmutableMap.Builder<InstanceIdentifier<?>, S> builder = ImmutableMap.builder();
        // Iterate writer types according to their relationships from graph
        handlersRelations.iterator()
                .forEachRemaining(writerType -> {
                    // There might be types stored just for relationship sake, no real writer, ignoring those
                    if (handlersMap.containsKey(writerType)) {
                        builder.put(writerType, handlersMap.get(writerType));
                    }
                });

        // TODO we could optimize subtree handlers, if there is a dedicated handler for a node managed by a subtree
        // handler, recreate the subtree handler with a subset of handled child nodes
        // This way it is not necessary to change the configuration of subtree writer, just to add a dedicated child
        // writer. This will be needed if we ever switch to annotations for reader/writer hierarchy initialization

        return builder.build();
    }

    @Override
    public void close() throws Exception {
        handlersMap.clear();
        // Wrap sets into another set to avoid concurrent modification ex in graph
        handlersRelations.removeAllEdges(Sets.newHashSet(handlersRelations.edgeSet()));
        handlersRelations.removeAllVertices(Sets.newHashSet(handlersRelations.vertexSet()));
    }

    // Represents edges in graph
    private class Order {}
}
