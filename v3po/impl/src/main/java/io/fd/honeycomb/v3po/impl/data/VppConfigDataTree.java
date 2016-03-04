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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import io.fd.honeycomb.v3po.impl.trans.VppApiInvocationException;
import io.fd.honeycomb.v3po.impl.trans.VppWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
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
    private final VppWriter writer;

    /**
     * Creates configuration data tree instance.
     *
     * @param serializer service for serialization between Java Binding Data representation and NormalizedNode
     *                   representation.
     * @param dataTree   data tree for configuration data representation
     * @param vppWriter  service for translation between Java Binding Data and Vpp.
     */
    public VppConfigDataTree(@Nonnull final BindingNormalizedNodeSerializer serializer,
                             @Nonnull final DataTree dataTree, @Nonnull final VppWriter vppWriter) {
        this.serializer = Preconditions.checkNotNull(serializer, "serializer should not be null");
        this.dataTree = Preconditions.checkNotNull(dataTree, "dataTree should not be null");
        this.writer = Preconditions.checkNotNull(vppWriter, "vppWriter should not be null");
    }

    @Override
    public VppDataTreeSnapshot takeSnapshot() {
        return new ConfigSnapshot(dataTree.takeSnapshot());
    }

    @Override
    public void commit(final DataTreeModification modification) throws DataValidationFailedException, VppApiInvocationException {
        dataTree.validate(modification);

        final DataTreeCandidate candidate = dataTree.prepare(modification);

        final DataTreeCandidateNode rootNode = candidate.getRootNode();
        final YangInstanceIdentifier rootPath = candidate.getRootPath();
        final Optional<NormalizedNode<?, ?>> normalizedDataBefore = rootNode.getDataBefore();
        final Optional<NormalizedNode<?, ?>> normalizedDataAfter = rootNode.getDataAfter();
        LOG.debug("VppConfigDataProxy.commit() rootPath={}, rootNode={}, dataBefore={}, dataAfter={}",
                rootPath, rootNode, normalizedDataBefore, normalizedDataAfter);

        final Map<InstanceIdentifier<?>, DataObject> nodesBefore = extractNetconfData(normalizedDataBefore);
        LOG.debug("VppConfigDataProxy.commit() extracted nodesBefore={}", nodesBefore.keySet());

        final Map<InstanceIdentifier<?>, DataObject> nodesAfter = extractNetconfData(normalizedDataAfter);
        LOG.debug("VppConfigDataProxy.commit() extracted nodesAfter={}", nodesAfter.keySet());

        final ChangesProcessor processor = new ChangesProcessor(writer, nodesBefore, nodesAfter);
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

    private Map<InstanceIdentifier<?>, DataObject> extractNetconfData(final Optional<NormalizedNode<?, ?>> parentOptional) {
        if (parentOptional.isPresent()) {
            final DataContainerNode parent = (DataContainerNode)parentOptional.get();
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
                final YangInstanceIdentifier path) {
            return Futures.immediateCheckedFuture(snapshot.readNode(path));
        }

        @Override
        public DataTreeModification newModification() {
            return snapshot.newModification();
        }
    }

    private static final class ChangesProcessor {
        private final VppWriter writer;
        private final List<InstanceIdentifier<?>> processedNodes;
        private final Map<InstanceIdentifier<?>, DataObject> nodesBefore;
        private final Map<InstanceIdentifier<?>, DataObject> nodesAfter;

        ChangesProcessor(@Nonnull final VppWriter writer,
                         final Map<InstanceIdentifier<?>, DataObject> nodesBefore,
                         final Map<InstanceIdentifier<?>, DataObject> nodesAfter) {
            this.writer = Preconditions.checkNotNull(writer, "VppWriter is null!");
            this.nodesBefore = Preconditions.checkNotNull(nodesBefore, "nodesBefore is null!");
            this.nodesAfter = Preconditions.checkNotNull(nodesAfter, "nodesAfter is null!");
            processedNodes = new ArrayList<>();
        }

        void applyChanges() throws VppApiInvocationException {
            // TODO we should care about the order of modified subtrees
            final Set<InstanceIdentifier<?>> allNodes = new HashSet<>();
            allNodes.addAll(nodesBefore.keySet());
            allNodes.addAll(nodesAfter.keySet());
            LOG.debug("VppConfigDataProxy.applyChanges() all extracted nodes: {}", allNodes);

            for (InstanceIdentifier<?> node : allNodes) {
                LOG.debug("VppConfigDataProxy.applyChanges() processing node={}", node);
                final DataObject dataBefore = nodesBefore.get(node);
                final DataObject dataAfter = nodesAfter.get(node);

                try {
                    writer.process(dataBefore, dataAfter);
                    processedNodes.add(node);
                } catch (VppApiInvocationException e) {
                    LOG.error("Error while processing data change (before={}, after={})", dataBefore, dataAfter, e);
                    throw e;
                }
            }
        }

        void revertChanges() throws VppApiInvocationException {
            Preconditions.checkNotNull(writer, "VppWriter is nuserializerll!");

            // revert changes in reverse order they were applied
            final ListIterator<InstanceIdentifier<?>> iterator = processedNodes.listIterator(processedNodes.size());

            while (iterator.hasPrevious()) {
                final InstanceIdentifier<?> node = iterator.previous();
                LOG.debug("VppConfigDataProxy.revertChanges() processing node={}", node);

                final DataObject dataBefore = nodesBefore.get(node);
                final DataObject dataAfter = nodesAfter.get(node);

                // revert a change by invoking writer with reordered arguments
                writer.process(dataAfter, dataBefore);
            }
        }
    }
}



