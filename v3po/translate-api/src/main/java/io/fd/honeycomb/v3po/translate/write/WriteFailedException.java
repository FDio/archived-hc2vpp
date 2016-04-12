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

import io.fd.honeycomb.v3po.translate.TranslationException;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Thrown when a writer or customizer is not able to write/update/delete data .
 */
public class WriteFailedException extends TranslationException {

    private final InstanceIdentifier<?> failedId;

    /**
     * Constructs an WriteFailedException given data id and exception cause.
     *
     * @param failedId instance identifier of the data object that could not be read
     * @param cause    the cause of read failure
     * @param message
     */
    public WriteFailedException(@Nonnull final InstanceIdentifier<?> failedId,
                                @Nonnull final String message,
                                @Nonnull final Throwable cause) {
        super(message, cause);
        this.failedId = checkNotNull(failedId, "failedId should not be null");
    }

    /**
     * Constructs an WriteFailedException given data id.
     *
     * @param failedId instance identifier of the data object that could not be written
     */
    public WriteFailedException(@Nonnull final InstanceIdentifier<?> failedId,
                                @Nonnull final String message) {
        super(message);
        this.failedId = checkNotNull(failedId, "failedId should not be null");
    }

    /**
     * Returns id of the data object that could not be written.
     *
     * @return data object instance identifier
     */
    @Nonnull
    public InstanceIdentifier<?> getFailedId() {
        return failedId;
    }


    /**
     * Delete specific write failed exception
     */
    public static class DeleteFailedException extends WriteFailedException {

        public DeleteFailedException(@Nonnull final InstanceIdentifier<?> failedId, @Nonnull final Throwable cause) {
            super(failedId, getMsg(failedId), cause);
        }

        private static String getMsg(@Nonnull final InstanceIdentifier<?> failedId) {
            return String.format("Failed to delete data at: %s", failedId);
        }

        public DeleteFailedException(@Nonnull final InstanceIdentifier<?> failedId) {
            super(failedId, getMsg(failedId));
        }
    }

    /**
     * Create specific write failed exception
     */
    public static class CreateFailedException extends WriteFailedException {

        private final DataObject data;

        public CreateFailedException(@Nonnull final InstanceIdentifier<?> failedId,
                                     @Nonnull final DataObject data,
                                     @Nonnull final Throwable cause) {
            super(failedId, getMsg(failedId, data), cause);
            this.data = checkNotNull(data, "data");
        }

        private static String getMsg(final @Nonnull InstanceIdentifier<?> failedId, final DataObject data) {
            return String.format("Failed to create data: %s at: %s", data, failedId);
        }

        public CreateFailedException(@Nonnull final InstanceIdentifier<?> failedId,
                                     @Nonnull final DataObject data) {
            super(failedId, getMsg(failedId, data));
            this.data = checkNotNull(data, "data");
        }

        public DataObject getData() {
            return data;
        }
    }

    /**
     * Update specific write failed exception
     */
    public static class UpdateFailedException extends WriteFailedException {

        private final DataObject dataBefore;
        private final DataObject dataAfter;

        public UpdateFailedException(@Nonnull final InstanceIdentifier<?> failedId,
                                     @Nonnull final DataObject dataBefore,
                                     @Nonnull final DataObject dataAfter,
                                     @Nonnull final Throwable cause) {
            super(failedId, getMsg(failedId, dataBefore, dataAfter), cause);
            this.dataBefore = checkNotNull(dataBefore, "dataBefore");
            this.dataAfter = checkNotNull(dataAfter, "dataAfter");
        }

        private static String getMsg(final @Nonnull InstanceIdentifier<?> failedId, final DataObject dataBefore,
                                     final DataObject dataAfter) {
            return String.format("Failed to update data from: %s to: %s, at: %s", dataBefore, dataAfter, failedId);
        }

        public UpdateFailedException(@Nonnull final InstanceIdentifier<?> failedId,
                                     @Nonnull final DataObject dataBefore,
                                     @Nonnull final DataObject dataAfter) {
            super(failedId, getMsg(failedId, dataBefore, dataAfter));
            this.dataBefore = checkNotNull(dataBefore, "dataBefore");
            this.dataAfter = checkNotNull(dataAfter, "dataAfter");
        }

        public DataObject getDataBefore() {
            return dataBefore;
        }

        public DataObject getDataAfter() {
            return dataAfter;
        }
    }
}
