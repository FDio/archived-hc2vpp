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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.data.DataModification;
import io.fd.honeycomb.v3po.data.ReadableDataManager;
import io.fd.honeycomb.v3po.translate.TranslationException;
import io.fd.honeycomb.v3po.translate.util.write.TransactionMappingContext;
import io.fd.honeycomb.v3po.translate.util.write.TransactionWriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriterRegistry;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of {@link ModifiableDataTreeManager} that propagates data changes to underlying writer layer before they
 * are fully committed in the backing data tree. Data changes are propagated in BA format.
 */
public final class ModifiableDataTreeDelegator extends ModifiableDataTreeManager {

    private static final Logger LOG = LoggerFactory.getLogger(ModifiableDataTreeDelegator.class);
    private static final ReadableDataManager EMPTY_OPERATIONAL = p -> immediateCheckedFuture(Optional.absent());

    private final WriterRegistry writerRegistry;
    private final org.opendaylight.controller.md.sal.binding.api.DataBroker contextBroker;
    // TODO what to use instead of deprecated BindingNormalizedNodeSerializer ?
    private final BindingNormalizedNodeSerializer serializer;

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
            LOG.trace("ConfigDataTree.modify() rootPath={}, rootNode={}, dataBefore={}, dataAfter={}",
                rootPath, rootNode, rootNode.getDataBefore(), rootNode.getDataAfter());

            final ModificationDiff modificationDiff =
                    ModificationDiff.recursivelyFromCandidate(YangInstanceIdentifier.EMPTY, rootNode);
            LOG.debug("ConfigDataTree.modify() diff: {}", modificationDiff);

            final Map<InstanceIdentifier<?>, DataObject> nodesBefore = toBindingAware(modificationDiff.getModificationsBefore());
            LOG.debug("ConfigDataTree.modify() extracted nodesBefore={}", nodesBefore.keySet());
            final Map<InstanceIdentifier<?>, DataObject> nodesAfter = toBindingAware(modificationDiff.getModificationsAfter());
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

