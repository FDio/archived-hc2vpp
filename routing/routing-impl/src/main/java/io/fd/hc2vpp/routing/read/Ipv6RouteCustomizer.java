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
import io.fd.hc2vpp.routing.naming.Ipv6RouteNamesFactory;
import io.fd.hc2vpp.routing.trait.Ipv6RoutePathParser;
import io.fd.hc2vpp.routing.trait.RouteMapper;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.core.dto.Ip6FibDetails;
import io.fd.vpp.jvpp.core.dto.Ip6FibDetailsReplyDump;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.RouteKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.VppIpv6RouteStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv6RouteCustomizer
        implements ListReaderCustomizer<Route, RouteKey, RouteBuilder>, RouteMapper, Ipv6RoutePathParser {

    private final DumpCacheManager<Ip6FibDetailsReplyDump, Void> ipv6RoutesDumpManager;
    private final RoutingConfiguration configuration;
    private final MultiNamingContext routeHopContext;
    private final NamingContext interfaceContext;
    private final NamingContext routesContext;
    private final NamingContext routingProtocolContext;
    private final Ipv6RouteNamesFactory namesFactory;

    public Ipv6RouteCustomizer(@Nonnull final DumpCacheManager<Ip6FibDetailsReplyDump, Void> ipv6RoutesDumpManager,
                               @Nonnull final RoutingConfiguration configuration,
                               @Nonnull final MultiNamingContext routeHopContext,
                               @Nonnull final NamingContext interfaceContext,
                               @Nonnull final NamingContext routesContext,
                               @Nonnull final NamingContext routingProtocolContext) {
        this.ipv6RoutesDumpManager = ipv6RoutesDumpManager;
        this.configuration = configuration;
        this.interfaceContext = interfaceContext;
        this.routeHopContext = routeHopContext;
        this.routesContext = routesContext;
        this.routingProtocolContext = routingProtocolContext;
        this.namesFactory = new Ipv6RouteNamesFactory(interfaceContext, routingProtocolContext);
    }

    @Nonnull
    @Override
    public List<RouteKey> getAllIds(@Nonnull final InstanceIdentifier<Route> instanceIdentifier,
                                    @Nonnull final ReadContext readContext) throws ReadFailedException {

        final Optional<Ip6FibDetailsReplyDump> ipv6RoutesDump =
                ipv6RoutesDumpManager.getDump(instanceIdentifier, readContext.getModificationCache());

        final String protocolName = instanceIdentifier.firstKeyOf(RoutingProtocol.class).getName();
        final int protocolTableId = routingProtocolContext.getIndex(protocolName, readContext.getMappingContext());

        return ipv6RoutesDump.isPresent()
                ? ipv6RoutesDump.get().ip6FibDetails.stream()
                .filter(details -> protocolTableId == details.tableId)
                .map(ip6FibDetails -> toKey(ip6FibDetails, readContext.getMappingContext()))
                .collect(Collectors.toList())
                : Collections.emptyList();
    }

    /**
     * route id is represented as number, but there's no index in dumped data,
     * so index is assigned to name formatted as tableId_address_addressLength(should be unique combination)
     */
    private RouteKey toKey(final Ip6FibDetails details, final MappingContext mappingContext) {
        String routeName = namesFactory.uniqueRouteName(details, mappingContext);
        // first condition excludes data written manually, second one data that has been already learned
        if (!routesContext.containsIndex(routeName, mappingContext)) {
            String learnedRouteName = nameWithPrefix(configuration.getLearnedRouteNamePrefix(), routeName);
            if (!routesContext.containsIndex(learnedRouteName, mappingContext)) {
                routesContext.addName(learnedRouteName, mappingContext);
            }
            return keyForName(mappingContext, learnedRouteName);
        }
        return keyForName(mappingContext, routeName);
    }

    private RouteKey keyForName(final MappingContext mappingContext, final String name) {
        return new RouteKey(Long.valueOf(routesContext.getIndex(name, mappingContext)));
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<Route> list) {
        Ipv6Builder.class.cast(builder).setRoute(list);
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
        final String mappedName = routesContext.getName(key.getId().intValue(), readContext.getMappingContext());
        final String protocolName = instanceIdentifier.firstKeyOf(RoutingProtocol.class).getName();
        final int protocolTableId = routingProtocolContext.getIndex(protocolName, readContext.getMappingContext());
        final Optional<Ip6FibDetailsReplyDump> ipv6RoutesDump =
                ipv6RoutesDumpManager.getDump(instanceIdentifier, readContext.getModificationCache());

        if (ipv6RoutesDump.isPresent() && !ipv6RoutesDump.get().ip6FibDetails.isEmpty()) {

            final java.util.Optional<Ip6FibDetails> opDetail = ipv6RoutesDump.get().ip6FibDetails.stream()
                    .filter(details -> protocolTableId == details.tableId)
                    .filter(details -> equalsWithConfigOrLearned(configuration.getLearnedRouteNamePrefix(), mappedName,
                            namesFactory.uniqueRouteName(details, readContext.getMappingContext())))
                    .findAny();

            if (opDetail.isPresent()) {
                final Ip6FibDetails detail = opDetail.get();

                routeBuilder.setNextHopOptions(
                        resolveHopType(mappedName, Arrays.asList(detail.path), interfaceContext, routeHopContext,
                                readContext.getMappingContext(), namesFactory))
                        .setKey(key)
                        .setId(key.getId())
                        .setDestinationPrefix(toIpv6Prefix(detail.address, toJavaByte(detail.addressLength)))
                        .setVppIpv6RouteState(new VppIpv6RouteStateBuilder().build());
            }
        }
    }
}
