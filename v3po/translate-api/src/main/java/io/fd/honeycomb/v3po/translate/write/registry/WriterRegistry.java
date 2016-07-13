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

package io.fd.honeycomb.v3po.translate.write.registry;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import io.fd.honeycomb.v3po.translate.TranslationException;
import io.fd.honeycomb.v3po.translate.write.DataObjectUpdate;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.Writer;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Special {@link Writer} capable of performing bulk updates.
 */
@Beta
public interface WriterRegistry {

    /**
     * Performs bulk update.
     *
     * @throws BulkUpdateException in case bulk update fails
     * @throws TranslationException in case some other error occurs while processing update request
     */
    void update(@Nonnull DataObjectUpdates updates,
                @Nonnull WriteContext ctx) throws TranslationException;

    /**
     * Simple DTO containing updates for {@link WriterRegistry}. Currently only deletes and updates (create + update)
     * are distinguished.
     */
    @Beta
    final class DataObjectUpdates {

        private final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates;
        private final Multimap<InstanceIdentifier<?>, DataObjectUpdate.DataObjectDelete> deletes;

        /**
         * Create new instance.
         *
         * @param updates All updates indexed by their unkeyed {@link InstanceIdentifier}
         * @param deletes All deletes indexed by their unkeyed {@link InstanceIdentifier}
         */
        public DataObjectUpdates(@Nonnull final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates,
                                 @Nonnull final Multimap<InstanceIdentifier<?>, DataObjectUpdate.DataObjectDelete> deletes) {
            this.deletes = deletes;
            this.updates = updates;
        }

        public Multimap<InstanceIdentifier<?>, DataObjectUpdate> getUpdates() {
            return updates;
        }

        public Multimap<InstanceIdentifier<?>, DataObjectUpdate.DataObjectDelete> getDeletes() {
            return deletes;
        }

        public boolean isEmpty() {
            return updates.isEmpty() && deletes.isEmpty();
        }

        @Override
        public String toString() {
            return "DataObjectUpdates{" + "updates=" + updates + ", deletes=" + deletes + '}';
        }

        /**
         * Get a {@link Set} containing all update types from both updates as well as deletes.
         */
        public Set<InstanceIdentifier<?>> getTypeIntersection() {
            return Sets.union(deletes.keySet(), updates.keySet());
        }

        /**
         * Check whether there is only a single type of data object to be updated.
         *
         * @return true if there is only a single type of updates (update + delete)
         */
        public boolean containsOnlySingleType() {
            return getTypeIntersection().size() == 1;
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            final DataObjectUpdates that = (DataObjectUpdates) other;

            if (!updates.equals(that.updates)) {
                return false;
            }
            return deletes.equals(that.deletes);

        }

        @Override
        public int hashCode() {
            int result = updates.hashCode();
            result = 31 * result + deletes.hashCode();
            return result;
        }

    }

    /**
     * Thrown when bulk update failed.
     */
    @Beta
    class BulkUpdateException extends TranslationException {

        private final Reverter reverter;
        private final Set<InstanceIdentifier<?>> failedIds;

        /**
         * Constructs an BulkUpdateException.
         * @param failedIds instance identifiers of the data objects that were not processed during bulk update.
         * @param cause the cause of bulk update failure
         */
        public BulkUpdateException(@Nonnull final Set<InstanceIdentifier<?>> failedIds,
                                   @Nonnull final Reverter reverter,
                                   @Nonnull final Throwable cause) {
            super("Bulk update failed at: " + failedIds, cause);
            this.failedIds = failedIds;
            this.reverter = checkNotNull(reverter, "reverter should not be null");
        }

        /**
         * Reverts changes that were successfully applied during bulk update before failure occurred.
         *
         * @throws Reverter.RevertFailedException if revert fails
         */
        public void revertChanges() throws Reverter.RevertFailedException {
            reverter.revert();
        }

        public Set<InstanceIdentifier<?>> getFailedIds() {
            return failedIds;
        }
    }

    /**
     * Abstraction over revert mechanism in case of a bulk update failure.
     */
    @Beta
    interface Reverter {

        /**
         * Reverts changes that were successfully applied during bulk update before failure occurred. Changes are
         * reverted in reverse order they were applied.
         *
         * @throws RevertFailedException if not all of applied changes were successfully reverted
         */
        void revert() throws RevertFailedException;

        /**
         * Thrown when some of the changes applied during bulk update were not reverted.
         */
        @Beta
        class RevertFailedException extends TranslationException {

            // TODO change to list of VppDataModifications to make debugging easier
            private final Set<InstanceIdentifier<?>> notRevertedChanges;

            /**
             * Constructs a RevertFailedException with the list of changes that were not reverted.
             *
             * @param notRevertedChanges list of changes that were not reverted
             * @param cause              the cause of revert failure
             */
            public RevertFailedException(@Nonnull final Set<InstanceIdentifier<?>> notRevertedChanges,
                                         final Throwable cause) {
                super(cause);
                checkNotNull(notRevertedChanges, "notRevertedChanges should not be null");
                this.notRevertedChanges = ImmutableSet.copyOf(notRevertedChanges);
            }

            /**
             * Returns the list of changes that were not reverted.
             *
             * @return list of changes that were not reverted
             */
            @Nonnull
            public Set<InstanceIdentifier<?>> getNotRevertedChanges() {
                return notRevertedChanges;
            }
        }
    }
}