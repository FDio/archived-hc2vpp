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

import static io.fd.hc2vpp.routing.trait.RouteMapper.isDefaultInterfaceIndex;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.naming.Ipv4RouteNamesFactory;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.vpp.jvpp.core.types.FibPath;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.NextHopOptions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SimpleNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SimpleNextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SpecialNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SpecialNextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.NextHopListBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.next.hop.list.NextHopBuilder;

public interface Ipv4RoutePathParser extends RouteMapper {

    static NextHopOptions resolveOption(final String routeName,
                                        final List<FibPath> parsedHops,
                                        final NamingContext interfaceContext,
                                        final MultiNamingContext routeHopContext,
                                        final MappingContext mappingContext,
                                        final Ipv4RouteNamesFactory namesFactory) {
        return parsedHops.size() == 1
                ? RouteMapper.INSTANCE.isSpecialHop(parsedHops.get(0))
                ? specialHop(parsedHops.get(0))
                : simpleHop(parsedHops.get(0), interfaceContext, mappingContext)
                : hopList(routeName, parsedHops, interfaceContext, routeHopContext, mappingContext, namesFactory);
    }

    static SpecialNextHop specialHop(final FibPath singlePath) {
        return new SpecialNextHopBuilder()
                .setSpecialNextHop(RouteMapper.INSTANCE.specialHopType(singlePath))
                .build();
    }

    static SimpleNextHop simpleHop(final FibPath path, final NamingContext interfaceContext,
                                   final MappingContext mappingContext) {
        return resolveInterfaceIfSpecified(new SimpleNextHopBuilder(), path.swIfIndex, interfaceContext, mappingContext)
                .setNextHop(AddressTranslator.INSTANCE.arrayToIpv4AddressNoZone(path.nextHop))
                .build();
    }

    static SimpleNextHopBuilder resolveInterfaceIfSpecified(final SimpleNextHopBuilder builder, final int index,
                                                            final NamingContext interfaceContext,
                                                            final MappingContext mappingContext) {
        if (!isDefaultInterfaceIndex(index)) {
            builder.setOutgoingInterface(interfaceContext.getName(index, mappingContext));
        }
        return builder;
    }

    static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.NextHopList hopList(
            final String routeName,
            final List<FibPath> parsedHops,
            final NamingContext interfaceContext,
            final MultiNamingContext routeHopContext,
            final MappingContext mappingContext,
            final Ipv4RouteNamesFactory namesFactory) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.NextHopListBuilder()
                .setNextHopList(
                        new NextHopListBuilder().setNextHop(parsedHops.stream()
                                .map(fibPath -> resolveInterfaceIfSpecified(new NextHopBuilder(), fibPath.swIfIndex,
                                        interfaceContext, mappingContext)
                                        .setId((long) (routeHopContext.getChildIndex(routeName,
                                                namesFactory.uniqueRouteHopName(fibPath, mappingContext),
                                                mappingContext)))
                                        .setWeight(((short) fibPath.weight))
                                        .setAddress(
                                                AddressTranslator.INSTANCE
                                                        .arrayToIpv4AddressNoZone(fibPath.nextHop))
                                        .build())
                                .collect(Collectors.toList()))
                                .build())
                .build();
    }

    static NextHopBuilder resolveInterfaceIfSpecified(final NextHopBuilder builder, final int index,
                                                      final NamingContext interfaceContext,
                                                      final MappingContext mappingContext) {
        if (!isDefaultInterfaceIndex(index)) {
            builder.setOutgoingInterface(interfaceContext.getName(index, mappingContext));
        }
        return builder;
    }

    default NextHopOptions resolveHopType(@Nonnull final String routeName,
                                          final List<FibPath> parsedHops,
                                          @Nonnull final NamingContext interfaceContext,
                                          @Nonnull final MultiNamingContext routeHopContext,
                                          @Nonnull final MappingContext mappingContext,
                                          @Nonnull final Ipv4RouteNamesFactory namesFactory) {

        return parsedHops == null || parsedHops.isEmpty()
                ? null
                : resolveOption(routeName, parsedHops, interfaceContext, routeHopContext, mappingContext, namesFactory);
    }
}
