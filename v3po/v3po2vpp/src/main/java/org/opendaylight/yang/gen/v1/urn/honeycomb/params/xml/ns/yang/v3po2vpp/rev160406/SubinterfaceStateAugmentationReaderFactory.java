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

package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;

import static io.fd.honeycomb.v3po.translate.util.RWUtils.singletonChildReaderList;

import io.fd.honeycomb.v3po.translate.impl.read.CompositeChildReader;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeListReader;
import io.fd.honeycomb.v3po.translate.read.ChildReader;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveAugmentReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.RewriteCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.SubInterfaceCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.SubInterfaceL2Customizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.ip.SubInterfaceIpv4AddressCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.SubInterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.l2.Rewrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.ipv4.Address;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.openvpp.jvpp.future.FutureJVpp;

final class SubinterfaceStateAugmentationReaderFactory {

    private SubinterfaceStateAugmentationReaderFactory() {
    }

    private static ChildReader<L2> getL2Reader(@Nonnull final FutureJVpp futureJvpp,
                                                 @Nonnull final NamingContext interfaceContext,
                                                 @Nonnull final NamingContext bridgeDomainContext) {
        final ChildReader<Rewrite> rewriteReader = new CompositeChildReader<>(
            Rewrite.class, new RewriteCustomizer(futureJvpp, interfaceContext));

        return new CompositeChildReader<>(L2.class,
                singletonChildReaderList(rewriteReader),
                new SubInterfaceL2Customizer(futureJvpp, interfaceContext, bridgeDomainContext));
    }

    private static ChildReader<Ipv4> getIpv4Reader(@Nonnull final FutureJVpp futureJvpp,
                                                   @Nonnull final NamingContext interfaceContext) {

        final ChildReader<Address> addressReader = new CompositeListReader<>(Address.class,
            new SubInterfaceIpv4AddressCustomizer(futureJvpp, interfaceContext));

        return new CompositeChildReader<>(
            Ipv4.class,
            RWUtils.singletonChildReaderList(addressReader),
            new ReflexiveChildReaderCustomizer<>(Ipv4Builder.class));

    }

    static ChildReader<SubinterfaceStateAugmentation> createInstance(
        @Nonnull final FutureJVpp futureJvpp, @Nonnull final NamingContext interfaceContext,
        @Nonnull final NamingContext bridgeDomainContext) {

        List<ChildReader<? extends ChildOf<SubInterface>>> childReaders = new ArrayList<>();

        // TODO can get rid of that cast?
        childReaders.add((ChildReader) getL2Reader(futureJvpp, interfaceContext, bridgeDomainContext));
        childReaders.add((ChildReader) getIpv4Reader(futureJvpp, interfaceContext));

        final CompositeListReader<SubInterface, SubInterfaceKey, SubInterfaceBuilder> subInterfaceReader =
            new CompositeListReader<>(SubInterface.class, childReaders, new SubInterfaceCustomizer(futureJvpp,
                interfaceContext));

        final ChildReader<SubInterfaces> subInterfacesReader = new CompositeChildReader<>(
            SubInterfaces.class,
            RWUtils.singletonChildReaderList(subInterfaceReader),
            new ReflexiveChildReaderCustomizer<>(SubInterfacesBuilder.class));

        final ChildReader<SubinterfaceStateAugmentation> subinterfaceStateAugmentationReader =
            new CompositeChildReader<>(SubinterfaceStateAugmentation.class,
                singletonChildReaderList(subInterfacesReader),
                new ReflexiveAugmentReaderCustomizer<>(
                    SubinterfaceStateAugmentationBuilder.class,
                    SubinterfaceStateAugmentation.class));

        return subinterfaceStateAugmentationReader;
    }
}
