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
import io.fd.hc2vpp.l3.write.ipv6.Ipv6AddressCustomizer;
import io.fd.hc2vpp.l3.write.ipv6.Ipv6AddressValidator;
import io.fd.hc2vpp.l3.write.ipv6.Ipv6Customizer;
import io.fd.hc2vpp.l3.write.ipv6.Ipv6NeighbourCustomizer;
import io.fd.hc2vpp.l3.write.ipv6.Ipv6NeighbourValidator;
import io.fd.hc2vpp.l3.write.ipv6.nd.NdProxyCustomizer;
import io.fd.hc2vpp.l3.write.ipv6.nd.NdProxyValidator;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.NdProxyIp6Augmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces._interface.ipv6.NdProxies;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces._interface.ipv6.nd.proxies.NdProxy;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv6.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv6.Neighbor;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv6WriterFactory implements WriterFactory {

    @Inject
    private FutureJVppCore jvpp;

    @Inject
    @Named("interface-context")
    private NamingContext ifcNamingContext;

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {

        final InstanceIdentifier<Interface1> ifc1AugId = InstanceIdentifier.create(Interfaces.class)
                .child(Interface.class).augmentation(Interface1.class);

        // Ipv6(after interface) =
        final InstanceIdentifier<Ipv6> ipv6Id = ifc1AugId.child(Ipv6.class);
        // changing ip table for interface that has ip address assigned is illegal action(internal vpp behaviour)
        registry.addAfter(new GenericWriter<>(ipv6Id, new Ipv6Customizer(jvpp)), ImmutableSet
                .of(IFC_ID, VPP_IFC_AUG_ID.child(Routing.class)));

        final InstanceIdentifier<Address>
                ipv6AddressId = ipv6Id.child(Address.class);
        registry.addAfter(new GenericListWriter<>(ipv6AddressId, new Ipv6AddressCustomizer(jvpp, ifcNamingContext),
                        new Ipv6AddressValidator(ifcNamingContext)),
                ipv6Id);

        registry.addAfter(new GenericListWriter<>(ipv6Id.child(Neighbor.class),
                        new Ipv6NeighbourCustomizer(jvpp, ifcNamingContext), new Ipv6NeighbourValidator(ifcNamingContext)),
                ipv6AddressId);
        //     ND Proxy
        final InstanceIdentifier<NdProxy> ndProxyId =
                ipv6Id.augmentation(NdProxyIp6Augmentation.class).child(NdProxies.class).child(NdProxy.class);
        registry.addAfter(new GenericListWriter<>(ndProxyId, new NdProxyCustomizer(jvpp, ifcNamingContext),
                new NdProxyValidator(ifcNamingContext)), ipv6Id);
    }
}
