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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import io.fd.honeycomb.v3po.translate.TranslationException;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.write.DataObjectUpdate;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import io.fd.honeycomb.v3po.translate.write.Writer;
import io.fd.honeycomb.v3po.translate.write.WriterRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flat writer registry, delegating updates to writers in the order writers were submitted.
 */
@ThreadSafe
final class FlatWriterRegistry implements WriterRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(FlatWriterRegistry.class);

    // All types handled by writers directly or as children
    private final ImmutableSet<InstanceIdentifier<?>> handledTypes;

    private final Set<InstanceIdentifier<?>> writersOrderReversed;
    private final Set<InstanceIdentifier<?>> writersOrder;
    private final Map<InstanceIdentifier<?>, Writer<?>> writers;

    /**
     * Create flat registry instance.
     *
     * @param writers immutable, ordered map of writers to use to process updates. Order of the writers has to be
     *                one in which create and update operations should be handled. Deletes will be handled in reversed
     *                order. All deletes are handled before handling all the updates.
     */
    FlatWriterRegistry(@Nonnull final ImmutableMap<InstanceIdentifier<?>, Writer<?>> writers) {
        this.writers = writers;
        this.writersOrderReversed = Sets.newLinkedHashSet(Lists.reverse(Lists.newArrayList(writers.keySet())));
        this.writersOrder = writers.keySet();
        this.handledTypes = getAllHandledTypes(writers);
    }

    private static ImmutableSet<InstanceIdentifier<?>> getAllHandledTypes(
            @Nonnull final ImmutableMap<InstanceIdentifier<?>, Writer<?>> writers) {
        final ImmutableSet.Builder<InstanceIdentifier<?>> handledTypesBuilder = ImmutableSet.builder();
        for (Map.Entry<InstanceIdentifier<?>, Writer<?>> writerEntry : writers.entrySet()) {
            final InstanceIdentifier<?> writerType = writerEntry.getKey();
            final Writer<?> writer = writerEntry.getValue();
            handledTypesBuilder.add(writerType);
            if (writer instanceof SubtreeWriter) {
                handledTypesBuilder.addAll(((SubtreeWriter<?>) writer).getHandledChildTypes());
            }
        }
        return handledTypesBuilder.build();
    }

    @Override
    public void update(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                       @Nullable final DataObject dataBefore,
                       @Nullable final DataObject dataAfter,
                       @Nonnull final WriteContext ctx) throws WriteFailedException {
        singleUpdate(ImmutableMultimap.of(
                RWUtils.makeIidWildcarded(id), DataObjectUpdate.create(id, dataBefore, dataAfter)), ctx);
    }

    @Override
    public void update(@Nonnull final DataObjectUpdates updates,
                       @Nonnull final WriteContext ctx) throws TranslationException {
        if (updates.isEmpty()) {
            return;
        }

        // Optimization
        if (updates.containsOnlySingleType()) {
            // First process delete
            singleUpdate(updates.getDeletes(), ctx);
            // Next is update
            singleUpdate(updates.getUpdates(), ctx);
        } else {
            // First process deletes
            bulkUpdate(updates.getDeletes(), ctx, true, writersOrderReversed);
            // Next are updates
            bulkUpdate(updates.getUpdates(), ctx, true, writersOrder);
        }

        LOG.debug("Update successful for types: {}", updates.getTypeIntersection());
        LOG.trace("Update successful for: {}", updates);
    }

    private void singleUpdate(@Nonnull final Multimap<InstanceIdentifier<?>, ? extends DataObjectUpdate> updates,
                              @Nonnull final WriteContext ctx) throws WriteFailedException {
        if (updates.isEmpty()) {
            return;
        }

        final InstanceIdentifier<?> singleType = updates.keySet().iterator().next();
        LOG.debug("Performing single type update for: {}", singleType);
        Collection<? extends DataObjectUpdate> singleTypeUpdates = updates.get(singleType);
        Writer<?> writer = getWriter(singleType);

        if (writer == null) {
            // This node must be handled by a subtree writer, find it and call it or else fail
            checkArgument(handledTypes.contains(singleType), "Unable to process update. Missing writers for: %s",
                    singleType);
            writer = getSubtreeWriterResponsible(singleType);
            singleTypeUpdates = getParentDataObjectUpdate(ctx, updates, writer);
        }

        LOG.trace("Performing single type update with writer: {}", writer);
        for (DataObjectUpdate singleUpdate : singleTypeUpdates) {
            writer.update(singleUpdate.getId(), singleUpdate.getDataBefore(), singleUpdate.getDataAfter(), ctx);
        }
    }

    private Writer<?> getSubtreeWriterResponsible(final InstanceIdentifier<?> singleType) {
        final Writer<?> writer;// This is slow ( minor TODO-perf )
        writer = writers.values().stream()
                .filter(w -> w instanceof SubtreeWriter)
                .filter(w -> ((SubtreeWriter<?>) w).getHandledChildTypes().contains(singleType))
                .findFirst()
                .get();
        return writer;
    }

    private Collection<DataObjectUpdate> getParentDataObjectUpdate(final WriteContext ctx,
                                                                   final Multimap<InstanceIdentifier<?>, ? extends DataObjectUpdate> updates,
                                                                   final Writer<?> writer) {
        // Now read data for subtree reader root, but first keyed ID is needed and that ID can be cut from updates
        InstanceIdentifier<?> firstAffectedChildId = ((SubtreeWriter<?>) writer).getHandledChildTypes().stream()
                .filter(updates::containsKey)
                .map(unkeyedId -> updates.get(unkeyedId))
                .flatMap(doUpdates -> doUpdates.stream())
                .map(DataObjectUpdate::getId)
                .findFirst()
                .get();

        final InstanceIdentifier<?> parentKeyedId =
                RWUtils.cutId(firstAffectedChildId, writer.getManagedDataObjectType());

        final Optional<? extends DataObject> parentBefore = ctx.readBefore(parentKeyedId);
        final Optional<? extends DataObject> parentAfter = ctx.readAfter(parentKeyedId);
        return Collections.singleton(
                DataObjectUpdate.create(parentKeyedId, parentBefore.orNull(), parentAfter.orNull()));
    }

    private void bulkUpdate(@Nonnull final Multimap<InstanceIdentifier<?>, ? extends DataObjectUpdate> updates,
                            @Nonnull final WriteContext ctx,
                            final boolean attemptRevert,
                            @Nonnull final Set<InstanceIdentifier<?>> writersOrder) throws BulkUpdateException {
        if (updates.isEmpty()) {
            return;
        }

        LOG.debug("Performing bulk update with revert attempt: {} for: {}", attemptRevert, updates.keySet());

        // Check that all updates can be handled
        checkAllTypesCanBeHandled(updates);

        // Capture all changes successfully processed in case revert is needed
        final Set<InstanceIdentifier<?>> processedNodes = new HashSet<>();

        // Iterate over all writers and call update if there are any related updates
        for (InstanceIdentifier<?> writerType : writersOrder) {
            Collection<? extends DataObjectUpdate> writersData = updates.get(writerType);
            final Writer<?> writer = getWriter(writerType);

            if (writersData.isEmpty()) {
                // If there are no data for current writer, but it is a SubtreeWriter and there are updates to
                // its children, still invoke it with its root data
                if (writer instanceof SubtreeWriter<?> && isAffected(((SubtreeWriter<?>) writer), updates)) {
                    // Provide parent data for SubtreeWriter for further processing
                    writersData = getParentDataObjectUpdate(ctx, updates, writer);
                } else {
                    // Skipping unaffected writer
                    // Alternative to this would be modification sort according to the order of writers
                    continue;
                }
            }

            LOG.debug("Performing update for: {}",  writerType);
            LOG.trace("Performing update with writer: {}", writer);

            for (DataObjectUpdate singleUpdate : writersData) {
                try {
                    writer.update(singleUpdate.getId(), singleUpdate.getDataBefore(), singleUpdate.getDataAfter(), ctx);
                    processedNodes.add(singleUpdate.getId());
                    LOG.trace("Update successful for type: {}", writerType);
                    LOG.debug("Update successful for: {}", singleUpdate);
                } catch (Exception e) {
                    LOG.error("Error while processing data change of: {} (updates={})", writerType, writersData, e);

                    final Reverter reverter = attemptRevert
                            ? new ReverterImpl(processedNodes, updates, writersOrder, ctx)
                            : () -> {}; // NOOP reverter

                    // Find out which changes left unprocessed
                    final Set<InstanceIdentifier<?>> unprocessedChanges = updates.values().stream()
                            .map(DataObjectUpdate::getId)
                            .filter(id -> !processedNodes.contains(id))
                            .collect(Collectors.toSet());
                    throw new BulkUpdateException(unprocessedChanges, reverter, e);
                }
            }
        }
    }

    private void checkAllTypesCanBeHandled(
            @Nonnull final Multimap<InstanceIdentifier<?>, ? extends DataObjectUpdate> updates) {
        if (!handledTypes.containsAll(updates.keySet())) {
            final Sets.SetView<InstanceIdentifier<?>> missingWriters = Sets.difference(updates.keySet(), handledTypes);
            LOG.warn("Unable to process update. Missing writers for: {}", missingWriters);
            throw new IllegalArgumentException("Unable to process update. Missing writers for: " + missingWriters);
        }
    }

    /**
     * Check whether {@link SubtreeWriter} is affected by the updates.
     *
     * @return true if there are any updates to SubtreeWriter's child nodes (those marked by SubtreeWriter
     *         as being taken care of)
     * */
    private static boolean isAffected(final SubtreeWriter<?> writer,
                               final Multimap<InstanceIdentifier<?>, ? extends DataObjectUpdate> updates) {
        return !Sets.intersection(writer.getHandledChildTypes(), updates.keySet()).isEmpty();
    }

    private Writer<?> getWriter(@Nonnull final InstanceIdentifier<?> singleType) {
        final Writer<?> writer = writers.get(singleType);
        checkNotNull(writer,
                "Unable to write %s. Missing writer. Current writers for: %s", singleType, writers.keySet());
        return writer;
    }

    @Nonnull
    @Override
    public InstanceIdentifier<DataObject> getManagedDataObjectType() {
        throw new UnsupportedOperationException("Registry has no managed type");
    }

    // FIXME unit test
    private final class ReverterImpl implements Reverter {

        private final Collection<InstanceIdentifier<?>> processedNodes;
        private final Multimap<InstanceIdentifier<?>, ? extends DataObjectUpdate> updates;
        private final Set<InstanceIdentifier<?>> revertDeleteOrder;
        private final WriteContext ctx;

        ReverterImpl(final Collection<InstanceIdentifier<?>> processedNodes,
                     final Multimap<InstanceIdentifier<?>, ? extends DataObjectUpdate> updates,
                     final Set<InstanceIdentifier<?>> writersOrderOriginal,
                     final WriteContext ctx) {
            this.processedNodes = processedNodes;
            this.updates = updates;
            // Use opposite ordering when executing revert
            this.revertDeleteOrder =  writersOrderOriginal == FlatWriterRegistry.this.writersOrder
                    ? FlatWriterRegistry.this.writersOrderReversed
                    : FlatWriterRegistry.this.writersOrder;
            this.ctx = ctx;
        }

        @Override
        public void revert() throws RevertFailedException {
            Multimap<InstanceIdentifier<?>, DataObjectUpdate> updatesToRevert =
                    filterAndRevertProcessed(updates, processedNodes);

            LOG.info("Attempting revert for changes: {}", updatesToRevert);
            try {
                // Perform reversed bulk update without revert attempt
                bulkUpdate(updatesToRevert, ctx, true, revertDeleteOrder);
                LOG.info("Revert successful");
            } catch (BulkUpdateException e) {
                LOG.error("Revert failed", e);
                throw new RevertFailedException(e.getFailedIds(), e);
            }
        }

        /**
         * Create new updates map, but only keep already processed changes. Switching before and after data for each
         * update.
         */
        private Multimap<InstanceIdentifier<?>, DataObjectUpdate> filterAndRevertProcessed(
                final Multimap<InstanceIdentifier<?>, ? extends DataObjectUpdate> updates,
                final Collection<InstanceIdentifier<?>> processedNodes) {
            final Multimap<InstanceIdentifier<?>, DataObjectUpdate> filtered = HashMultimap.create();
            for (InstanceIdentifier<?> processedNode : processedNodes) {
                final InstanceIdentifier<?> wildcardedIid = RWUtils.makeIidWildcarded(processedNode);
                if (updates.containsKey(wildcardedIid)) {
                    updates.get(wildcardedIid).stream()
                            .filter(dataObjectUpdate -> processedNode.contains(dataObjectUpdate.getId()))
                            .forEach(dataObjectUpdate -> filtered.put(processedNode, dataObjectUpdate.reverse()));
                }
            }
            return filtered;
        }
    }

}
