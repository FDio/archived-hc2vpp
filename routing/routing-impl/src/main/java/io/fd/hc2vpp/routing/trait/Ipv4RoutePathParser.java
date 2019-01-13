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
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipv4.unicast.routing.rev180319.VppIpv4NextHopAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipv4.unicast.routing.rev180319.VppIpv4NextHopAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.next.hop.NextHop1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.next.hop.NextHop1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.next.hop.SimpleNextHop1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.next.hop.SimpleNextHop1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.NextHopOptions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.NextHopList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.NextHopListBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.SimpleNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.SimpleNextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.SpecialNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.SpecialNextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.TableLookupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.next.hop.list.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.next.hop.list.NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.VniReference;

public interface Ipv4RoutePathParser extends RouteMapper {

    static NextHopOptions resolveOption(final String routeName,
                                        final List<FibPath> parsedHops,
                                        final NamingContext interfaceContext,
                                        final MultiNamingContext routeHopContext,
                                        final MappingContext mappingContext,
                                        final Ipv4RouteNamesFactory namesFactory) {
        if (parsedHops.size() == 1) {
            final FibPath path = parsedHops.get(0);
            if (RouteMapper.INSTANCE.isTableLookup(path)) return tableLookup();
            if (RouteMapper.INSTANCE.isSpecialHop(path)) return specialHop(path);
            return simpleHop(path, interfaceContext, mappingContext);
        }
        return hopList(routeName, parsedHops, interfaceContext, routeHopContext, mappingContext, namesFactory);
    }

    static NextHopOptions tableLookup() {
        return new TableLookupCaseBuilder()
                // TODO - https://jira.fd.io/browse/VPP-994
                .setSecondaryVrf(new VniReference(0L))
                .setAddressFamily(Ipv4.class)
                .build();
    }

    static SpecialNextHop specialHop(final FibPath singlePath) {
        return new SpecialNextHopBuilder()
                .setSpecialNextHopEnum(RouteMapper.INSTANCE.specialHopType(singlePath))
                .build();
    }

    static SimpleNextHop simpleHop(final FibPath path, final NamingContext interfaceContext,
                                   final MappingContext mappingContext) {
        return resolveInterfaceIfSpecified(new SimpleNextHopBuilder(), path.swIfIndex, interfaceContext, mappingContext)
                .addAugmentation(SimpleNextHop1.class, new SimpleNextHop1Builder()
                    .setNextHopAddress(AddressTranslator.INSTANCE.arrayToIpv4AddressNoZone(path.nextHop))
                    .build())
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

    static NextHopList hopList(final String routeName, final List<FibPath> parsedHops,
                               final NamingContext interfaceContext, final MultiNamingContext routeHopContext,
                               final MappingContext mappingContext, final Ipv4RouteNamesFactory namesFactory) {

        return new NextHopListBuilder().setNextHopList(
            parseNextHopList(routeName, parsedHops, interfaceContext, routeHopContext, mappingContext, namesFactory))
            .build();
    }

    static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.NextHopList parseNextHopList(
        String routeName, List<FibPath> parsedHops, NamingContext interfaceContext, MultiNamingContext routeHopContext,
        MappingContext mappingContext, Ipv4RouteNamesFactory namesFactory) {

        List<NextHop> nextHops = parsedHops.stream()
            .map(fibPath -> resolveInterfaceIfSpecified(new NextHopBuilder(), fibPath.swIfIndex, interfaceContext,
                                                        mappingContext).setIndex(
                getRouteIndex(routeName, routeHopContext, mappingContext, namesFactory, fibPath))
                .addAugmentation(NextHop1.class, new NextHop1Builder().setNextHopAddress(
                    AddressTranslator.INSTANCE.arrayToIpv4AddressNoZone(fibPath.nextHop)).build())
                .addAugmentation(VppIpv4NextHopAugmentation.class,
                                 new VppIpv4NextHopAugmentationBuilder().setWeight((short) fibPath.weight).build())
                .build())
            .collect(Collectors.toList());

        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.NextHopListBuilder()
            .setNextHop(nextHops)
            .build();
    }

    static String getRouteIndex(String routeName, MultiNamingContext routeHopContext, MappingContext mappingContext,
                                Ipv4RouteNamesFactory namesFactory, FibPath fibPath) {
        if (routeName.contains("learned-route")) {
            routeHopContext.addChild(routeName, namesFactory.uniqueRouteHopName(fibPath, mappingContext),
                                     mappingContext);
        }
        return String.valueOf(
            routeHopContext.getChildIndex(routeName, namesFactory.uniqueRouteHopName(fibPath, mappingContext),
                                          mappingContext));
    }

    static NextHopBuilder resolveInterfaceIfSpecified(final NextHopBuilder builder, final int index,
                                                      final NamingContext interfaceContext,
                                                      final MappingContext mappingContext) {
        if (!isDefaultInterfaceIndex(index)) {
            builder.setOutgoingInterface(interfaceContext.getName(index, mappingContext));
        }
        return builder;
    }

    default org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.NextHop resolveHopType(
        @Nonnull final String routeName, final List<FibPath> parsedHops, @Nonnull final NamingContext interfaceContext,
        @Nonnull final MultiNamingContext routeHopContext, @Nonnull final MappingContext mappingContext,
        @Nonnull final Ipv4RouteNamesFactory namesFactory) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.NextHopBuilder
            nextHopBuilder = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.NextHopBuilder();

        return parsedHops == null || parsedHops.isEmpty() ? null : nextHopBuilder.setNextHopOptions(
            resolveOption(routeName, parsedHops, interfaceContext, routeHopContext, mappingContext, namesFactory))
            .build();
    }
}
