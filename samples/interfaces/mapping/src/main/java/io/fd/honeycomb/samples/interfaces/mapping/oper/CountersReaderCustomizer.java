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
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810._interface.state.Counters;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810._interface.state.CountersBuilder;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.interfaces.state.InterfaceKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a customizer responsible for reading Counters operational data
 */
public class CountersReaderCustomizer implements ReaderCustomizer<Counters, CountersBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(CountersReaderCustomizer.class);
    private final LowerLayerAccess access;

    public CountersReaderCustomizer(final LowerLayerAccess access) {
        this.access = access;
    }


    @Nonnull
    @Override
    public CountersBuilder getBuilder(@Nonnull final InstanceIdentifier<Counters> id) {
        return new CountersBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Counters> id,
                                      @Nonnull final CountersBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        // Get the information about which interface to get counters for
        final InterfaceKey interfaceKey = id.firstKeyOf(Interface.class);
        LOG.info("Reading counters for interface: {} at {}", interfaceKey.getInterfaceId(), id);

        // Set some random data
        builder.setDroppedPackets(access.getDroppedPacketsForIfc(interfaceKey.getInterfaceId().getValue()));
        builder.setTotalPackets(access.getTotalPacketsForInterface(interfaceKey.getInterfaceId().getValue()));
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final Counters readValue) {
        ((InterfaceBuilder) parentBuilder).setCounters(readValue);
    }
}
