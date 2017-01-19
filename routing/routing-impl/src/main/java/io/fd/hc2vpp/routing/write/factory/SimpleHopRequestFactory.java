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

package io.fd.hc2vpp.routing.write.factory;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.write.factory.base.BasicHopRequestFactory;
import io.fd.hc2vpp.routing.write.trait.RouteRequestProducer;
import io.fd.hc2vpp.v3po.vppclassifier.VppClassifierContextManager;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.VppIpv4Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SimpleNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.VppIpv6Route;


/**
 * Factory for creating requests to create route with hop simple hop
 */
public class SimpleHopRequestFactory extends BasicHopRequestFactory implements RouteRequestProducer {

    private SimpleHopRequestFactory(final VppClassifierContextManager classifierContextManager,
                                    final NamingContext interfaceContext,
                                    final NamingContext routingProtocolContext) {
        super(classifierContextManager, interfaceContext, routingProtocolContext);
    }

    public static SimpleHopRequestFactory forContexts(
            @Nonnull final VppClassifierContextManager classifierContextManager,
            @Nonnull final NamingContext interfaceContext,
            @Nonnull final NamingContext routingProtocolContext) {
        return new SimpleHopRequestFactory(classifierContextManager, interfaceContext, routingProtocolContext);
    }

    public IpAddDelRoute createIpv4SimpleHopRequest(final boolean add,
                                                    @Nonnull final String parentProtocolName,
                                                    @Nonnull final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.Route route,
                                                    @Nonnull final MappingContext mappingContext) {

        final SimpleNextHop hop = SimpleNextHop.class.cast(route.getNextHopOptions());
        final VppIpv4Route routingAttributes = route.getVppIpv4Route();
        final int nextHopInterfaceIndex =
                getInterfaceNamingContext().getIndex(hop.getOutgoingInterface(), mappingContext);

        if (routingAttributes != null &&
                classifyTablePresent(routingAttributes.getClassifyTable(), getVppClassifierContextManager(),
                        mappingContext)) {
            return getSimpleHopRequest(add,
                    route.getDestinationPrefix(),
                    nextHopInterfaceIndex,
                    hop.getNextHop(),
                    getRoutingProtocolContext().getIndex(parentProtocolName, mappingContext),
                    optionalVni(routingAttributes.getSecondaryVrf()),
                    classifyTableIndex(routingAttributes.getClassifyTable(), getVppClassifierContextManager(),
                            mappingContext),
                    true);
        } else {
            return getSimpleHopRequest(add,
                    route.getDestinationPrefix(),
                    nextHopInterfaceIndex,
                    hop.getNextHop(),
                    getRoutingProtocolContext().getIndex(parentProtocolName, mappingContext),
                    optionalVni(null),
                    0,
                    false);
        }
    }

    public IpAddDelRoute createIpv6SimpleHopRequest(final boolean add,
                                                    @Nonnull final String parentProtocolName,
                                                    @Nonnull final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.Route route,
                                                    @Nonnull final MappingContext mappingContext) {
        final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.SimpleNextHop
                hop =
                (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.SimpleNextHop) route
                        .getNextHopOptions();
        final VppIpv6Route routingAttributes = route.getVppIpv6Route();
        final int nextHopInterfaceIndex =
                getInterfaceNamingContext().getIndex(hop.getOutgoingInterface(), mappingContext);

        if (routingAttributes != null &&
                classifyTablePresent(routingAttributes.getClassifyTable(), getVppClassifierContextManager(),
                        mappingContext)) {
            return getSimpleHopRequest(add,
                    route.getDestinationPrefix(),
                    nextHopInterfaceIndex,
                    hop.getNextHop(),
                    getRoutingProtocolContext().getIndex(parentProtocolName, mappingContext),
                    optionalVni(routingAttributes.getSecondaryVrf()),
                    classifyTableIndex(routingAttributes.getClassifyTable(), getVppClassifierContextManager(),
                            mappingContext),
                    true);
        } else {
            return getSimpleHopRequest(add,
                    route.getDestinationPrefix(),
                    nextHopInterfaceIndex,
                    hop.getNextHop(),
                    getRoutingProtocolContext().getIndex(parentProtocolName, mappingContext),
                    optionalVni(null),
                    0,
                    false);
        }
    }


    private IpAddDelRoute getSimpleHopRequest(final boolean isAdd, @Nonnull final Ipv6Prefix destinationAddress,
                                              final int nextHopInterfaceIndex,
                                              @Nonnull final Ipv6Address nextHopAddress,
                                              final int primaryVrf, final int secondaryVrf,
                                              final int classifyTableIndex, final boolean classifyIndexSet) {
        return flaglessAddDelRouteRequest(booleanToByte(isAdd), nextHopInterfaceIndex,
                ipv6AddressNoZoneToArray(nextHopAddress), DEFAULT_HOP_WEIGHT, BYTE_TRUE,
                ipv6AddressPrefixToArray(destinationAddress), extractPrefix(destinationAddress.getValue()), BYTE_FALSE,
                primaryVrf, secondaryVrf, classifyTableIndex, booleanToByte(classifyIndexSet));
    }

    private IpAddDelRoute getSimpleHopRequest(final boolean isAdd, @Nonnull final Ipv4Prefix destinationAddress,
                                              final int nextHopInterfaceIndex,
                                              @Nonnull final Ipv4Address nextHopAddress,
                                              final int primaryVrf, final int secondaryVrf,
                                              final int classifyTableIndex, final boolean classifyIndexSet) {
        return flaglessAddDelRouteRequest(booleanToByte(isAdd), nextHopInterfaceIndex,
                ipv4AddressNoZoneToArray(nextHopAddress.getValue()), DEFAULT_HOP_WEIGHT, BYTE_FALSE,
                ipv4AddressPrefixToArray(destinationAddress), extractPrefix(destinationAddress.getValue()), BYTE_FALSE,
                primaryVrf, secondaryVrf, classifyTableIndex, booleanToByte(classifyIndexSet));
    }
}
