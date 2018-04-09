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

package io.fd.hc2vpp.routing.trait;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.naming.Ipv6RouteNamesFactory;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.vpp.jvpp.core.types.FibPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.NextHopOptions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.NextHopList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.NextHopListBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.next.hop.list.NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.table.lookup.TableLookupParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.VniReference;


import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

import static io.fd.hc2vpp.routing.trait.RouteMapper.isDefaultInterfaceIndex;

public interface Ipv6RoutePathParser extends RouteMapper {

    static NextHopOptions resolveOption(final String routeName,
                                        final List<FibPath> parsedHops,
                                        final NamingContext interfaceContext,
                                        final MultiNamingContext routeHopContext,
                                        final MappingContext mappingContext,
                                        final Ipv6RouteNamesFactory namesFactory) {
        if (parsedHops.size() == 1) {
            final FibPath path = parsedHops.get(0);
            if (RouteMapper.INSTANCE.isTableLookup(path)) return tableLookup();
            if (RouteMapper.INSTANCE.isSpecialHop(path)) return specialHop(path);
            return simpleHop(path, interfaceContext, mappingContext);
        }
        return hopList(routeName, parsedHops, interfaceContext, routeHopContext, mappingContext, namesFactory);
    }

    static NextHopOptions tableLookup() {
        return new TableLookupBuilder()
                .setTableLookupParams(
                        new TableLookupParamsBuilder()
                                // TODO - https://jira.fd.io/browse/VPP-994
                                .setSecondaryVrf(new VniReference(0L))
                                .build()
                ).build();
    }

    static SpecialNextHop specialHop(final FibPath fibPath) {
        return new SpecialNextHopBuilder()
                .setSpecialNextHop(INSTANCE.specialHopType(fibPath))
                .build();
    }

    static SimpleNextHop simpleHop(final FibPath path, final NamingContext interfaceContext,
                                   final MappingContext mappingContext) {
        return resolveInterfaceIfSpecified(new SimpleNextHopBuilder(), path.swIfIndex, interfaceContext, mappingContext)
                .setNextHop(AddressTranslator.INSTANCE.arrayToIpv6AddressNoZone(path.nextHop))
                .build();
    }

    static SimpleNextHopBuilder resolveInterfaceIfSpecified(SimpleNextHopBuilder builder, final int index,
                                                            final NamingContext interfaceContext,
                                                            final MappingContext mappingContext) {
        if (!isDefaultInterfaceIndex(index)) {
            builder.setOutgoingInterface(interfaceContext.getName(index, mappingContext));
        }

        return builder;
    }

    static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.NextHopList hopList(
            final String routeName,
            final List<FibPath> parsedHops,
            final NamingContext interfaceContext,
            final MultiNamingContext routeHopContext,
            final MappingContext mappingContext,
            final Ipv6RouteNamesFactory namesFactory) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.NextHopListBuilder()
                .setNextHopList(
                        buildNextHopList(routeName, parsedHops, interfaceContext, routeHopContext, mappingContext,
                                namesFactory))
                .build();
    }

    static NextHopList buildNextHopList(final String routeName, final List<FibPath> parsedHops,
                                        final NamingContext interfaceContext,
                                        final MultiNamingContext routeHopContext,
                                        final MappingContext mappingContext,
                                        final Ipv6RouteNamesFactory namesFactory) {
        return new NextHopListBuilder().setNextHop(parsedHops.stream()
                .map(fibPath -> resolveInterfaceIfSpecified(new NextHopBuilder(), fibPath.swIfIndex, interfaceContext,
                        mappingContext)
                        .setId(findIdWithinRouteContext(routeName, routeHopContext, mappingContext, fibPath,
                                namesFactory))
                        .setWeight((short) fibPath.weight)
                        .setAddress(AddressTranslator.INSTANCE.arrayToIpv6AddressNoZone(fibPath.nextHop))
                        .build())
                .collect(Collectors.toList()))
                .build();
    }

    static NextHopBuilder resolveInterfaceIfSpecified(NextHopBuilder builder, final int index,
                                                      final NamingContext interfaceContext,
                                                      final MappingContext mappingContext) {
        if (!isDefaultInterfaceIndex(index)) {
            builder.setOutgoingInterface(interfaceContext.getName(index, mappingContext));
        }

        return builder;
    }

    static long findIdWithinRouteContext(final String routeName, final MultiNamingContext routeHopContext,
                                         final MappingContext mappingContext, final FibPath fibPath,
                                         final Ipv6RouteNamesFactory namesFactory) {
        return (long) (routeHopContext
                .getChildIndex(routeName, namesFactory.uniqueRouteHopName(fibPath, mappingContext), mappingContext));
    }

    default NextHopOptions resolveHopType(@Nonnull final String routeName,
                                          final List<FibPath> parsedHops,
                                          @Nonnull final NamingContext interfaceContext,
                                          @Nonnull final MultiNamingContext routeHopContext,
                                          @Nonnull final MappingContext mappingContext,
                                          @Nonnull final Ipv6RouteNamesFactory namesFactory) {

        return parsedHops == null || parsedHops.isEmpty()
                ? null
                : resolveOption(routeName, parsedHops, interfaceContext, routeHopContext, mappingContext, namesFactory);
    }
}
