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

package io.fd.hc2vpp.l3.write.factory;

import static io.fd.hc2vpp.v3po.factory.InterfacesWriterFactory.IFC_ID;
import static io.fd.hc2vpp.v3po.factory.InterfacesWriterFactory.VPP_IFC_AUG_ID;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.l3.write.ipv4.Ipv4AddressCustomizer;
import io.fd.hc2vpp.l3.write.ipv4.Ipv4Customizer;
import io.fd.hc2vpp.l3.write.ipv4.Ipv4NeighbourCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Neighbor;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv4WriterFactory implements WriterFactory {

    @Inject
    private FutureJVppCore jvpp;

    @Inject
    @Named("interface-context")
    private NamingContext ifcNamingContext;

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {

        final InstanceIdentifier<Interface1> ifc1AugId = IFC_ID.augmentation(Interface1.class);

        // Ipv4(after interface)
        final InstanceIdentifier<Ipv4> ipv4Id = ifc1AugId.child(Ipv4.class);
        // changing ip table for interface that has ip address assigned is illegal action(internal vpp behaviour)
        registry.addAfter(new GenericWriter<>(ipv4Id, new Ipv4Customizer(jvpp)),
                ImmutableSet.of(IFC_ID, VPP_IFC_AUG_ID.child(Routing.class)));
        //  Address(after Ipv4) =
        final InstanceIdentifier<Address> ipv4AddressId = ipv4Id.child(Address.class);
        registry.addAfter(new GenericListWriter<>(ipv4AddressId, new Ipv4AddressCustomizer(jvpp, ifcNamingContext)),
                ipv4Id);
        //  Neighbor(after ipv4Address)
        registry.addAfter(new GenericListWriter<>(ipv4Id.child(Neighbor.class), new Ipv4NeighbourCustomizer(jvpp,
                ifcNamingContext)), ipv4AddressId);
    }

}
