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
import com.google.common.collect.Multimap;
import io.fd.honeycomb.v3po.impl.trans.ReadFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Simple delegating reader suitable as a holder for all other root readers, providing readAll feature.
 */
@Beta
public interface ReaderRegistry extends VppReader<DataObject> {

    /**
     * Performs read on all registered root readers and merges the results into a Multimap. Keys represent identifiers
     * for root DataObjects from the data tree modeled by YANG.
     *
     * @param ctx Read context
     *
     * @return multimap that preserves deterministic iteration order across non-distinct key values
     * @throws ReadFailedException if read was unsuccessful
     */
    @Nonnull
    Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> readAll(@Nonnull final ReadContext ctx)
            throws ReadFailedException;
}
