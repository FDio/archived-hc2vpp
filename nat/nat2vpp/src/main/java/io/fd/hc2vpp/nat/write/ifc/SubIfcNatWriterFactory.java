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

package io.fd.hc2vpp.nat.write.ifc;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.nat.future.FutureJVppNatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev170816._interface.nat.attributes.Nat;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev170816._interface.nat.attributes.nat.Inbound;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev170816._interface.nat.attributes.nat.Outbound;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.subinterface.nat.rev170615.NatSubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Nat writers registration for sub-interfaces.
 */
public final class SubIfcNatWriterFactory implements WriterFactory {

    private static final InstanceIdentifier<SubInterface>
            SUB_IFC_ID = InstanceIdentifier.create(Interfaces.class).child(Interface.class).augmentation(
            SubinterfaceAugmentation.class).child(SubInterfaces.class).child(SubInterface.class);
    private static final InstanceIdentifier<Nat> NAT_AUG_ID =
            SUB_IFC_ID.augmentation(NatSubinterfaceAugmentation.class).child(Nat.class);

    private final FutureJVppNatFacade jvppNat;
    private final NamingContext ifcContext;

    @Inject
    public SubIfcNatWriterFactory(final FutureJVppNatFacade jvppNat,
                                  @Named("interface-context") final NamingContext ifcContext) {
        this.jvppNat = jvppNat;
        this.ifcContext = ifcContext;
    }

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        registry.addAfter(new GenericWriter<>(NAT_AUG_ID.child(Inbound.class),
                new SubInterfaceInboundNatCustomizer(jvppNat, ifcContext)), SUB_IFC_ID);
        registry.addAfter(new GenericWriter<>(NAT_AUG_ID.child(Outbound.class),
                new SubInterfaceOutboundNatCustomizer(jvppNat, ifcContext)), SUB_IFC_ID);
    }
}
