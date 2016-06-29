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

package io.fd.honeycomb.v3po.translate.util.write.registry;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.write.ModifiableWriterRegistry;
import io.fd.honeycomb.v3po.translate.write.Writer;
import io.fd.honeycomb.v3po.translate.write.WriterRegistryBuilder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder for {@link FlatWriterRegistry} allowing users to specify inter-writer relationships.
 */
@NotThreadSafe
public final class FlatWriterRegistryBuilder implements ModifiableWriterRegistry, WriterRegistryBuilder, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FlatWriterRegistryBuilder.class);

    // Using directed acyclic graph to represent the ordering relationships between writers
    private final DirectedAcyclicGraph<InstanceIdentifier<?>, WriterRelation>
            writersRelations = new DirectedAcyclicGraph<>((sourceVertex, targetVertex) -> new WriterRelation());
    private final Map<InstanceIdentifier<?>, Writer<?>> writersMap = new HashMap<>();

    /**
     * AddWriter without any special relationship to any other type.
     */
    @Override
    public FlatWriterRegistryBuilder addWriter(@Nonnull final Writer<? extends DataObject> writer) {
        // Make IID wildcarded just in case
        // + the way InstanceIdentifier.create + equals work for Identifiable items is unexpected, meaning updates would
        // not be matched to writers in registry
        final InstanceIdentifier<?> targetType = RWUtils.makeIidWildcarded(writer.getManagedDataObjectType());
        checkWriterNotPresentYet(targetType);
        writersRelations.addVertex(targetType);
        writersMap.put(targetType, writer);
        return this;
    }

    /**
     * AddWriter without any special relationship to any other type.
     */
    @Override
    public FlatWriterRegistryBuilder addSubtreeWriter(@Nonnull final Set<InstanceIdentifier<?>> handledChildren,
                                                      @Nonnull final Writer<? extends DataObject> writer) {
        addWriter(SubtreeWriter.createForWriter(handledChildren, writer));
        return this;
    }

    private void checkWriterNotPresentYet(final InstanceIdentifier<?> targetType) {
        Preconditions.checkArgument(!writersMap.containsKey(targetType),
                "Writer for type: %s already present: %s", targetType, writersMap.get(targetType));
    }

    /**
     * Add writer with relationship: to be executed before writer handling relatedType.
     */
    @Override
    public FlatWriterRegistryBuilder addWriterBefore(@Nonnull final Writer<? extends DataObject> writer,
                                                     @Nonnull final InstanceIdentifier<?> relatedType) {
        final InstanceIdentifier<?> targetType = RWUtils.makeIidWildcarded(writer.getManagedDataObjectType());
        final InstanceIdentifier<?> wildcardedRelatedType = RWUtils.makeIidWildcarded(relatedType);
        checkWriterNotPresentYet(targetType);
        writersRelations.addVertex(targetType);
        writersRelations.addVertex(wildcardedRelatedType);
        addEdge(targetType, wildcardedRelatedType);
        writersMap.put(targetType, writer);
        return this;
    }

    @Override
    public FlatWriterRegistryBuilder addSubtreeWriterBefore(@Nonnull final Set<InstanceIdentifier<?>> handledChildren,
                                                            @Nonnull final Writer<? extends DataObject> writer,
                                                            @Nonnull final InstanceIdentifier<?> relatedType) {
        return addWriterBefore(SubtreeWriter.createForWriter(handledChildren, writer), relatedType);
    }

    @Override
    public FlatWriterRegistryBuilder addWriterBefore(@Nonnull final Writer<? extends DataObject> writer,
                                                     @Nonnull final Collection<InstanceIdentifier<?>> relatedTypes) {
        final InstanceIdentifier<?> targetType = RWUtils.makeIidWildcarded(writer.getManagedDataObjectType());
        checkWriterNotPresentYet(targetType);
        writersRelations.addVertex(targetType);
        relatedTypes.stream()
                .map(RWUtils::makeIidWildcarded)
                .forEach(writersRelations::addVertex);
        relatedTypes.stream()
                .map(RWUtils::makeIidWildcarded)
                .forEach(type -> addEdge(targetType, type));
        writersMap.put(targetType, writer);
        return this;
    }

    @Override
    public FlatWriterRegistryBuilder addSubtreeWriterBefore(@Nonnull final Set<InstanceIdentifier<?>> handledChildren,
                                                            @Nonnull final Writer<? extends DataObject> writer,
                                                            @Nonnull final Collection<InstanceIdentifier<?>> relatedTypes) {
        return addWriterBefore(SubtreeWriter.createForWriter(handledChildren, writer), relatedTypes);
    }

    /**
     * Add writer with relationship: to be executed after writer handling relatedType.
     */
    @Override
    public FlatWriterRegistryBuilder addWriterAfter(@Nonnull final Writer<? extends DataObject> writer,
                                                    @Nonnull final InstanceIdentifier<?> relatedType) {
        final InstanceIdentifier<?> targetType = RWUtils.makeIidWildcarded(writer.getManagedDataObjectType());
        final InstanceIdentifier<?> wildcardedRelatedType = RWUtils.makeIidWildcarded(relatedType);
        checkWriterNotPresentYet(targetType);
        writersRelations.addVertex(targetType);
        writersRelations.addVertex(wildcardedRelatedType);
        // set edge to indicate before relationship, just reversed
        addEdge(wildcardedRelatedType, targetType);
        writersMap.put(targetType, writer);
        return this;
    }

    @Override
    public FlatWriterRegistryBuilder addSubtreeWriterAfter(@Nonnull final Set<InstanceIdentifier<?>> handledChildren,
                                                           @Nonnull final Writer<? extends DataObject> writer,
                                                           @Nonnull final InstanceIdentifier<?> relatedType) {
        return addWriterAfter(SubtreeWriter.createForWriter(handledChildren, writer), relatedType);
    }

    @Override
    public FlatWriterRegistryBuilder addWriterAfter(@Nonnull final Writer<? extends DataObject> writer,
                                                    @Nonnull final Collection<InstanceIdentifier<?>> relatedTypes) {
        final InstanceIdentifier<?> targetType = RWUtils.makeIidWildcarded(writer.getManagedDataObjectType());
        checkWriterNotPresentYet(targetType);
        writersRelations.addVertex(targetType);
        relatedTypes.stream()
                .map(RWUtils::makeIidWildcarded)
                .forEach(writersRelations::addVertex);
        // set edge to indicate before relationship, just reversed
        relatedTypes.stream()
                .map(RWUtils::makeIidWildcarded)
                .forEach(type -> addEdge(type, targetType));
        writersMap.put(targetType, writer);
        return this;
    }

    @Override
    public FlatWriterRegistryBuilder addSubtreeWriterAfter(@Nonnull final Set<InstanceIdentifier<?>> handledChildren,
                                                           @Nonnull final Writer<? extends DataObject> writer,
                                                           @Nonnull final Collection<InstanceIdentifier<?>> relatedTypes) {
        return addWriterAfter(SubtreeWriter.createForWriter(handledChildren, writer), relatedTypes);
    }


    private void addEdge(final InstanceIdentifier<?> targetType,
                         final InstanceIdentifier<?> relatedType) {
        try {
            writersRelations.addDagEdge(targetType, relatedType);
        } catch (DirectedAcyclicGraph.CycleFoundException e) {
            throw new IllegalArgumentException(String.format(
                    "Unable to add writer with relation: %s -> %s. Loop detected", targetType, relatedType), e);
        }
    }

    /**
     * Create FlatWriterRegistry with writers ordered according to submitted relationships.
     */
    @Override
    public FlatWriterRegistry build() {
        final ImmutableMap<InstanceIdentifier<?>, Writer<?>> mappedWriters = getMappedWriters();
        LOG.debug("Building writer registry with writers: {}",
                mappedWriters.keySet().stream()
                        .map(InstanceIdentifier::getTargetType)
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", ")));
        LOG.trace("Building writer registry with writers: {}", mappedWriters);
        return new FlatWriterRegistry(mappedWriters);
    }

    @VisibleForTesting
    ImmutableMap<InstanceIdentifier<?>, Writer<?>> getMappedWriters() {
        final ImmutableMap.Builder<InstanceIdentifier<?>, Writer<?>> builder = ImmutableMap.builder();
        // Iterate writer types according to their relationships from graph
        writersRelations.iterator()
                .forEachRemaining(writerType -> {
                    // There might be types stored just for relationship sake, no real writer, ignoring those
                    if (writersMap.containsKey(writerType)) {
                        builder.put(writerType, writersMap.get(writerType));
                    }
                });
        return builder.build();
    }

    @Override
    public void close() throws Exception {
        writersMap.clear();
        writersRelations.removeAllEdges(writersRelations.edgeSet());
        writersRelations.removeAllVertices(writersRelations.vertexSet());
    }

    // Represents edges in graph
    private static final class WriterRelation {}
}
