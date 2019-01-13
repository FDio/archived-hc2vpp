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

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.RoutingConfiguration;
import io.fd.hc2vpp.routing.naming.Ipv4RouteNamesFactory;
import io.fd.hc2vpp.routing.trait.Ipv4RoutePathParser;
import io.fd.hc2vpp.routing.trait.RouteMapper;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.core.dto.IpFibDetails;
import io.fd.vpp.jvpp.core.dto.IpFibDetailsReplyDump;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipv4.unicast.routing.rev180319.VppIpv4RouteAttributesAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipv4.unicast.routing.rev180319.VppIpv4RouteAttributesAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipv4.unicast.routing.rev180319.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.VppIpv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.RouteKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocol;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class Ipv4RouteCustomizer
        implements ListReaderCustomizer<Route, RouteKey, RouteBuilder>, RouteMapper, Ipv4RoutePathParser {

    private final DumpCacheManager<IpFibDetailsReplyDump, Void> ipv4RoutesDumpManager;
    private final RoutingConfiguration configuration;
    private final MultiNamingContext routeHopContext;
    private final NamingContext interfaceContext;
    private final NamingContext routesContext;
    private final NamingContext routingProtocolContext;

    private final Ipv4RouteNamesFactory namesFactory;

    Ipv4RouteCustomizer(@Nonnull final DumpCacheManager<IpFibDetailsReplyDump, Void> ipv4RoutesDumpManager,
                        @Nonnull final RoutingConfiguration configuration,
                        @Nonnull final MultiNamingContext routeHopContext,
                        @Nonnull final NamingContext interfaceContext,
                        @Nonnull final NamingContext routesContext,
                        @Nonnull final NamingContext routingProtocolContext) {
        this.ipv4RoutesDumpManager = ipv4RoutesDumpManager;
        this.configuration = configuration;
        this.interfaceContext = interfaceContext;
        this.routeHopContext = routeHopContext;
        this.routesContext = routesContext;
        this.routingProtocolContext = routingProtocolContext;
        this.namesFactory = new Ipv4RouteNamesFactory(interfaceContext, routingProtocolContext);
    }

    @Nonnull
    @Override
    public List<RouteKey> getAllIds(@Nonnull final InstanceIdentifier<Route> instanceIdentifier,
                                    @Nonnull final ReadContext readContext) throws ReadFailedException {

        final Optional<IpFibDetailsReplyDump> ipv4RoutesDump =
                ipv4RoutesDumpManager.getDump(instanceIdentifier, readContext.getModificationCache());

        final String protocolName = instanceIdentifier.firstKeyOf(ControlPlaneProtocol.class).getName();
        final int protocolTableId = routingProtocolContext.getIndex(protocolName, readContext.getMappingContext());

        return ipv4RoutesDump.isPresent()
                ? ipv4RoutesDump.get().ipFibDetails.stream()
                .filter(details -> protocolTableId == details.tableId)
                .map(ipFibDetails -> toKey(ipFibDetails, readContext.getMappingContext()))
                .collect(Collectors.toList())
                : Collections.emptyList();
    }

    /**
     * route id is represented as number, but there's no index in dumped data,
     * so index is assigned to name formatted as tableId_address_addressLength(should be unique combination)
     */
    private RouteKey toKey(final IpFibDetails details, final MappingContext mappingContext) {
        String routeName = namesFactory.uniqueRouteName(details, mappingContext);
        // first condition excludes data written manually, second one data that has been already learned
        if (!routesContext.containsIndex(routeName, mappingContext)) {
            String learnedRouteName = nameWithPrefix(configuration.getLearnedRouteNamePrefix(), routeName);
            if (!routesContext.containsIndex(learnedRouteName, mappingContext)) {
                routesContext.addName(learnedRouteName, mappingContext);
            }
            return keyForLearnedName(learnedRouteName);
        }
        return keyForName(routeName);
    }

    private RouteKey keyForName(final String name) {
        // example learned-protocol-0_0-0-0-0_0
        return new RouteKey(namesFactory.ipv4PrefixFromUniqueRouteName(name));
    }

    private RouteKey keyForLearnedName(final String name) {
        // example learned-route_learned-protocol-0_0-0-0-0_0
        return new RouteKey(namesFactory.ipv4PrefixFromUniqueRouteName(name.replace("learned-route_", "")));
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<Route> list) {
        Ipv4Builder.class.cast(builder).setRoute(list);
    }

    @Nonnull
    @Override
    public RouteBuilder getBuilder(@Nonnull final InstanceIdentifier<Route> instanceIdentifier) {
        return new RouteBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Route> instanceIdentifier,
                                      @Nonnull final RouteBuilder routeBuilder, @Nonnull final ReadContext readContext)
            throws ReadFailedException {
        final RouteKey key = instanceIdentifier.firstKeyOf(Route.class);
        final String protocolName = instanceIdentifier.firstKeyOf(ControlPlaneProtocol.class).getName();
        final String mappedName = namesFactory.uniqueRouteName(protocolName, key.getDestinationPrefix());

        final int protocolTableId = routingProtocolContext.getIndex(protocolName, readContext.getMappingContext());
        final Optional<IpFibDetailsReplyDump> ipv4RoutesDump =
                ipv4RoutesDumpManager.getDump(instanceIdentifier, readContext.getModificationCache());

        if (ipv4RoutesDump.isPresent() && !ipv4RoutesDump.get().ipFibDetails.isEmpty()) {

            final java.util.Optional<IpFibDetails> opDetail = ipv4RoutesDump.get().ipFibDetails.stream()
                    .filter(details -> protocolTableId == details.tableId)
                    .filter(details -> equalsWithConfigOrLearned(configuration.getLearnedRouteNamePrefix(), mappedName,
                            namesFactory.uniqueRouteName(details, readContext.getMappingContext())))
                    .findAny();

            if (opDetail.isPresent()) {
                final IpFibDetails detail = opDetail.get();

                routeBuilder.setNextHop(
                        resolveHopType(mappedName, Arrays.asList(detail.path), interfaceContext, routeHopContext,
                                readContext.getMappingContext(), namesFactory))
                        .withKey(key)
                        .setDestinationPrefix(toIpv4Prefix(detail.address, toJavaByte(detail.addressLength)))
                        .addAugmentation(VppIpv4RouteAttributesAugmentation.class,
                                         new VppIpv4RouteAttributesAugmentationBuilder()
                                             .setVppIpv4Route(new VppIpv4RouteBuilder()
                                                                  .build())
                                             .build());
            }
        }
    }
}
