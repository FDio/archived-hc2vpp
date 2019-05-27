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

package io.fd.hc2vpp.nat.read.ifc;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import io.fd.jvpp.nat.types.NatConfigFlags;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev170816.NatInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev170816._interface.nat.attributes.Nat;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev170816._interface.nat.attributes.NatBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev170816._interface.nat.attributes.nat.Inbound;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev170816._interface.nat.attributes.nat.InboundBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InterfaceInboundNatCustomizer extends AbstractInterfaceNatCustomizer<Inbound, InboundBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceInboundNatCustomizer.class);

    InterfaceInboundNatCustomizer(@Nonnull final FutureJVppNatFacade jvppNat,
                                  @Nonnull final NamingContext ifcContext) {
        super(jvppNat, ifcContext, new InboundAttributesBuilder());
    }

    @Override
    protected Logger getLog() {
        return LOG;
    }

    @Override
    boolean isExpectedNatType(final NatConfigFlags flags) {
        return flags.contains(NatConfigFlags.NatConfigFlagsOptions.NAT_IS_INSIDE);
    }

    @Nonnull
    @Override
    public InboundBuilder getBuilder(@Nonnull final InstanceIdentifier<Inbound> id) {
        // Return not present value by default
        return new InboundBuilder();
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final Inbound readValue) {
        ((NatBuilder) parentBuilder).setInbound(readValue);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<Inbound> id,
                                                  @Nonnull final Inbound readValue,
                                                  @Nonnull final ReadContext ctx) {
        final InstanceIdentifier<Inbound> cfgId =
                InstanceIdentifier.create(Interfaces.class)
                        .child(Interface.class,
                                new InterfaceKey(id.firstKeyOf(
                                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface.class)
                                        .getName()))
                        .augmentation(NatInterfaceAugmentation.class)
                        .child(Nat.class)
                        .child(Inbound.class);
        return Initialized.create(cfgId, readValue);
    }
}
