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

package io.fd.hc2vpp.nat.read.ifc;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.impl.read.GenericInitReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.vpp.jvpp.nat.future.FutureJVppNatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816._interface.nat.attributes.Nat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816._interface.nat.attributes.NatBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816._interface.nat.attributes.nat.Inbound;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816._interface.nat.attributes.nat.Outbound;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.subinterface.nat.rev170615.NatSubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.subinterface.nat.rev170615.NatSubinterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Nat readers registration for sub-interfaces.
 */
public final class SubIfcNatReaderFactory implements ReaderFactory {

    private static final InstanceIdentifier<SubInterface>
            SUB_IFC_ID = InstanceIdentifier.create(InterfacesState.class).child(Interface.class).augmentation(
            SubinterfaceStateAugmentation.class).child(SubInterfaces.class).child(SubInterface.class);
    private static final InstanceIdentifier<NatSubinterfaceStateAugmentation> NAT_SUB_AUG_ID =
            SUB_IFC_ID.augmentation(NatSubinterfaceStateAugmentation.class);
    private static final InstanceIdentifier<Nat> NAT_AUG_CONTAINER_ID = NAT_SUB_AUG_ID.child(Nat.class);

    private final NamingContext ifcContext;
    private final FutureJVppNatFacade jvppNat;

    @Inject
    public SubIfcNatReaderFactory(final FutureJVppNatFacade jvppNat,
                                  @Named("interface-context") final NamingContext ifcContext) {
        this.jvppNat = jvppNat;
        this.ifcContext = ifcContext;
    }

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        registry.addStructuralReader(NAT_SUB_AUG_ID, NatSubinterfaceStateAugmentationBuilder.class);
        registry.addStructuralReader(NAT_AUG_CONTAINER_ID, NatBuilder.class);

        registry.addAfter(new GenericInitReader<>(NAT_AUG_CONTAINER_ID.child(Inbound.class),
                new SubInterfaceInboundNatCustomizer(jvppNat, ifcContext)), SUB_IFC_ID);
        registry.addAfter(new GenericInitReader<>(NAT_AUG_CONTAINER_ID.child(Outbound.class),
                new SubInterfaceOutboundNatCustomizer(jvppNat, ifcContext)), SUB_IFC_ID);
    }
}
