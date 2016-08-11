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

package io.fd.honeycomb.samples.interfaces.mapping.oper;

import io.fd.honeycomb.samples.interfaces.mapping.LowerLayerAccess;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.InterfaceId;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.interfaces.state.InterfaceKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a customizer responsible for reading Interface operational data
 */
public class InterfaceReaderCustomizer implements ListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceReaderCustomizer.class);
    private final LowerLayerAccess access;

    public InterfaceReaderCustomizer(final LowerLayerAccess access) {
        this.access = access;
    }

    @Nonnull
    @Override
    public List<InterfaceKey> getAllIds(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final ReadContext context) throws ReadFailedException {
        // context can be used to access cache (lifetime during a transaction) to store any information for
        // subsequent invocations or for other customizers
        // context.getModificationCache();

        // context can also be used to access context data. Context data are stored in a persistent data store
        // and usually are additional data required to perform the translation in customizers e.g. if underlying layer
        // does not recognize interface-ids as string names, but only indices, the mapping between them can should
        // be stored in the context data store
        // Note: The context datastore is also YANG drive, so context data must be modeled in YANG prior to using them
        // context.getMappingContext();

        // return some sample IDs
        return access.getAllInterfaceNames().stream()
                .map(InterfaceId::new)
                .map(InterfaceKey::new)
                .collect(Collectors.toList());
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<Interface> readData) {
        // Just set the result of this customizers read into parent builder
        // Builder has to be cast properly
        ((InterfacesStateBuilder) builder).setInterface(readData);
    }

    @Nonnull
    @Override
    public InterfaceBuilder getBuilder(@Nonnull final InstanceIdentifier<Interface> id) {
        // Just providing empty builder
        return new InterfaceBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                      @Nonnull final InterfaceBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        // This is where the actual "read" is happening, read attributes for a specific interface
        final InterfaceKey k = id.firstKeyOf(Interface.class);
        final String ifcId = k.getInterfaceId().getValue();
        LOG.info("Reading data for interface: {} at {}", ifcId, id);

        // Fill in some random values, this is actually the place where communication with lower layer
        // would occur to get the real values
        builder.setMtu(access.getMtuForInterface(ifcId));
        builder.setInterfaceId(k.getInterfaceId());
        // Counters container is not set here, instead a dedicated customizer is created for it
        // It could be set here, if this customizer + its reader were marked as subtree reader in the ReaderFactory
        // However its a good practice to provide a dedicated reader+customizer for every complex node
    }

}
