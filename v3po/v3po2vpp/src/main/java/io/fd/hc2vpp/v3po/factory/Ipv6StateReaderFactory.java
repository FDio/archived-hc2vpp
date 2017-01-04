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

package io.fd.hc2vpp.v3po.factory;


import static io.fd.hc2vpp.v3po.factory.InterfacesStateReaderFactory.IFC_ID;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.ip.v6.Ipv6AddressCustomizer;
import io.fd.hc2vpp.v3po.interfacesstate.ip.v6.Ipv6Customizer;
import io.fd.hc2vpp.v3po.interfacesstate.ip.v6.Ipv6NeighbourCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv6.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv6.Neighbor;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv6StateReaderFactory implements ReaderFactory {

    private static final InstanceIdentifier<Interface2> IFC_2_ID = IFC_ID.augmentation(Interface2.class);

    @Inject
    private FutureJVppCore jvpp;

    @Inject
    @Named("interface-context")
    private NamingContext ifcNamingCtx;

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        //    Ipv6
        final InstanceIdentifier<Ipv6> ipv6Id = IFC_2_ID.child(Ipv6.class);
        registry.add(new GenericReader<>(ipv6Id, new Ipv6Customizer(jvpp)));
        //     Ipv6 Address
        final InstanceIdentifier<Address> ipv6AddrId = ipv6Id.child(Address.class);
        registry.add(new GenericInitListReader<>(ipv6AddrId, new Ipv6AddressCustomizer(jvpp, ifcNamingCtx)));
        //     Ipv6 Neighbor
        final InstanceIdentifier<Neighbor> neighborId = ipv6Id.child(Neighbor.class);
        registry.add(new GenericListReader<>(neighborId, new Ipv6NeighbourCustomizer(jvpp, ifcNamingCtx)));
    }
}
