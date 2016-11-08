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

package io.fd.honeycomb.nat.read.ifc;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceDetails;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceDetailsReplyDump;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214.NatInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.Nat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.NatBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.Outbound;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.OutboundBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InterfaceOutboundNatCustomizer extends AbstractInterfaceNatCustomizer<Outbound, OutboundBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceOutboundNatCustomizer.class);

    InterfaceOutboundNatCustomizer(
            @Nonnull final DumpCacheManager<SnatInterfaceDetailsReplyDump, Void> dumpMgr,
            @Nonnull final NamingContext ifcContext) {
        super(dumpMgr, ifcContext);
    }

    @Override
    protected Logger getLog() {
        return LOG;
    }

    @Override
    boolean isExpectedNatType(final SnatInterfaceDetails snatInterfaceDetails) {
        return snatInterfaceDetails.isInside == 0;
    }

    @Nonnull
    @Override
    public OutboundBuilder getBuilder(@Nonnull final InstanceIdentifier<Outbound> id) {
        return new OutboundBuilder();
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final Outbound readValue) {
        ((NatBuilder) parentBuilder).setOutbound(readValue);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<Outbound> id,
                                                  @Nonnull final Outbound readValue,
                                                  @Nonnull final ReadContext ctx) {
        final InstanceIdentifier<Outbound> cfgId =
                InstanceIdentifier.create(Interfaces.class)
                        .child(Interface.class,
                                new InterfaceKey(id.firstKeyOf(
                                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.class).getName()))
                        .augmentation(NatInterfaceAugmentation.class)
                        .child(Nat.class)
                        .child(Outbound.class);
        return Initialized.create(cfgId, readValue);
    }
}
