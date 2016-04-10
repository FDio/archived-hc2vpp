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

package io.fd.honeycomb.v3po.data.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.fd.honeycomb.v3po.data.impl.DataTreeUtils.childrenFromNormalized;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import io.fd.honeycomb.v3po.data.DataTreeSnapshot;
import io.fd.honeycomb.v3po.data.ModifiableDataTree;
import io.fd.honeycomb.v3po.data.ReadableDataTree;
import io.fd.honeycomb.v3po.translate.TranslationException;
import io.fd.honeycomb.v3po.translate.util.write.TransactionWriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriterRegistry;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataTree implementation for configuration data.
 */
public final class ConfigDataTree implements ModifiableDataTree {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigDataTree.class);

    private final BindingNormalizedNodeSerializer serializer;
    private final DataTree dataTree;
    private final WriterRegistry writerRegistry;
    public static final ReadableDataTree EMPTY_OPERATIONAL = new ReadableDataTree() {
        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
                @Nonnull final YangInstanceIdentifier path) {
            return Futures.immediateCheckedFuture(Optional.<NormalizedNode<?, ?>>absent());
        }
    };

    /**
     * Creates configuration data tree instance.
     *
     * @param serializer     service for serialization between Java Binding Data representation and NormalizedNode
     *                       representation.
     * @param dataTree       data tree for configuration data representation
     * @param writerRegistry service for translation between Java Binding Data and data provider, capable of performing
     *                       bulk updates.
     */
    public ConfigDataTree(@Nonnull final BindingNormalizedNodeSerializer serializer,
                          @Nonnull final DataTree dataTree, @Nonnull final WriterRegistry writerRegistry) {
        this.serializer = checkNotNull(serializer, "serializer should not be null");
        this.dataTree = checkNotNull(dataTree, "dataTree should not be null");
        this.writerRegistry = checkNotNull(writerRegistry, "writerRegistry should not be null");
    }

    @Override
    public DataTreeSnapshot takeSnapshot() {
        return new ConfigSnapshot(dataTree.takeSnapshot());
    }

    @Override
    public void modify(final DataTreeModification modification)
            throws DataValidationFailedException, TranslationException {
        dataTree.validate(modification);

        final DataTreeCandidate candidate = dataTree.prepare(modification);

        final DataTreeCandidateNode rootNode = candidate.getRootNode();
        final YangInstanceIdentifier rootPath = candidate.getRootPath();
        final Optional<NormalizedNode<?, ?>> normalizedDataBefore = rootNode.getDataBefore();
        final Optional<NormalizedNode<?, ?>> normalizedDataAfter = rootNode.getDataAfter();
        LOG.debug("ConfigDataTree.modify() rootPath={}, rootNode={}, dataBefore={}, dataAfter={}",
                rootPath, rootNode, normalizedDataBefore, normalizedDataAfter);

        final Map<InstanceIdentifier<?>, DataObject> nodesBefore = extractNetconfData(normalizedDataBefore);
        LOG.debug("ConfigDataTree.modify() extracted nodesBefore={}", nodesBefore.keySet());

        final Map<InstanceIdentifier<?>, DataObject> nodesAfter = extractNetconfData(normalizedDataAfter);
        LOG.debug("ConfigDataTree.modify() extracted nodesAfter={}", nodesAfter.keySet());


        final DOMDataReadOnlyTransaction beforeTx = new ReadOnlyTransaction(EMPTY_OPERATIONAL, takeSnapshot());
        final ConfigSnapshot modificationSnapshot = new ConfigSnapshot(modification);
        final DOMDataReadOnlyTransaction afterTx = new ReadOnlyTransaction(EMPTY_OPERATIONAL, modificationSnapshot);
        try (final WriteContext ctx = new TransactionWriteContext(serializer, beforeTx, afterTx)) {
            writerRegistry.update(nodesBefore, nodesAfter, ctx);
        } catch (io.fd.honeycomb.v3po.translate.write.WriterRegistry.BulkUpdateException e) {
            LOG.warn("Failed to apply all changes", e);
            LOG.info("Trying to revert successful changes for current transaction");

            try {
                e.revertChanges();
                LOG.info("Changes successfully reverted");
            } catch (io.fd.honeycomb.v3po.translate.write.WriterRegistry.Reverter.RevertFailedException revertFailedException) {
                // fail with failed revert
                LOG.error("Failed to revert successful changes", revertFailedException);
                throw revertFailedException;
            }

            throw e; // fail with success revert
        } catch (TranslationException e) {
            LOG.error("Error while processing data change (before={}, after={})", nodesBefore, nodesAfter, e);
            throw e;
        }

        dataTree.commit(candidate);
    }

    private Map<InstanceIdentifier<?>, DataObject> extractNetconfData(
            final Optional<NormalizedNode<?, ?>> parentOptional) {
        if (parentOptional.isPresent()) {
            final DataContainerNode parent = (DataContainerNode) parentOptional.get();
            return childrenFromNormalized(parent, serializer);
        }
        return Collections.emptyMap();
    }

    private final static class ConfigSnapshot implements DataTreeSnapshot {
        private final org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot snapshot;

        ConfigSnapshot(@Nonnull final org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot snapshot) {
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
}



