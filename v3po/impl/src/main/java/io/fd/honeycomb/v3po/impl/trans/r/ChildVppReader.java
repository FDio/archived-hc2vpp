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

package io.fd.honeycomb.v3po.impl.trans.r;

import com.google.common.annotations.Beta;
import io.fd.honeycomb.v3po.impl.trans.ReadFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Child VPP reader allowing its parent to pass the builder object
 *
 * @param <C> Specific DataObject derived type, that is handled by this reader
 */
@Beta
public interface ChildVppReader<C extends DataObject> extends VppReader<C> {

    /**
     * Reads subtree starting from node managed by this reader and place the subtree within parent builder object if the
     * data exists.
     *
     * @param id            Unique identifier pointing to the node managed by this reader. Useful when necessary to
     *                      determine the exact position within more complex subtrees.
     * @param parentBuilder Builder of parent DataObject. Objects read on this level (if any) must be placed into the
     *                      parent builder.
     * @throws ReadFailedException if read was unsuccessful
     */
    void read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
              @Nonnull final Builder<? extends DataObject> parentBuilder) throws ReadFailedException;

}