        private Map<InstanceIdentifier<?>, DataObject> toBindingAware(final Map<YangInstanceIdentifier, NormalizedNode<?, ?>> biNodes) {
            return ModifiableDataTreeDelegator.toBindingAware(biNodes, serializer);
        }
    }

    @VisibleForTesting
    static Map<InstanceIdentifier<?>, DataObject> toBindingAware(final Map<YangInstanceIdentifier, NormalizedNode<?, ?>> biNodes,
                                                                 final BindingNormalizedNodeSerializer serializer) {
        final HashMap<InstanceIdentifier<?>, DataObject> transformed = new HashMap<>(biNodes.size());
        for (Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> biEntry : biNodes.entrySet()) {
            final Map.Entry<InstanceIdentifier<?>, DataObject> baEntry = serializer.fromNormalizedNode(biEntry.getKey(), biEntry.getValue());
            if(baEntry != null) {
                transformed.put(baEntry.getKey(), baEntry.getValue());
            }
        }
        return transformed;
    }

    @VisibleForTesting
    static final class ModificationDiff {

        private static final ModificationDiff EMPTY_DIFF = new ModificationDiff(Collections.emptyMap(), Collections.emptyMap());
        private static final EnumSet LEAF_MODIFICATIONS = EnumSet.of(ModificationType.WRITE, ModificationType.DELETE);

        private final Map<YangInstanceIdentifier, NormalizedNode<?, ?>> modificationsBefore;
        private final Map<YangInstanceIdentifier, NormalizedNode<?, ?>> modificationsAfter;

        private ModificationDiff(@Nonnull final Map<YangInstanceIdentifier, NormalizedNode<?, ?>> modificationsBefore,
                                 @Nonnull final Map<YangInstanceIdentifier, NormalizedNode<?, ?>> modificationsAfter) {
            this.modificationsBefore = modificationsBefore;
            this.modificationsAfter = modificationsAfter;
        }

        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getModificationsBefore() {
            return modificationsBefore;
        }

        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getModificationsAfter() {
            return modificationsAfter;
        }

        private ModificationDiff merge(final ModificationDiff other) {
            if (this == EMPTY_DIFF) {
                return other;
            }

            if (other == EMPTY_DIFF) {
                return this;
            }

            return new ModificationDiff(join(modificationsBefore, other.modificationsBefore),
                    join(modificationsAfter, other.modificationsAfter));
        }

        private static Map<YangInstanceIdentifier, NormalizedNode<?, ?>> join(
                final Map<YangInstanceIdentifier, NormalizedNode<?, ?>> mapOne,
                final Map<YangInstanceIdentifier, NormalizedNode<?, ?>> mapTwo) {
            // Check unique modifications
            // TODO Probably not necessary to check
            final Sets.SetView<YangInstanceIdentifier> duplicates = Sets.intersection(mapOne.keySet(), mapTwo.keySet());
            checkArgument(duplicates.size() == 0, "Duplicates detected: %s. In maps: %s and %s", duplicates, mapOne, mapTwo);
            final HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>> joined = new HashMap<>();
            joined.putAll(mapOne);
            joined.putAll(mapTwo);
            return joined;
        }

        private static ModificationDiff createFromBefore(YangInstanceIdentifier idBefore, DataTreeCandidateNode candidate) {
            return new ModificationDiff(
                    Collections.singletonMap(idBefore, candidate.getDataBefore().get()),
                    Collections.emptyMap());
        }

        private static ModificationDiff create(YangInstanceIdentifier id, DataTreeCandidateNode candidate) {
            return new ModificationDiff(
                    Collections.singletonMap(id, candidate.getDataBefore().get()),
                    Collections.singletonMap(id, candidate.getDataAfter().get()));
        }

        private static ModificationDiff createFromAfter(YangInstanceIdentifier idAfter, DataTreeCandidateNode candidate) {
            return new ModificationDiff(
                    Collections.emptyMap(),
                    Collections.singletonMap(idAfter, candidate.getDataAfter().get()));
        }

        /**
         * Produce a diff from a candidate node recursively
         */
        @Nonnull
        static ModificationDiff recursivelyFromCandidate(@Nonnull final YangInstanceIdentifier yangIid,
                                                         @Nonnull final DataTreeCandidateNode currentCandidate) {
            switch (currentCandidate.getModificationType()) {
                case APPEARED:
                case DISAPPEARED:
                case UNMODIFIED: {
                    // (dis)appeared nodes are not important, no real data to process
                    return ModificationDiff.EMPTY_DIFF;
                }
                case WRITE: {
                    return currentCandidate.getDataBefore().isPresent() ?
                            ModificationDiff.create(yangIid, currentCandidate) :
                            ModificationDiff.createFromAfter(yangIid, currentCandidate);
                    // TODO HONEYCOMB-94 process children recursively to get modifications for child nodes
                }
                case DELETE:
                    return ModificationDiff.createFromBefore(yangIid, currentCandidate);
                case SUBTREE_MODIFIED: {
                    // Modifications here are presented also for leaves. However that kind of granularity is not required
                    // So if there's a modified leaf, mark current complex node also as modification
                    java.util.Optional<Boolean> leavesModified = currentCandidate.getChildNodes().stream()
                            .filter(ModificationDiff::isLeaf)
                            // For some reason, we get modifications on unmodified list keys TODO debug and report ODL bug
                            // and that messes up our modifications collection here, so we need to skip
                            .filter(ModificationDiff::isModification)
                            .map(child -> LEAF_MODIFICATIONS.contains(child.getModificationType()))
                            .reduce((aBoolean, aBoolean2) -> aBoolean || aBoolean2);

                    //
                    if(leavesModified.isPresent() && leavesModified.get()) {
                        return ModificationDiff.create(yangIid, currentCandidate);
                        // TODO HONEYCOMB-94 process children recursively to get modifications for child nodes even if current
                        // was modified
                    } else {
                        // SUBTREE MODIFIED, no modification on current, but process children recursively
                        return currentCandidate.getChildNodes().stream()
                                // not interested in modifications to leaves
                                .filter(child -> !isLeaf(child))
                                .map(candidate -> recursivelyFromCandidate(yangIid.node(candidate.getIdentifier()), candidate))
                                .reduce(ModificationDiff::merge)
                                .orElse(EMPTY_DIFF);
                    }
                }
                default:
                    throw new IllegalStateException("Unknown modification type: "
                            + currentCandidate.getModificationType() + ". Unsupported");
            }
        }

        /**
         * Check whether candidate.before and candidate.after is different. If not
         * return false.
         */
        private static boolean isModification(final DataTreeCandidateNode a) {
            if(a.getDataBefore().isPresent()) {
                if(a.getDataAfter().isPresent()) {
                    return !a.getDataAfter().get().equals(a.getDataBefore().get());
                } else {
                    return true;
                }
            }

            return true;
        }

        /**
         * Check whether candidate node is for a leaf type node
         */
        private static boolean isLeaf(final DataTreeCandidateNode a) {
            return a.getDataAfter().isPresent()
                    ? (a.getDataAfter().get() instanceof LeafNode<?>)
                    : (a.getDataBefore().get() instanceof LeafNode<?>);
        }

        @Override
        public String toString() {
            return "ModificationDiff{" +
                    "modificationsBefore=" + modificationsBefore +
                    ", modificationsAfter=" + modificationsAfter +
                    '}';
        }
    }
}



