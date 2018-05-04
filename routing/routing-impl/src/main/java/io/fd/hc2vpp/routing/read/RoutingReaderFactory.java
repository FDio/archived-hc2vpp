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

package io.fd.hc2vpp.routing.read;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.Ipv4RoutingNodes;
import io.fd.hc2vpp.routing.Ipv6RoutingNodes;
import io.fd.hc2vpp.routing.RoutingConfiguration;
import io.fd.hc2vpp.routing.RoutingIIds;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.core.dto.Ip6FibDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.Ip6FibDump;
import io.fd.vpp.jvpp.core.dto.IpFibDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.IpFibDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.StaticRoutes1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.RoutingBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.ControlPlaneProtocolsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.control.plane.protocol.StaticRoutesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Factory producing readers for routing plugin's data.
 */
public final class RoutingReaderFactory implements ReaderFactory, Ipv4RoutingNodes, Ipv6RoutingNodes {

    @Inject
    private RoutingConfiguration configuration;

    @Inject
    @Named("interface-context")
    private NamingContext interfaceContext;

    @Inject
    @Named(RoutingConfiguration.ROUTING_PROTOCOL_CONTEXT)
    private NamingContext routingProtocolContext;

    @Inject
    @Named(RoutingConfiguration.ROUTE_CONTEXT)
    private NamingContext routeContext;

    @Inject
    @Named(RoutingConfiguration.ROUTE_HOP_CONTEXT)
    private MultiNamingContext routeHopContext;

    @Inject
    private FutureJVppCore vppApi;

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        final DumpCacheManager<IpFibDetailsReplyDump, Void> ipv4DumpManager = newIpv4RoutesDumpManager(vppApi);
        final DumpCacheManager<Ip6FibDetailsReplyDump, Void> ipv6DumpManager = newIpv6RoutesDumpManager(vppApi);

        registry.addStructuralReader(RoutingIIds.ROUTING, RoutingBuilder.class);
        registry.addStructuralReader(RoutingIIds.RT_CPS, ControlPlaneProtocolsBuilder.class);
        registry.add(new GenericListReader<>(RoutingIIds.RT_CPS_CP,
                                             new ControlPlaneProtocolCustomizer(routingProtocolContext,
                                                                                ipv4DumpManager,
                                                                                ipv6DumpManager)));
        registry.addStructuralReader(RoutingIIds.RT_CPS_CP_SR, StaticRoutesBuilder.class);

        registerIpv4RoutesReaders(registry, ipv4DumpManager, configuration, routeHopContext,
                                  interfaceContext, routeContext, routingProtocolContext);
        registerIpv6RoutesReaders(registry, ipv6DumpManager, configuration, routeHopContext,
                                  interfaceContext, routeContext, routingProtocolContext);
    }

    private static InstanceIdentifier<Ipv4> ipv4Identifier(
        final InstanceIdentifier<StaticRoutes1> staticRoutes2InstanceIdentifier) {
        return staticRoutes2InstanceIdentifier.child(Ipv4.class);
    }

    private static InstanceIdentifier<Ipv6> ipv6Identifier(
        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.StaticRoutes1> staticRoutes2InstanceIdentifier) {
        return staticRoutes2InstanceIdentifier.child(Ipv6.class);
    }

    private static InstanceIdentifier<StaticRoutes1> staticRoutesIpv4Iid() {
        return RoutingIIds.RT_CPS_CP_SR.augmentation(StaticRoutes1.class);
    }

    private static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.StaticRoutes1> staticRoutesIpv6Iid() {
        return RoutingIIds.RT_CPS_CP_SR.augmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.StaticRoutes1.class);
    }

    private DumpCacheManager<IpFibDetailsReplyDump, Void> newIpv4RoutesDumpManager(
        @Nonnull final FutureJVppCore vppApi) {
        return new DumpCacheManager.DumpCacheManagerBuilder<IpFibDetailsReplyDump, Void>()
            .withExecutor(
                (identifier, params) -> getReplyForRead(vppApi.ipFibDump(new IpFibDump()).toCompletableFuture(),
                                                        identifier))
            .acceptOnly(IpFibDetailsReplyDump.class)
            .build();
    }

    private DumpCacheManager<Ip6FibDetailsReplyDump, Void> newIpv6RoutesDumpManager(
        @Nonnull final FutureJVppCore vppApi) {
        return new DumpCacheManager.DumpCacheManagerBuilder<Ip6FibDetailsReplyDump, Void>()
            .withExecutor(
                (identifier, params) -> getReplyForRead(
                    vppApi.ip6FibDump(new Ip6FibDump()).toCompletableFuture(), identifier))
            .acceptOnly(Ip6FibDetailsReplyDump.class)
            .build();
    }

    private void registerIpv4RoutesReaders(@Nonnull final ModifiableReaderRegistryBuilder registry,
                                           @Nonnull final DumpCacheManager<IpFibDetailsReplyDump, Void> ipv4DumpManager,
                                           @Nonnull final RoutingConfiguration configuration,
                                           @Nonnull final MultiNamingContext routeHopContext,
                                           @Nonnull final NamingContext interfaceContext,
                                           @Nonnull final NamingContext routeContext,
                                           @Nonnull final NamingContext routingProtocolContext) {

        final InstanceIdentifier<StaticRoutes1> staticRoutes2InstanceIdentifier =
            staticRoutesIpv4Iid();
        final InstanceIdentifier<Ipv4> ipv4InstanceIdentifier = ipv4Identifier(staticRoutes2InstanceIdentifier);

        registry.addStructuralReader(staticRoutes2InstanceIdentifier, StaticRoutes1Builder.class);

        registry.addStructuralReader(ipv4InstanceIdentifier, Ipv4Builder.class);
        registry.subtreeAdd(ipv4RoutingHandledChildren(InstanceIdentifier.create(Route.class)),
                            new GenericListReader<>(ipv4InstanceIdentifier.child(Route.class),
                                                    new Ipv4RouteCustomizer(ipv4DumpManager, configuration,
                                                                            routeHopContext, interfaceContext,
                                                                            routeContext, routingProtocolContext)));
    }

    private void registerIpv6RoutesReaders(@Nonnull final ModifiableReaderRegistryBuilder registry,
                                           @Nonnull final DumpCacheManager<Ip6FibDetailsReplyDump, Void> ipv6DumpManager,
                                           @Nonnull final RoutingConfiguration configuration,
                                           @Nonnull final MultiNamingContext routeHopContext,
                                           @Nonnull final NamingContext interfaceContext,
                                           @Nonnull final NamingContext routeContext,
                                           @Nonnull final NamingContext routingProtocolContext) {

        final InstanceIdentifier<Ipv6> ipv6InstanceIdentifier = ipv6Identifier(staticRoutesIpv6Iid());

        registry.addStructuralReader(staticRoutesIpv6Iid(),
                                     org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.StaticRoutes1Builder.class);
        registry.addStructuralReader(ipv6InstanceIdentifier, Ipv6Builder.class);
        registry.subtreeAdd(ipv6RoutingHandledChildren(InstanceIdentifier.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.Route.class)),
                            new GenericListReader<>(ipv6InstanceIdentifier.child(
                                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.Route.class),
                                                    new Ipv6RouteCustomizer(ipv6DumpManager, configuration,
                                                                            routeHopContext, interfaceContext,
                                                                            routeContext, routingProtocolContext)));
    }
}
