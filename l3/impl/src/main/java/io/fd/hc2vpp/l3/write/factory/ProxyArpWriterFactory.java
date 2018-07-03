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

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.l3.write.ipv4.ProxyArpCustomizer;
import io.fd.hc2vpp.l3.write.ipv4.ProxyRangeCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev180703.ProxyArpInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev180703.ProxyRanges;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev180703.interfaces._interface.ProxyArp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev180703.proxy.ranges.ProxyRange;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class ProxyArpWriterFactory implements WriterFactory {

    public static final InstanceIdentifier<ProxyRange> PROXY_RANGE_IID =
        InstanceIdentifier.create(ProxyRanges.class).child(ProxyRange.class);
    private static final InstanceIdentifier<Interface>
        IFC_ID = InstanceIdentifier.create(Interfaces.class).child(Interface.class);
    private static final InstanceIdentifier<ProxyArp> PROXY_ARP_IID =
        IFC_ID.augmentation(ProxyArpInterfaceAugmentation.class).child(ProxyArp.class);

    private final FutureJVppCore jvpp;
    private final NamingContext ifcNamingContext;

    @Inject
    public ProxyArpWriterFactory(final FutureJVppCore vppJvppIfcDependency,
                                 @Named("interface-context") final NamingContext interfaceContextDependency) {
        this.jvpp = vppJvppIfcDependency;
        this.ifcNamingContext = interfaceContextDependency;
    }

    @Override
    public void init(final ModifiableWriterRegistryBuilder registry) {
        // proxy-arp
        //  proxy-range =
        registry.add(new GenericListWriter<>(PROXY_RANGE_IID, new ProxyRangeCustomizer(jvpp)));

        // interfaces
        //  interface
        //   proxy-arp-interface-augmentation
        //    proxy-arp =
        registry.addAfter(new GenericWriter<>(PROXY_ARP_IID, new ProxyArpCustomizer(jvpp, ifcNamingContext)),
            Sets.newHashSet(PROXY_RANGE_IID, IFC_ID));
    }
}
