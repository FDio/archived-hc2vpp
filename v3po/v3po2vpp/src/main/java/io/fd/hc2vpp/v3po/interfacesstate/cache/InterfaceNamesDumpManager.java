/*
 * Copyright (c) 2019 Cisco and/or its affiliates.
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


package io.fd.hc2vpp.v3po.interfacesstate.cache;

import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.jvpp.stats.dto.InterfaceName;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Manager for dump data of interface names from stats api. The main purpose of this manager is to cache common
 * interface data between various classes that process this kind of data.
 */
public interface InterfaceNamesDumpManager {

    /**
     * Provides stream of all currently configured vpp interface names
     *
     * @param identifier id of currently processed data
     * @param cache        Modification cache of current transaction
     * @return {@link List} of currently configured interface names
     * @throws ReadFailedException if dumping of data was unsuccessful
     */
    @Nonnull
    List<InterfaceName> getInterfaceNames(@Nonnull final InstanceIdentifier<?> identifier,
                                          @Nonnull final ModificationCache cache)
            throws ReadFailedException;
}
