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

package io.fd.hc2vpp.routing.write;

import static io.fd.hc2vpp.routing.RoutingConfiguration.ROUTE_CONTEXT;
import static io.fd.hc2vpp.routing.RoutingConfiguration.ROUTE_HOP_CONTEXT;
import static io.fd.hc2vpp.routing.RoutingConfiguration.ROUTING_PROTOCOL_CONTEXT;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.Ipv4RoutingNodes;
import io.fd.hc2vpp.routing.Ipv6RoutingNodes;
import io.fd.hc2vpp.routing.RoutingConfiguration;
import io.fd.hc2vpp.routing.RoutingIIds;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.routing.ra.rev180319.ControlAdvPrefixesVppAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.routing.ra.rev180319.Ipv6RouterAdvertisementsVppAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.routing.rev180319.RoutingProtocolVppAttr;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.routing.rev180319.routing.control.plane.protocols.control.plane.protocol.VppProtocolAttributes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.interfaces._interface.ipv6.Ipv6RouterAdvertisements;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.interfaces._interface.ipv6.ipv6.router.advertisements.prefix.list.Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.ControlPlaneProtocols;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocol;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Factory producing writers for routing plugin's data.
 */
public final class RoutingWriterFactory implements WriterFactory, Ipv4RoutingNodes, Ipv6RoutingNodes {

    private static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface> IFACE_ID =
            InstanceIdentifier.create(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface.class);

    @Inject
    private FutureJVppCore vppApi;

    @Inject
    private RoutingConfiguration configuration;

    @Inject
    @Named("interface-context")
    private NamingContext interfaceContext;

    @Inject
    @Named(ROUTING_PROTOCOL_CONTEXT)
    private NamingContext routingProtocolContext;

    @Inject
    @Named(ROUTE_CONTEXT)
    private NamingContext routeContext;

    @Inject
    @Named("classify-table-context")
    private VppClassifierContextManager vppClassifierContextManager;

    @Inject
    @Named(ROUTE_HOP_CONTEXT)
    private MultiNamingContext routHopContext;

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {

        registry.subtreeAdd(rootNodeHandledChildren(RoutingIIds.ROUTING),
                new GenericWriter<>(RoutingIIds.ROUTING, new RoutingCustomizer()));

        registry.subtreeAdd(routingProtocolHandledChildren(),new GenericWriter<>(RoutingIIds.RT_CPS_CP,
                new ControlPlaneProtocolCustomizer(routingProtocolContext)));

        registry.subtreeAddAfter(ipv4RoutingHandledChildren(RoutingIIds.RT_CPS_CP_SR_SRV4_IPV4_RT_PARENT),
                                 new GenericWriter<>(RoutingIIds.RT_CPS_CP_SR_SRV4_IPV4_RT,
                                                     new Ipv4RouteCustomizer(vppApi, interfaceContext, routeContext,
                                                                             routingProtocolContext, routHopContext,
                                                                             vppClassifierContextManager)), IFACE_ID);
        registry.subtreeAddAfter(ipv6RoutingHandledChildren(RoutingIIds.RT_CPS_CP_SR_SRV6_IPV6_RT_PARENT),
                                 new GenericWriter<>(RoutingIIds.RT_CPS_CP_SR_SRV6_IPV6_RT,
                                                     new Ipv6RouteCustomizer(vppApi, interfaceContext, routeContext,
                                                                             routingProtocolContext, routHopContext,
                                                                             vppClassifierContextManager)), IFACE_ID);

        //router advertisements
        registry.subtreeAdd(raHandledChildren(),
            new GenericWriter<>(RoutingIIds.IFCS_IFC_IFC1_IPV6_IPV61_RTADV,
                                new RouterAdvertisementsCustomizer(vppApi, interfaceContext)));
        registry.subtreeAdd(prefixHandledChildren(),
            new GenericWriter<>(RoutingIIds.IFCS_IFC_IFC1_IPV6_IPV61_RTADV_PRLST_PRFX,
                                new PrefixCustomizer(vppApi, interfaceContext)));
    }

    private static ImmutableSet<InstanceIdentifier<?>> routingProtocolHandledChildren() {
        return ImmutableSet.of(InstanceIdentifier.create(ControlPlaneProtocol.class)
                                   .augmentation(RoutingProtocolVppAttr.class)
                                   .child(VppProtocolAttributes.class));
    }

    private static Set<InstanceIdentifier<?>> rootNodeHandledChildren(final InstanceIdentifier<Routing> parent) {
        return ImmutableSet.of(parent.child(ControlPlaneProtocols.class));
    }

    private static Set<InstanceIdentifier<?>> raHandledChildren() {
        final InstanceIdentifier<Ipv6RouterAdvertisements> raIID =
            InstanceIdentifier.create(Ipv6RouterAdvertisements.class);
        return ImmutableSet.of(raIID.augmentation(Ipv6RouterAdvertisementsVppAugmentation.class));
    }

    private static Set<InstanceIdentifier<?>> prefixHandledChildren() {
        final InstanceIdentifier<Prefix> prefixIID = InstanceIdentifier.create(Prefix.class);
        return ImmutableSet.of(prefixIID.augmentation(ControlAdvPrefixesVppAugmentation.class));
    }
}
