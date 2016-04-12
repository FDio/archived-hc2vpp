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

package io.fd.honeycomb.v3po.translate.write;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.v3po.translate.TranslationException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Special {@link Writer} capable of performing bulk updates
 */
@Beta
public interface WriterRegistry extends Writer<DataObject> {

    /**
     * Performs bulk update
     *
     * @throws BulkUpdateException in case bulk update fails
     * @throws TranslationException        in case some other error occurs while processing update request
     */
    void update(@Nonnull final Map<InstanceIdentifier<?>, DataObject> dataBefore,
                @Nonnull final Map<InstanceIdentifier<?>, DataObject> dataAfter,
                @Nonnull final WriteContext ctx) throws TranslationException;

    /**
     * Thrown when bulk update failed.
     */
    @Beta
    class BulkUpdateException extends WriteFailedException {

        private final Reverter reverter;

        /**
         * Constructs an BulkUpdateException.
         *
         * @param failedId instance identifier of the data object that caused bulk update to fail.
         * @param cause    the cause of bulk update failure
         */
        public BulkUpdateException(@Nonnull final InstanceIdentifier<?> failedId, @Nonnull final Reverter reverter,
                                   final Throwable cause) {
            super(failedId, "Bulk update failed at " + failedId, cause);
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

    }

    /**
     * Abstraction over revert mechanism in case of a bulk update failure
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
            private final List<InstanceIdentifier<?>> notRevertedChanges;

            /**
             * Constructs an RevertFailedException with the list of changes that were not reverted.
             *
             * @param notRevertedChanges list of changes that were not reverted
             * @param cause              the cause of revert failure
             */
            public RevertFailedException(@Nonnull final List<InstanceIdentifier<?>> notRevertedChanges,
                                         final Throwable cause) {
                super(cause);
                checkNotNull(notRevertedChanges, "notRevertedChanges should not be null");
                this.notRevertedChanges = ImmutableList.copyOf(notRevertedChanges);
            }

            /**
             * Returns the list of changes that were not reverted.
             *
             * @return list of changes that were not reverted
             */
            @Nonnull
            public List<InstanceIdentifier<?>> getNotRevertedChanges() {
                return notRevertedChanges;
            }
        }
    }
}