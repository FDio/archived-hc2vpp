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

package io.fd.hc2vpp.l3.read.factory;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.l3.read.ipv6.subinterface.SubInterfaceIpv6AddressCustomizer;
import io.fd.hc2vpp.l3.read.ipv6.subinterface.SubInterfaceIpv6NeighbourCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.Ipv6;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.Ipv6Builder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.ipv6.Address;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.ipv6.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceStateIpv6ReaderFactory implements ReaderFactory {

    private static final InstanceIdentifier<Interface> IFC_ID =
            InstanceIdentifier.create(InterfacesState.class).child(Interface.class);

    @Inject
    private FutureJVppCore jvpp;

    @Inject
    @Named("interface-context")
    private NamingContext ifcNamingContext;

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {

        final InstanceIdentifier<SubInterface> subIfcId =
                IFC_ID.augmentation(SubinterfaceStateAugmentation.class)
                        .child(SubInterfaces.class).child(SubInterface.class);

        //    Ipv6(Structural)
        final InstanceIdentifier<Ipv6> ipv6Id = subIfcId.child(Ipv6.class);
        registry.addStructuralReader(ipv6Id, Ipv6Builder.class);
        //     Address
        registry.add(
                new GenericInitListReader<>(ipv6Id.child(Address.class),
                        new SubInterfaceIpv6AddressCustomizer(jvpp, ifcNamingContext)));

        registry.add(new GenericListReader<>(ipv6Id.child(Neighbor.class),
                new SubInterfaceIpv6NeighbourCustomizer(jvpp, ifcNamingContext)));
    }
}
