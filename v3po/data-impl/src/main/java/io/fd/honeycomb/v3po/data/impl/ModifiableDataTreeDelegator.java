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
import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;
import static io.fd.honeycomb.v3po.data.impl.DataTreeUtils.childrenFromNormalized;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.data.DataModification;
import io.fd.honeycomb.v3po.data.ReadableDataManager;
import io.fd.honeycomb.v3po.translate.TranslationException;
import io.fd.honeycomb.v3po.translate.util.write.TransactionMappingContext;
import io.fd.honeycomb.v3po.translate.util.write.TransactionWriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriterRegistry;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of {@link ModifiableDataTreeManager} that propagates data changes to underlying writer layer before they
 * are fully committed in the backing data tree. Data changes are propagated in BA format.
 */
public final class ModifiableDataTreeDelegator extends ModifiableDataTreeManager {

    private static final Logger LOG = LoggerFactory.getLogger(ModifiableDataTreeDelegator.class);
    private static final ReadableDataManager EMPTY_OPERATIONAL = p -> immediateCheckedFuture(Optional.absent());

    // TODO what to use instead of deprecated BindingNormalizedNodeSerializer ?
    private final BindingNormalizedNodeSerializer serializer;
    private final WriterRegistry writerRegistry;
    private final org.opendaylight.controller.md.sal.binding.api.DataBroker contextBroker;

    /**
     * Creates configuration data tree instance.
     *  @param serializer     service for serialization between Java Binding Data representation and NormalizedNode
     *                       representation.
     * @param dataTree       data tree for configuration data representation
     * @param writerRegistry service for translation between Java Binding Data and data provider, capable of performing
     * @param contextBroker BA broker providing full access to mapping context data
     */
    public ModifiableDataTreeDelegator(@Nonnull final BindingNormalizedNodeSerializer serializer,
                                       @Nonnull final DataTree dataTree,
                                       @Nonnull final WriterRegistry writerRegistry,
                                       @Nonnull final org.opendaylight.controller.md.sal.binding.api.DataBroker contextBroker) {
        super(dataTree);
        this.contextBroker = checkNotNull(contextBroker, "contextBroker should not be null");
        this.serializer = checkNotNull(serializer, "serializer should not be null");
        this.writerRegistry = checkNotNull(writerRegistry, "writerRegistry should not be null");
    }

    @Override
    public DataModification newModification() {
        return new ConfigSnapshot(super.newModification());
    }

    private final class ConfigSnapshot extends ModifiableDataTreeManager.ConfigSnapshot {

        private final DataModification untouchedModification;

        /**
         * @param untouchedModification DataModification captured while this modification/snapshot was created.
         *                              To be used later while invoking writers to provide them with before state
         *                              (state without current modifications).
         *                              It must be captured as close as possible to when current modification started.
         */
        ConfigSnapshot(final DataModification untouchedModification) {
            this.untouchedModification = untouchedModification;
        }

        /**
         * Pass the changes to underlying writer layer.
         * Transform from BI to BA.
         * Revert(Write data before to subtrees that have been successfully modified before failure) in case of failure.
         */
        @Override
        protected void processCandidate(final DataTreeCandidate candidate)
            throws TranslationException {
            final DataTreeCandidateNode rootNode = candidate.getRootNode();
            final YangInstanceIdentifier rootPath = candidate.getRootPath();
            final Optional<NormalizedNode<?, ?>> normalizedDataBefore = rootNode.getDataBefore();
            final Optional<NormalizedNode<?, ?>> normalizedDataAfter = rootNode.getDataAfter();
            LOG.debug("ConfigDataTree.modify() rootPath={}, rootNode={}, dataBefore={}, dataAfter={}",
                rootPath, rootNode, normalizedDataBefore, normalizedDataAfter);

            final Map<InstanceIdentifier<?>, DataObject> nodesBefore = toBindingAware(normalizedDataBefore);
            LOG.debug("ConfigDataTree.modify() extracted nodesBefore={}", nodesBefore.keySet());
            final Map<InstanceIdentifier<?>, DataObject> nodesAfter = toBindingAware(normalizedDataAfter);
            LOG.debug("ConfigDataTree.modify() extracted nodesAfter={}", nodesAfter.keySet());

            try (final WriteContext ctx = getTransactionWriteContext()) {
                writerRegistry.update(nodesBefore, nodesAfter, ctx);

                final CheckedFuture<Void, TransactionCommitFailedException> contextUpdateResult =
                    ((TransactionMappingContext) ctx.getMappingContext()).submit();
                // Blocking on context data update
                contextUpdateResult.checkedGet();

            } catch (WriterRegistry.BulkUpdateException e) {
                LOG.warn("Failed to apply all changes", e);
                LOG.info("Trying to revert successful changes for current transaction");

                try {
                    e.revertChanges();
                    LOG.info("Changes successfully reverted");
                } catch (WriterRegistry.Reverter.RevertFailedException revertFailedException) {
                    // fail with failed revert
                    LOG.error("Failed to revert successful changes", revertFailedException);
                    throw revertFailedException;
                }

                throw e; // fail with success revert
            } catch (TransactionCommitFailedException e) {
                // FIXME revert should probably occur when context is not written successfully
                final String msg = "Error while updating mapping context data";
                LOG.error(msg, e);
                throw new TranslationException(msg, e);
            } catch (TranslationException e) {
                LOG.error("Error while processing data change (before={}, after={})", nodesBefore, nodesAfter, e);
                throw e;
            }
        }

        private TransactionWriteContext getTransactionWriteContext() {
            // Before Tx must use modification
            final DOMDataReadOnlyTransaction beforeTx = ReadOnlyTransaction.create(untouchedModification, EMPTY_OPERATIONAL);
            // After Tx must use current modification
            final DOMDataReadOnlyTransaction afterTx = ReadOnlyTransaction.create(this, EMPTY_OPERATIONAL);
            final TransactionMappingContext mappingContext = new TransactionMappingContext(
                contextBroker.newReadWriteTransaction());
            return new TransactionWriteContext(serializer, beforeTx, afterTx, mappingContext);
        }

        private Map<InstanceIdentifier<?>, DataObject> toBindingAware(final Optional<NormalizedNode<?, ?>> parentOptional) {
            if (parentOptional.isPresent()) {
                final DataContainerNode parent = (DataContainerNode) parentOptional.get();
                return childrenFromNormalized(parent, serializer);
            }
            return Collections.emptyMap();
        }
    }
}



