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

import static io.fd.honeycomb.v3po.translate.util.RWUtils.singletonChildWriterList;

import io.fd.honeycomb.v3po.translate.impl.write.CompositeChildWriter;
import io.fd.honeycomb.v3po.translate.impl.write.CompositeListWriter;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.util.write.ReflexiveAugmentWriterCustomizer;
import io.fd.honeycomb.v3po.translate.util.write.ReflexiveChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.RewriteCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.SubInterfaceCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.SubInterfaceL2Customizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.ip.SubInterfaceIpv4AddressCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.write.ChildWriter;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.l2.Rewrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.ipv4.Address;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.openvpp.jvpp.future.FutureJVpp;

final class SubinterfaceAugmentationWriterFactory {

    private SubinterfaceAugmentationWriterFactory() {
    }

    private static ChildWriter<Ipv4> getIp4Writer(
        @Nonnull final FutureJVpp futureJvpp, @Nonnull final NamingContext interfaceContext) {

        final ChildWriter<Address> addressWriter = new CompositeListWriter<>(
            Address.class,
            new SubInterfaceIpv4AddressCustomizer(futureJvpp, interfaceContext));

        return new CompositeChildWriter<>(
            Ipv4.class,
            RWUtils.singletonChildWriterList(addressWriter),
            new ReflexiveChildWriterCustomizer<>());
    }

    private static ChildWriter<L2> getL2Writer(
        @Nonnull final FutureJVpp futureJvpp, @Nonnull final NamingContext interfaceContext,
        @Nonnull final NamingContext bridgeDomainContext) {

        final ChildWriter<? extends ChildOf<L2>> rewriteWriter =
            new CompositeChildWriter<>(Rewrite.class, new RewriteCustomizer(futureJvpp, interfaceContext));

        return new CompositeChildWriter<>(
            L2.class,
            singletonChildWriterList(rewriteWriter),
            new SubInterfaceL2Customizer(futureJvpp, interfaceContext, bridgeDomainContext)
        );
    }

    static ChildWriter<SubinterfaceAugmentation> createInstance(
        @Nonnull final FutureJVpp futureJvpp, @Nonnull final NamingContext interfaceContext,
        @Nonnull final NamingContext bridgeDomainContext) {
        final List<ChildWriter<? extends ChildOf<SubInterface>>> childWriters = new ArrayList<>();

        // TODO L2 is ChildOf<SubInterfaceBaseAttributes>, but SubInterface extends SubInterfaceBaseAttributes
        // If we use containers inside groupings, we need to cast and lose static type checking.
        // Can we get rid of the cast?
        childWriters.add((ChildWriter) getL2Writer(futureJvpp, interfaceContext, bridgeDomainContext));
        childWriters.add((ChildWriter) getIp4Writer(futureJvpp, interfaceContext));

        final CompositeListWriter<SubInterface, SubInterfaceKey> subInterfaceWriter = new CompositeListWriter<>(
            SubInterface.class,
            childWriters,
            new SubInterfaceCustomizer(futureJvpp, interfaceContext));

        final ChildWriter<SubInterfaces> subInterfacesWriter = new CompositeChildWriter<>(
            SubInterfaces.class,
            singletonChildWriterList(subInterfaceWriter),
            new ReflexiveChildWriterCustomizer<>());

        return new CompositeChildWriter<>(
            SubinterfaceAugmentation.class,
            singletonChildWriterList(subInterfacesWriter),
            RWUtils.emptyAugWriterList(),
            new ReflexiveAugmentWriterCustomizer<>());
    }
}
