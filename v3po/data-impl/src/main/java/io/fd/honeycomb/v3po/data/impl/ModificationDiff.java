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

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MixinNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

/**
 * Recursively collects and provides all unique and non-null modifications (modified normalized nodes).
 */
final class ModificationDiff {

    private static final ModificationDiff EMPTY_DIFF = new ModificationDiff(Collections.emptyMap());
    private static final EnumSet LEAF_MODIFICATIONS = EnumSet.of(ModificationType.WRITE, ModificationType.DELETE);

    private final Map<YangInstanceIdentifier, NormalizedNodeUpdate> updates;

    private ModificationDiff(@Nonnull Map<YangInstanceIdentifier, NormalizedNodeUpdate> updates) {
        this.updates = updates;
    }

    /**
     * Get processed modifications.
     *
     * @return mapped modifications, where key is keyed {@link YangInstanceIdentifier}.
     */
    Map<YangInstanceIdentifier, NormalizedNodeUpdate> getUpdates() {
        return updates;
    }

    private ModificationDiff merge(final ModificationDiff other) {
        if (this == EMPTY_DIFF) {
            return other;
        }

        if (other == EMPTY_DIFF) {
            return this;
        }

        return new ModificationDiff(join(updates, other.updates));
    }

    private static Map<YangInstanceIdentifier, NormalizedNodeUpdate> join(Map<YangInstanceIdentifier, NormalizedNodeUpdate> first,
                                                                          Map<YangInstanceIdentifier, NormalizedNodeUpdate> second) {
        final Map<YangInstanceIdentifier, NormalizedNodeUpdate> merged = new HashMap<>();
        merged.putAll(first);
        merged.putAll(second);
        return merged;
    }

    private static ModificationDiff create(YangInstanceIdentifier id, DataTreeCandidateNode candidate) {
        return new ModificationDiff(ImmutableMap.of(id, NormalizedNodeUpdate.create(id, candidate)));
    }

    /**
     * Produce an aggregated diff from a candidate node recursively. MixinNodes are ignored as modifications and so
     * are complex nodes which direct leaves were not modified.
     */
    @Nonnull
    static ModificationDiff recursivelyFromCandidate(@Nonnull final YangInstanceIdentifier yangIid,
                                                     @Nonnull final DataTreeCandidateNode currentCandidate) {
        // recursively process child nodes for exact modifications
        return recursivelyChildrenFromCandidate(yangIid, currentCandidate)
                // also add modification on current level, if elligible
                .merge(isModification(currentCandidate)
                        ? ModificationDiff.create(yangIid, currentCandidate)
                        : EMPTY_DIFF);
    }

    /**
     * Check whether current node was modified. {@link MixinNode}s are ignored
     * and only nodes which direct leaves(or choices) are modified are considered a modification.
     */
    private static Boolean isModification(@Nonnull final DataTreeCandidateNode currentCandidate) {
        // Mixin nodes are not considered modifications
        if (isMixin(currentCandidate) && !isAugment(currentCandidate)) {
            return false;
        } else {
            return isCurrentModified(currentCandidate);
        }
    }

    private static Boolean isCurrentModified(final @Nonnull DataTreeCandidateNode currentCandidate) {
        // Check if there are any modified leaves and if so, consider current node as modified
        final Boolean directLeavesModified = currentCandidate.getChildNodes().stream()
                .filter(ModificationDiff::isLeaf)
                // For some reason, we get modifications on unmodified list keys TODO debug and report ODL bug
                // and that messes up our modifications collection here, so we need to skip
                .filter(ModificationDiff::isBeforeAndAfterDifferent)
                .filter(child -> LEAF_MODIFICATIONS.contains(child.getModificationType()))
                .findFirst()
                .isPresent();

        return directLeavesModified
                // Also check choices (choices do not exist in BA world and if anything within a choice was modified,
                // consider its parent as being modified)
                || currentCandidate.getChildNodes().stream()
                        .filter(ModificationDiff::isChoice)
                        // Recursively check each choice if there was any change to it
                        .filter(ModificationDiff::isCurrentModified)
                        .findFirst()
                        .isPresent();
    }

