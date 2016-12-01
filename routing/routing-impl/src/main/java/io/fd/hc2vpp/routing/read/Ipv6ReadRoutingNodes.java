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
import io.fd.vpp.jvpp.core.dto.Ip6FibDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.Ip6FibDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.StaticRoutes2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.StaticRoutes2Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.VppIpv6RouteState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.NextHopList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.next.hop.list.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol.StaticRoutes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

interface Ipv6ReadRoutingNodes extends JvppReplyConsumer {

    static InstanceIdentifier<StaticRoutes2> staticRoutesInstanceIdentifier(
            final InstanceIdentifier<StaticRoutes> staticRoutesInstanceIdentifier) {
        return staticRoutesInstanceIdentifier.augmentation(StaticRoutes2.class);
    }

    static InstanceIdentifier<Ipv6> ipv6Identifier(
            final InstanceIdentifier<StaticRoutes2> staticRoutes2InstanceIdentifier) {
        return staticRoutes2InstanceIdentifier.child(Ipv6.class);
    }

    default DumpCacheManager<Ip6FibDetailsReplyDump, Void> newIpv6RoutesDumpManager(
            @Nonnull final FutureJVppCore vppApi) {
        return new DumpCacheManager.DumpCacheManagerBuilder<Ip6FibDetailsReplyDump, Void>()
                .withExecutor(
                        (identifier, params) -> getReplyForRead(
                                vppApi.ip6FibDump(new Ip6FibDump()).toCompletableFuture(), identifier))
                .acceptOnly(Ip6FibDetailsReplyDump.class)
                .build();
    }

    default void registerIpv6Routes(@Nonnull final InstanceIdentifier<StaticRoutes> subTreeId,
                                    @Nonnull final ModifiableReaderRegistryBuilder registry,
                                    @Nonnull final DumpCacheManager<Ip6FibDetailsReplyDump, Void> ipv6DumpManager,
                                    @Nonnull final RoutingConfiguration configuration,
                                    @Nonnull final MultiNamingContext routeHopContext,
                                    @Nonnull final NamingContext interfaceContext,
                                    @Nonnull final NamingContext routeContext,
                                    @Nonnull final NamingContext routingProtocolContext) {

        final InstanceIdentifier<StaticRoutes2> staticRoutes2InstanceIdentifier =
                staticRoutesInstanceIdentifier(subTreeId);
        final InstanceIdentifier<Ipv6> ipv6InstanceIdentifier = ipv6Identifier(staticRoutes2InstanceIdentifier);

        registry.addStructuralReader(staticRoutes2InstanceIdentifier, StaticRoutes2Builder.class);

        registry.addStructuralReader(ipv6InstanceIdentifier, Ipv6Builder.class);
        registry.subtreeAdd(ipv6RoutingHandledChildren(InstanceIdentifier.create(Route.class)),
                new GenericListReader<>(ipv6InstanceIdentifier.child(Route.class),
                        new Ipv6RouteCustomizer(ipv6DumpManager, configuration, routeHopContext, interfaceContext,
                                routeContext, routingProtocolContext)));
    }

    default Set<InstanceIdentifier<?>> ipv6RoutingHandledChildren(
            final InstanceIdentifier<Route> parent) {
        return ImmutableSet.of(parent.child(NextHopList.class),
                parent.child(NextHopList.class).child(NextHop.class),
                parent.child(VppIpv6RouteState.class));
    }

}
