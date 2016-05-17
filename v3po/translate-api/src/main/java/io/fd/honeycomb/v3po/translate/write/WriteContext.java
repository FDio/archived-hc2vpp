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

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.ModificationContext;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Context providing information about current state of DataTree to writers
 */
@Beta
public interface WriteContext extends ModificationContext {

    /**
     * Read any config data object before current modification was applied
     *
     * @param currentId Id of an object to read
     *
     * @return Data before the modification was applied
     */
    <T extends DataObject> Optional<T> readBefore(@Nonnull final InstanceIdentifier<T> currentId);

    /**
     * Read any config data object from current modification
     *
     * @param currentId Id of an object to read
     *
     * @return Data from the modification
     */
    <T extends DataObject> Optional<T> readAfter(@Nonnull final InstanceIdentifier<T> currentId);

}