    /**
     * Process all non-leaf child nodes recursively, creating aggregated {@link ModificationDiff}.
     */
    private static ModificationDiff recursivelyChildrenFromCandidate(final @Nonnull YangInstanceIdentifier yangIid,
                                                                     final @Nonnull DataTreeCandidateNode currentCandidate) {
        // recursively process child nodes for specific modifications
        return currentCandidate.getChildNodes().stream()
                // not interested in modifications to leaves
                .filter(child -> !isLeaf(child))
                .map(candidate -> recursivelyFromCandidate(yangIid.node(candidate.getIdentifier()), candidate))
                .reduce(ModificationDiff::merge)
                .orElse(EMPTY_DIFF);
    }

    /**
     * Check whether candidate.before and candidate.after is different. If not return false.
     */
    private static boolean isBeforeAndAfterDifferent(@Nonnull final DataTreeCandidateNode candidateNode) {
        if (candidateNode.getDataBefore().isPresent()) {
            return !candidateNode.getDataBefore().get().equals(candidateNode.getDataAfter().orNull());
        }

        // considering not a modification if data after is also null
        return candidateNode.getDataAfter().isPresent();
    }

    /**
     * Check whether candidate node is for a leaf type node.
     */
    private static boolean isLeaf(final DataTreeCandidateNode candidateNode) {
        // orNull intentional, some candidate nodes have both data after and data before null
        return candidateNode.getDataAfter().orNull() instanceof LeafNode<?>
                || candidateNode.getDataBefore().orNull() instanceof LeafNode<?>;
    }

    /**
     * Check whether candidate node is for a Mixin type node.
     */
    private static boolean isMixin(final DataTreeCandidateNode candidateNode) {
        // orNull intentional, some candidate nodes have both data after and data before null
        return candidateNode.getDataAfter().orNull() instanceof MixinNode
                || candidateNode.getDataBefore().orNull() instanceof MixinNode;
    }

    /**
     * Check whether candidate node is for an Augmentation type node.
     */
    private static boolean isAugment(final DataTreeCandidateNode candidateNode) {
        // orNull intentional, some candidate nodes have both data after and data before null
        return candidateNode.getDataAfter().orNull() instanceof AugmentationNode
                || candidateNode.getDataBefore().orNull() instanceof AugmentationNode;
    }

    /**
     * Check whether candidate node is for a Choice type node.
     */
    private static boolean isChoice(final DataTreeCandidateNode candidateNode) {
        // orNull intentional, some candidate nodes have both data after and data before null
        return candidateNode.getDataAfter().orNull() instanceof ChoiceNode
                || candidateNode.getDataBefore().orNull() instanceof ChoiceNode;
    }

    @Override
    public String toString() {
        return "ModificationDiff{updates=" + updates + '}';
    }

    /**
     * Update to a normalized node identifiable by its {@link YangInstanceIdentifier}.
     */
    static final class NormalizedNodeUpdate {

        @Nonnull
        private final YangInstanceIdentifier id;
        @Nullable
        private final NormalizedNode<?, ?> dataBefore;
        @Nullable
        private final NormalizedNode<?, ?> dataAfter;

        private NormalizedNodeUpdate(@Nonnull final YangInstanceIdentifier id,
                                     @Nullable final NormalizedNode<?, ?> dataBefore,
                                     @Nullable final NormalizedNode<?, ?> dataAfter) {
            this.id = checkNotNull(id);
            this.dataAfter = dataAfter;
            this.dataBefore = dataBefore;
        }

        @Nullable
        public NormalizedNode<?, ?> getDataBefore() {
            return dataBefore;
        }

        @Nullable
        public NormalizedNode<?, ?> getDataAfter() {
            return dataAfter;
        }

        @Nonnull
        public YangInstanceIdentifier getId() {
            return id;
        }

        static NormalizedNodeUpdate create(@Nonnull final YangInstanceIdentifier id,
                                           @Nonnull final DataTreeCandidateNode candidate) {
            return create(id, candidate.getDataBefore().orNull(), candidate.getDataAfter().orNull());
        }

        static NormalizedNodeUpdate create(@Nonnull final YangInstanceIdentifier id,
                                           @Nullable final NormalizedNode<?, ?> dataBefore,
                                           @Nullable final NormalizedNode<?, ?> dataAfter) {
            checkArgument(!(dataBefore == null && dataAfter == null), "Both before and after data are null");
            return new NormalizedNodeUpdate(id, dataBefore, dataAfter);
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            final NormalizedNodeUpdate that = (NormalizedNodeUpdate) other;

            return id.equals(that.id);

        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return "NormalizedNodeUpdate{" + "id=" + id
                    + ", dataBefore=" + dataBefore
                    + ", dataAfter=" + dataAfter
                    + '}';
        }
    }

}
