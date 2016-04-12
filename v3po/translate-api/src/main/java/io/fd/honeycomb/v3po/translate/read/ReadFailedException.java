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

package io.fd.honeycomb.v3po.translate.read;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.v3po.translate.TranslationException;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Thrown when a reader or customizer is not able to read data for the given id.
 */
public class ReadFailedException extends TranslationException {

    private final InstanceIdentifier<?> failedId;

    /**
     * Constructs an ReadFailedException given data id and exception cause.
     *
     * @param failedId instance identifier of the data object that could not be read
     * @param cause              the cause of read failure
     */
    public ReadFailedException(@Nonnull final InstanceIdentifier<?> failedId, final Throwable cause) {
        super("Failed to read " + failedId, cause);
        this.failedId = checkNotNull(failedId, "failedId should not be null");
    }

    /**
     * Constructs an ReadFailedException given data id.
     *
     * @param failedId instance identifier of the data object that could not be read
     */
    public ReadFailedException(@Nonnull final InstanceIdentifier<?> failedId) {
        this(failedId, null);
    }

    /**
     * Returns id of the data object that could not be read.
     *
     * @return data object instance identifier
     */
    @Nonnull
    public InstanceIdentifier<?> getFailedId() {
        return failedId;
    }
}
