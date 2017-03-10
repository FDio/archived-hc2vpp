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


import static io.fd.hc2vpp.v3po.factory.SubinterfaceAugmentationWriterFactory.L2_ID;
import static io.fd.hc2vpp.v3po.factory.SubinterfaceAugmentationWriterFactory.SUB_IFC_ID;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.l3.write.ipv6.subinterface.SubInterfaceIpv6AddressCustomizer;
import io.fd.hc2vpp.l3.write.ipv6.subinterface.SubInterfaceIpv6NeighbourCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.l2.Rewrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip6.attributes.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip6.attributes.ipv6.Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceIpv6WriterFactory implements WriterFactory {

    @Inject
    private FutureJVppCore jvpp;

    @Inject
    @Named("interface-context")
    private NamingContext ifcNamingContext;

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {

        final InstanceIdentifier<Rewrite> rewriteId = L2_ID.child(Rewrite.class);

        //   Ipv6
        final InstanceIdentifier<Address>
                ipv6SubifcAddressId = SUB_IFC_ID.child(Ipv6.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip6.attributes.ipv6.Address.class);
        registry.addAfter(new GenericListWriter<>(ipv6SubifcAddressId,
                new SubInterfaceIpv6AddressCustomizer(jvpp, ifcNamingContext)), rewriteId);
        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip6.attributes.ipv6.Neighbor>
                ipv6NeighborId = SUB_IFC_ID.child(Ipv6.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip6.attributes.ipv6.Neighbor.class);
        registry.addAfter(new GenericListWriter<>(ipv6NeighborId,
                new SubInterfaceIpv6NeighbourCustomizer(jvpp, ifcNamingContext)), rewriteId);
    }
}
