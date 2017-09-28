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


import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.RoutingConfiguration;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.core.dto.IpFibDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.IpFibDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.StaticRoutes2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.StaticRoutes2Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.VppIpv4RouteState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.NextHopList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.next.hop.list.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.table.lookup.TableLookupParams;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol.StaticRoutes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;
import java.util.Set;

interface Ipv4ReadRoutingNodes extends JvppReplyConsumer {

    static InstanceIdentifier<StaticRoutes2> staticRoutesInstanceIdentifier(
            final InstanceIdentifier<StaticRoutes> staticRoutesInstanceIdentifier) {
        return staticRoutesInstanceIdentifier.augmentation(StaticRoutes2.class);
    }

    static InstanceIdentifier<Ipv4> ipv4Identifier(
            final InstanceIdentifier<StaticRoutes2> staticRoutes2InstanceIdentifier) {
        return staticRoutes2InstanceIdentifier.child(Ipv4.class);
    }

    default DumpCacheManager<IpFibDetailsReplyDump, Void> newIpv4RoutesDumpManager(
            @Nonnull final FutureJVppCore vppApi) {
        return new DumpCacheManager.DumpCacheManagerBuilder<IpFibDetailsReplyDump, Void>()
                .withExecutor(
                        (identifier, params) -> getReplyForRead(vppApi.ipFibDump(new IpFibDump()).toCompletableFuture(),
                                identifier))
                .acceptOnly(IpFibDetailsReplyDump.class)
                .build();
    }

    default void registerIpv4Routes(@Nonnull final InstanceIdentifier<StaticRoutes> subTreeId,
                                    @Nonnull final ModifiableReaderRegistryBuilder registry,
                                    @Nonnull final DumpCacheManager<IpFibDetailsReplyDump, Void> ipv4DumpManager,
                                    @Nonnull final RoutingConfiguration configuration,
                                    @Nonnull final MultiNamingContext routeHopContext,
                                    @Nonnull final NamingContext interfaceContext,
                                    @Nonnull final NamingContext routeContext,
                                    @Nonnull final NamingContext routingProtocolContext) {

        final InstanceIdentifier<StaticRoutes2> staticRoutes2InstanceIdentifier =
                staticRoutesInstanceIdentifier(subTreeId);
        final InstanceIdentifier<Ipv4> ipv4InstanceIdentifier = ipv4Identifier(staticRoutes2InstanceIdentifier);

        registry.addStructuralReader(staticRoutes2InstanceIdentifier, StaticRoutes2Builder.class);

        registry.addStructuralReader(ipv4InstanceIdentifier, Ipv4Builder.class);
        registry.subtreeAdd(ipv4RoutingHandledChildren(InstanceIdentifier.create(Route.class)),
                new GenericListReader<>(ipv4InstanceIdentifier.child(Route.class),
                        new Ipv4RouteCustomizer(ipv4DumpManager, configuration, routeHopContext, interfaceContext,
                                routeContext, routingProtocolContext)));
    }

    default Set<InstanceIdentifier<?>> ipv4RoutingHandledChildren(
            final InstanceIdentifier<Route> parent) {
        return ImmutableSet.of(
                parent.child(TableLookupParams.class),
                parent.child(NextHopList.class),
                parent.child(NextHopList.class).child(NextHop.class),
                parent.child(VppIpv4RouteState.class));
    }

}
