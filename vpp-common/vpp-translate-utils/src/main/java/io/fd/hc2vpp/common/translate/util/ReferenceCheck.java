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

package io.fd.hc2vpp.common.translate.util;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Checks references exist, and throws if present
 */
public interface ReferenceCheck {

    /**
     * Checks if references are present, and throw if so
     *
     * @throws IllegalStateException if any references present
     */
    default <T extends DataObject> void checkReferenceExist(@Nonnull final InstanceIdentifier<?> locSetId,
                                                                  final Collection<T> references) {
        checkState(references == null || references.isEmpty(), "%s is referenced in %s", locSetId, references);
    }
}
