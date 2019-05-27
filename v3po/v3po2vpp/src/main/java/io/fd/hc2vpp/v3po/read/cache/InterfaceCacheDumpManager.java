/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.v3po.read.cache;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.jvpp.core.dto.SwInterfaceDetails;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Manager for dump data of interfaces. The main purpose of this manager is to cache common interface data between
 * various classes that process this kind of data. If reader does not use this utility, it introduces a big overhead
 * because of size/complexity of interfaces dump
 */
public interface InterfaceCacheDumpManager {

    /**
     * Provides stream of all currently configured vpp interfaces
     *
     * @param identifier id of currently processed data
     * @param ctx        context of current transaction
     * @return {@link Stream} of currently configured interfaces
     * @throws ReadFailedException if dumping of data was unsuccessful
     */
    @Nonnull
    Stream<SwInterfaceDetails> getInterfaces(@Nonnull final InstanceIdentifier<?> identifier,
                                             @Nonnull final ReadContext ctx) throws ReadFailedException;

    /**
     * Provides details of interface
     *
     * @param identifier    id of currently processed data
     * @param ctx           context of current transaction
     * @param interfaceName name of requested interface
     * @return {@link SwInterfaceDetails} of requested interface
     * @throws ReadFailedException if dumping of data was unsuccessful
     */
    @Nullable
    SwInterfaceDetails getInterfaceDetail(@Nonnull final InstanceIdentifier<?> identifier,
                                          @Nonnull final ReadContext ctx,
                                          @Nonnull final String interfaceName) throws ReadFailedException;
}
