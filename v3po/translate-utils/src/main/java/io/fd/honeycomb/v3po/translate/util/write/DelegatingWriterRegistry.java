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

package io.fd.honeycomb.v3po.translate.util.write;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import io.fd.honeycomb.v3po.translate.write.Writer;
import io.fd.honeycomb.v3po.translate.write.WriterRegistry;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple writer registry able to perform and aggregated write (ROOT write) on top of all provided writers. Also able to
 * delegate a specific write to one of the delegate writers.
 *
 * This could serve as a utility to hold & hide all available writers in upper layers.
 */
public final class DelegatingWriterRegistry implements WriterRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(DelegatingWriterRegistry.class);

    private final Map<Class<? extends DataObject>, Writer<? extends DataObject>> rootWriters;

    /**
     * Create new {@link DelegatingWriterRegistry}
     *
     * @param rootWriters List of delegate writers
     */
    public DelegatingWriterRegistry(@Nonnull final List<Writer<? extends DataObject>> rootWriters) {
        this.rootWriters = RWUtils.uniqueLinkedIndex(checkNotNull(rootWriters), RWUtils.MANAGER_CLASS_FUNCTION);
    }

    /**
     * @throws UnsupportedOperationException This getter is not supported for writer registry since it does not manage a
     *                                       specific node type
     */
    @Nonnull
    @Override
    public InstanceIdentifier<DataObject> getManagedDataObjectType() {
        throw new UnsupportedOperationException("Root registry has no type");
    }

    @Override
    public void update(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                       @Nullable final DataObject dataBefore,
                       @Nullable final DataObject dataAfter,
                       @Nonnull final WriteContext ctx) throws WriteFailedException {
        final InstanceIdentifier.PathArgument first = checkNotNull(
                Iterables.getFirst(id.getPathArguments(), null), "Empty id");
        final Writer<? extends DataObject> writer = rootWriters.get(first.getType());
        checkNotNull(writer,
                "Unable to write %s. Missing writer. Current writers for: %s", id, rootWriters.keySet());
        writer.update(id, dataBefore, dataAfter, ctx);
    }

    @Override
    public void update(@Nonnull final Map<InstanceIdentifier<?>, DataObject> nodesBefore,
                       @Nonnull final Map<InstanceIdentifier<?>, DataObject> nodesAfter,
                       @Nonnull final WriteContext ctx) throws WriteFailedException {

        Multimap<InstanceIdentifier<?>, InstanceIdentifier<?>> rootIdToNestedIds = HashMultimap.create();
        try {
            checkAllWritersPresent(nodesBefore, rootIdToNestedIds);
            checkAllWritersPresent(nodesAfter, rootIdToNestedIds);
        } catch (IllegalArgumentException e) {
            LOG.warn("Unable to process update", e);
            throw e;
        }

        final List<InstanceIdentifier<?>> processedNodes = Lists.newArrayList();

        for (Map.Entry<Class<? extends DataObject>, Writer<? extends DataObject>> rootWriterEntry : rootWriters
                .entrySet()) {

            final InstanceIdentifier<? extends DataObject> id = rootWriterEntry.getValue().getManagedDataObjectType();
            // FIXME !! this is not ideal, we are not handling nested updates in expected order
            // Root writers are invoked in order they were registered, but nested updates are not, since they are
            // iterated here.
            //
            for (InstanceIdentifier<?> specificInstanceIdentifier : rootIdToNestedIds.get(id)) {
                final DataObject dataBefore = nodesBefore.get(specificInstanceIdentifier);
                final DataObject dataAfter = nodesAfter.get(specificInstanceIdentifier);

                // No change to current writer
                if (dataBefore == null && dataAfter == null) {
                    continue;
                }

                try {
                    LOG.debug("ChangesProcessor.applyChanges() processing dataBefore={}, dataAfter={}", dataBefore, dataAfter);
                    update(specificInstanceIdentifier, dataBefore, dataAfter, ctx);
                    processedNodes.add(id);
                } catch (Exception e) {
                    LOG.error("Error while processing data change of: {} (before={}, after={})", id, dataBefore, dataAfter, e);
                    throw new BulkUpdateException(id, new ReverterImpl(this, processedNodes, nodesBefore, nodesAfter, ctx), e);
                }
            }
        }
    }

    private void checkAllWritersPresent(final @Nonnull Map<InstanceIdentifier<?>, DataObject> nodesBefore,
                                        final @Nonnull Multimap<InstanceIdentifier<?>, InstanceIdentifier<?>> rootIdToNestedIds) {
        for (final InstanceIdentifier<?> changeId : nodesBefore.keySet()) {
            final InstanceIdentifier.PathArgument first = Iterables.getFirst(changeId.getPathArguments(), null);
            checkNotNull(first, "Empty identifier detected");
            final InstanceIdentifier<? extends DataObject> rootId = InstanceIdentifier.create(first.getType());
            checkArgument(rootWriters.keySet().contains(first.getType()),
                    "Unable to handle change. Missing dedicated writer for: %s", first.getType());
            rootIdToNestedIds.put(rootId, changeId);
        }
    }

    private static final class ReverterImpl implements Reverter {
        private final WriterRegistry delegatingWriterRegistry;
        private final List<InstanceIdentifier<?>> processedNodes;
        private final Map<InstanceIdentifier<?>, DataObject> nodesBefore;
        private final Map<InstanceIdentifier<?>, DataObject> nodesAfter;
        private final WriteContext ctx;

        ReverterImpl(final WriterRegistry delegatingWriterRegistry,
                            final List<InstanceIdentifier<?>> processedNodes,
                            final Map<InstanceIdentifier<?>, DataObject> nodesBefore,
                            final Map<InstanceIdentifier<?>, DataObject> nodesAfter, final WriteContext ctx) {
            this.delegatingWriterRegistry = delegatingWriterRegistry;
            this.processedNodes = processedNodes;
            this.nodesBefore = nodesBefore;
            this.nodesAfter = nodesAfter;
            this.ctx = ctx;
        }

        @Override
        public void revert() throws RevertFailedException {
            final LinkedList<InstanceIdentifier<?>> notReverted = new LinkedList<>(processedNodes);

            while (notReverted.size() > 0) {
                final InstanceIdentifier<?> node = notReverted.peekLast();
                LOG.debug("ChangesProcessor.revertChanges() processing node={}", node);

                final DataObject dataBefore = nodesBefore.get(node);
                final DataObject dataAfter = nodesAfter.get(node);

                // revert a change by invoking writer with reordered arguments
                try {
                    delegatingWriterRegistry.update(node, dataAfter, dataBefore, ctx);
                    notReverted.removeLast(); // change was successfully reverted
                } catch (Exception e) {
                    throw new RevertFailedException(notReverted, e);
                }

            }
        }
    }
}
