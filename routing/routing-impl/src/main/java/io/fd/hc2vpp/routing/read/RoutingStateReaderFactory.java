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
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.RoutingConfiguration;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.core.dto.Ip6FibDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.IpFibDetailsReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.RoutingState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.RoutingInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.RoutingProtocols;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.RoutingProtocolsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol.StaticRoutes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol.StaticRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.RoutingProtocolStateVppAttr;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.routing.state.routing.instance.routing.protocols.routing.protocol.VppProtocolStateAttributes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Factory producing readers for routing plugin's data.
 */
public final class RoutingStateReaderFactory implements ReaderFactory, Ipv4ReadRoutingNodes, Ipv6ReadRoutingNodes {

    private static final InstanceIdentifier<RoutingState> ROOT_STATE_CONTAINER_ID =
            InstanceIdentifier.create(RoutingState.class);

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

        final InstanceIdentifier<RoutingInstance> routingInstanceInstanceIdentifier =
                routingInstanceIdentifier(ROOT_STATE_CONTAINER_ID);
        final InstanceIdentifier<RoutingProtocols> routingProtocolsInstanceIdentifier =
                routingProtocolsId(routingInstanceInstanceIdentifier);
        final InstanceIdentifier<RoutingProtocol> routingProtocolInstanceIdentifier =
                routingProtocolInstanceIdentifier(routingProtocolsInstanceIdentifier);
        final InstanceIdentifier<StaticRoutes> staticRoutesInstanceIdentifier =
                staticRoutesInstanceIdentifier(routingProtocolInstanceIdentifier);

        // RoutingState
        registry.add(new GenericReader<>(ROOT_STATE_CONTAINER_ID, new RoutingStateCustomizer()));
        // RoutingInstance
        registry.add(new GenericListReader<>(routingInstanceInstanceIdentifier,
                new RoutingInstanceCustomizer(configuration)));

        // RoutingProtocols
        registry.addStructuralReader(routingProtocolsInstanceIdentifier, RoutingProtocolsBuilder.class);

        // RoutingProtocol
        registry.subtreeAdd(routingProtocolHandledChildren(), new GenericListReader<>(routingProtocolInstanceIdentifier,
                new RoutingProtocolCustomizer(routingProtocolContext, ipv4DumpManager, ipv6DumpManager)));

        // StaticRoutes
        registry.addStructuralReader(staticRoutesInstanceIdentifier, StaticRoutesBuilder.class);

        registerIpv4Routes(staticRoutesInstanceIdentifier, registry, ipv4DumpManager, configuration, routeHopContext,
                interfaceContext, routeContext, routingProtocolContext);
        registerIpv6Routes(staticRoutesInstanceIdentifier, registry, ipv6DumpManager, configuration, routeHopContext,
                interfaceContext, routeContext, routingProtocolContext);
    }

    private static ImmutableSet<InstanceIdentifier<?>> routingProtocolHandledChildren() {
        return ImmutableSet
                .of(InstanceIdentifier.create(RoutingProtocol.class).augmentation(RoutingProtocolStateVppAttr.class)
                        .child(VppProtocolStateAttributes.class));
    }

    private InstanceIdentifier<StaticRoutes> staticRoutesInstanceIdentifier(
            final InstanceIdentifier<RoutingProtocol> routingProtocolInstanceIdentifier) {
        return routingProtocolInstanceIdentifier.child(StaticRoutes.class);
    }

    private InstanceIdentifier<RoutingProtocol> routingProtocolInstanceIdentifier(
            final InstanceIdentifier<RoutingProtocols> routingProtocolsInstanceIdentifier) {
        return routingProtocolsInstanceIdentifier.child(RoutingProtocol.class);
    }

    private InstanceIdentifier<RoutingProtocols> routingProtocolsId(
            final InstanceIdentifier<RoutingInstance> routingInstanceInstanceIdentifier) {
        return routingInstanceInstanceIdentifier.child(RoutingProtocols.class);
    }

    private InstanceIdentifier<RoutingInstance> routingInstanceIdentifier(
            final InstanceIdentifier<RoutingState> routingStateId) {
        return routingStateId.child(RoutingInstance.class);
    }
}
