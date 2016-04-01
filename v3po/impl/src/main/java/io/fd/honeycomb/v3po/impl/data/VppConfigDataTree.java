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

package io.fd.honeycomb.v3po.impl.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import io.fd.honeycomb.v3po.impl.trans.w.WriteContext;
import io.fd.honeycomb.v3po.impl.trans.w.util.TransactionWriteContext;
import io.fd.honeycomb.v3po.impl.trans.VppApiInvocationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VppDataTree implementation for configuration data.
 */
public final class VppConfigDataTree implements VppDataTree {

    private static final Logger LOG = LoggerFactory.getLogger(VppConfigDataTree.class);

    private final BindingNormalizedNodeSerializer serializer;
    private final DataTree dataTree;
    private final VppWriterRegistry writer;
    public static final ReadableVppDataTree EMPTY_OPERATIONAL = new ReadableVppDataTree() {
        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
                @Nonnull final YangInstanceIdentifier path) {
            return Futures.immediateCheckedFuture(Optional.<NormalizedNode<?, ?>>absent());
        }
    };

    /**
     * Creates configuration data tree instance.
     *
     * @param serializer service for serialization between Java Binding Data representation and NormalizedNode
     *                   representation.
     * @param dataTree   data tree for configuration data representation
     * @param vppWriter  service for translation between Java Binding Data and Vpp.
     */
    public VppConfigDataTree(@Nonnull final BindingNormalizedNodeSerializer serializer,
                             @Nonnull final DataTree dataTree, @Nonnull final VppWriterRegistry vppWriter) {
        this.serializer = checkNotNull(serializer, "serializer should not be null");
        this.dataTree = checkNotNull(dataTree, "dataTree should not be null");
        this.writer = checkNotNull(vppWriter, "vppWriter should not be null");
    }

    @Override
    public VppDataTreeSnapshot takeSnapshot() {
        return new ConfigSnapshot(dataTree.takeSnapshot());
    }

    @Override
    public void commit(final DataTreeModification modification)
            throws DataValidationFailedException, VppApiInvocationException {
        dataTree.validate(modification);

        final DataTreeCandidate candidate = dataTree.prepare(modification);

        final DataTreeCandidateNode rootNode = candidate.getRootNode();
        final YangInstanceIdentifier rootPath = candidate.getRootPath();
        final Optional<NormalizedNode<?, ?>> normalizedDataBefore = rootNode.getDataBefore();
        final Optional<NormalizedNode<?, ?>> normalizedDataAfter = rootNode.getDataAfter();
        LOG.debug("VppConfigDataTree.commit() rootPath={}, rootNode={}, dataBefore={}, dataAfter={}",
                rootPath, rootNode, normalizedDataBefore, normalizedDataAfter);

        final Map<InstanceIdentifier<?>, DataObject> nodesBefore = extractNetconfData(normalizedDataBefore);
        LOG.debug("VppConfigDataTree.commit() extracted nodesBefore={}", nodesBefore.keySet());

        final Map<InstanceIdentifier<?>, DataObject> nodesAfter = extractNetconfData(normalizedDataAfter);
        LOG.debug("VppConfigDataTree.commit() extracted nodesAfter={}", nodesAfter.keySet());


        final DOMDataReadOnlyTransaction beforeTx = new VppReadOnlyTransaction(EMPTY_OPERATIONAL, takeSnapshot());
        final ConfigSnapshot modificationSnapshot = new ConfigSnapshot(modification);
        final DOMDataReadOnlyTransaction afterTx = new VppReadOnlyTransaction(EMPTY_OPERATIONAL, modificationSnapshot);
        final WriteContext ctx = new TransactionWriteContext(serializer, beforeTx, afterTx);

        final ChangesProcessor processor = new ChangesProcessor(writer, nodesBefore, nodesAfter, ctx);
        try {
            processor.applyChanges();
        } catch (VppApiInvocationException e) {
            LOG.warn("Failed to apply changes", e);
            LOG.info("Trying to revert successful changes for current transaction");

            try {
                processor.revertChanges();
                LOG.info("Changes successfully reverted");
            } catch (VppApiInvocationException e2) {
                LOG.error("Failed to revert successful changes", e2);
            }

            // rethrow as we can't do anything more about it
            throw e;
        }


        dataTree.commit(candidate);
    }

    private Map<InstanceIdentifier<?>, DataObject> extractNetconfData(
            final Optional<NormalizedNode<?, ?>> parentOptional) {
        if (parentOptional.isPresent()) {
            final DataContainerNode parent = (DataContainerNode) parentOptional.get();
            return DataTreeUtils.childrenFromNormalized(parent, serializer);
        }
        return Collections.emptyMap();
    }

    private final static class ConfigSnapshot implements VppDataTreeSnapshot {
        private final DataTreeSnapshot snapshot;

        ConfigSnapshot(@Nonnull final DataTreeSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
                @Nonnull final YangInstanceIdentifier path) {
            final Optional<NormalizedNode<?, ?>> node = snapshot.readNode(path);
            if (LOG.isTraceEnabled() && node.isPresent()) {
                LOG.trace("ConfigSnapshot.read: {}", node.get());
            }
            return Futures.immediateCheckedFuture(node);
        }

        @Override
        public DataTreeModification newModification() {
            return snapshot.newModification();
        }
    }

    private static final class ChangesProcessor {
        private final VppWriterRegistry writer;
        private final List<InstanceIdentifier<?>> processedNodes;
        private final Map<InstanceIdentifier<?>, DataObject> nodesBefore;
        private final Map<InstanceIdentifier<?>, DataObject> nodesAfter;
        private final WriteContext ctx;

        ChangesProcessor(@Nonnull final VppWriterRegistry writer,
                         final Map<InstanceIdentifier<?>, DataObject> nodesBefore,
                         final Map<InstanceIdentifier<?>, DataObject> nodesAfter,
                         @Nonnull final WriteContext writeContext) {
            this.ctx = checkNotNull(writeContext, "writeContext is null!");
            this.writer = checkNotNull(writer, "VppWriter is null!");
            this.nodesBefore = checkNotNull(nodesBefore, "nodesBefore is null!");
            this.nodesAfter = checkNotNull(nodesAfter, "nodesAfter is null!");
            processedNodes = new ArrayList<>();
        }

        void applyChanges() throws VppApiInvocationException {
            // TODO we should care about the order of modified subtrees
            // TODO maybe WriterRegistry could provide writeAll method and it will process the updates
            // in order in which it child writers are registered
            final Set<InstanceIdentifier<?>> allNodes = new HashSet<>();
            allNodes.addAll(nodesBefore.keySet());
            allNodes.addAll(nodesAfter.keySet());
            LOG.debug("ChangesProcessor.applyChanges() all extracted nodes: {}", allNodes);

            for (InstanceIdentifier<?> node : allNodes) {
                LOG.debug("ChangesProcessor.applyChanges() processing node={}", node);
                final DataObject dataBefore = nodesBefore.get(node);
                final DataObject dataAfter = nodesAfter.get(node);
                LOG.debug("ChangesProcessor.applyChanges() processing dataBefore={}, dataAfter={}", dataBefore,
                        dataAfter);

                try {
                    // TODO is List as input argument really necessary for writer ?
                    final List<DataObject> dataObjectsBefore = dataBefore == null
                            ? Collections.<DataObject>emptyList()
                            : singletonList(dataBefore);
                    final List<DataObject> dataObjectsAfter = dataAfter == null
                            ? Collections.<DataObject>emptyList()
                            : singletonList(dataAfter);
                    LOG.debug("ChangesProcessor.applyChanges() processing dataObjectsBefore={}, dataObjectsAfter={}",
                            dataObjectsBefore, dataObjectsAfter);
                    writer.update(node, dataObjectsBefore, dataObjectsAfter, ctx);
                    processedNodes.add(node);
                } catch (RuntimeException e) {
                    LOG.error("Error while processing data change (before={}, after={})", dataBefore, dataAfter, e);
                    // FIXME ex handling
                    throw new VppApiInvocationException("", 1, -1);
                }
            }
        }

        void revertChanges() throws VppApiInvocationException {
            checkNotNull(writer, "VppWriter is null!");

            // revert changes in reverse order they were applied
            final ListIterator<InstanceIdentifier<?>> iterator = processedNodes.listIterator(processedNodes.size());

            while (iterator.hasPrevious()) {
                final InstanceIdentifier<?> node = iterator.previous();
                LOG.debug("ChangesProcessor.revertChanges() processing node={}", node);

                final DataObject dataBefore = nodesBefore.get(node);
                final DataObject dataAfter = nodesAfter.get(node);

                // revert a change by invoking writer with reordered arguments
                try {
                    // TODO is List as input argument really necessary for writer ?
                    writer.update(node, singletonList(dataAfter), singletonList(dataBefore), ctx);
                } catch (RuntimeException e) {
                    // FIXME ex handling
                    throw new VppApiInvocationException("", 1, -1);
                }
            }
        }
    }
}



