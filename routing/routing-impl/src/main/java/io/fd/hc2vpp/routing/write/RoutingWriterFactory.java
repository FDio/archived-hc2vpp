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
import io.fd.hc2vpp.routing.RoutingConfiguration;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.interfaces._interface.Ipv6RouterAdvertisements;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.interfaces._interface.ipv6.router.advertisements.PrefixList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.interfaces._interface.ipv6.router.advertisements.prefix.list.Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.RoutingInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.RoutingProtocols;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol.StaticRoutes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.ra.rev170502.ControlAdvPrefixesVppAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.ra.rev170502.Ipv6RouterAdvertisementsVppAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.RoutingProtocolVppAttr;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol.VppProtocolAttributes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Factory producing writers for routing plugin's data.
 */
public final class RoutingWriterFactory implements WriterFactory, Ipv4WriteRoutingNodes, Ipv6WriteRoutingNodes {

    private static final InstanceIdentifier<Routing> ROOT_CONTAINER_ID = InstanceIdentifier.create(Routing.class);
    private static final InstanceIdentifier<Interface> ROUTING_INTERFACE_ID =
        ROOT_CONTAINER_ID.child(RoutingInstance.class).child(Interfaces.class).child(Interface.class);
    private static final InstanceIdentifier<Ipv6RouterAdvertisements> ROUTING_ADVERTISMENT_ID =
        ROUTING_INTERFACE_ID.augmentation(
            Interface1.class).child(Ipv6RouterAdvertisements.class);
    private static final InstanceIdentifier<Prefix> PREFIX_ID =
        ROUTING_ADVERTISMENT_ID.child(PrefixList.class).child(Prefix.class);

    private static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface> IFACE_ID =
            InstanceIdentifier.create(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface.class);

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

        registry.subtreeAdd(rootNodeHandledChildren(ROOT_CONTAINER_ID),
                new GenericWriter<>(ROOT_CONTAINER_ID, new RoutingCustomizer()));

        registry.add(new GenericWriter<>(routingInstanceIdentifier(), new RoutingInstanceCustomizer(configuration)));

        registry.subtreeAdd(routingProtocolHandledChildren(),new GenericWriter<>(routingProtocolIdentifier(),
                new RoutingProtocolCustomizer(routingProtocolContext)));

        final InstanceIdentifier<StaticRoutes> staticRoutesInstanceIdentifier = staticRoutesIdentifier();
        final InstanceIdentifier<Route> ipv4RouteIdentifier = ipv4RouteIdentifier(staticRoutesInstanceIdentifier);
        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.Route>
                ipv6RouteIdentifier = ipv6RouteIdentifier(staticRoutesInstanceIdentifier);
        registry.subtreeAddAfter(ipv4RoutingHandledChildren(ipv4RouteSubtree()), new GenericWriter<>(ipv4RouteIdentifier,
                new Ipv4RouteCustomizer(vppApi, interfaceContext, routeContext, routingProtocolContext, routHopContext,
                        vppClassifierContextManager)), IFACE_ID);
        registry.subtreeAddAfter(ipv6RoutingHandledChildren(ipv6RouteSubtree()), new GenericWriter<>(ipv6RouteIdentifier,
                new Ipv6RouteCustomizer(vppApi, interfaceContext, routeContext, routingProtocolContext, routHopContext,
                        vppClassifierContextManager)), IFACE_ID);
        registry.add(new GenericWriter<>(ROUTING_INTERFACE_ID, new RoutingInterfaceCustomizer()));
        registry.subtreeAdd(raHandledChildren(),
            new GenericWriter<>(ROUTING_ADVERTISMENT_ID, new RouterAdvertisementsCustomizer(vppApi, interfaceContext)));
        registry.subtreeAdd(prefixHandledChildren(),
            new GenericWriter<>(PREFIX_ID, new PrefixCustomizer(vppApi, interfaceContext)));

    }

    private static ImmutableSet<InstanceIdentifier<?>> routingProtocolHandledChildren() {
        return ImmutableSet
                .of(InstanceIdentifier.create(RoutingProtocol.class).augmentation(RoutingProtocolVppAttr.class).child(VppProtocolAttributes.class));
    }

    private static InstanceIdentifier<RoutingInstance> routingInstanceIdentifier() {
        return ROOT_CONTAINER_ID.child(RoutingInstance.class);
    }
    private static InstanceIdentifier<RoutingProtocol> routingProtocolIdentifier() {
        return routingInstanceIdentifier().child(RoutingProtocols.class).child(RoutingProtocol.class);
    }

    private static InstanceIdentifier<StaticRoutes> staticRoutesIdentifier() {
        return routingProtocolIdentifier().child(StaticRoutes.class);
    }

    private static Set<InstanceIdentifier<?>> rootNodeHandledChildren(final InstanceIdentifier<Routing> parent) {
        return ImmutableSet.of(parent.child(RoutingInstance.class).child(RoutingProtocols.class));
    }

    private static Set<InstanceIdentifier<?>> raHandledChildren() {
        final InstanceIdentifier<Ipv6RouterAdvertisements> raIID = InstanceIdentifier.create(Ipv6RouterAdvertisements.class);
        return ImmutableSet.of(raIID.augmentation(Ipv6RouterAdvertisementsVppAugmentation.class));
    }

    private static Set<InstanceIdentifier<?>> prefixHandledChildren() {
        final InstanceIdentifier<Prefix> prefixIID = InstanceIdentifier.create(Prefix.class);
        return ImmutableSet.of(prefixIID.augmentation(ControlAdvPrefixesVppAugmentation.class));
    }
}
