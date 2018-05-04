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

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.write.factory.base.BasicHopRequestFactory;
import io.fd.hc2vpp.routing.write.trait.RouteRequestProducer;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.SpecialNextHop;

public class SpecialNextHopRequestFactory extends BasicHopRequestFactory
        implements RouteRequestProducer {

    private SpecialNextHopRequestFactory(final VppClassifierContextManager classifierContextManager,
                                         final NamingContext interfaceContext,
                                         final NamingContext routingProtocolContext) {
        super(classifierContextManager, interfaceContext, routingProtocolContext);
    }

    public static SpecialNextHopRequestFactory forContexts(
            @Nonnull final VppClassifierContextManager classifierContextManager,
            @Nonnull final NamingContext interfaceContext,
            @Nonnull final NamingContext routingProtocolContext) {
        return new SpecialNextHopRequestFactory(classifierContextManager, interfaceContext, routingProtocolContext);
    }

    public IpAddDelRoute createIpv4SpecialHopRequest(final boolean add,
                                                     @Nonnull final String parentProtocolName,
                                                     @Nonnull final Route route,
                                                     @Nonnull final MappingContext mappingContext,
                                                     @Nonnull final SpecialNextHop.SpecialNextHopEnum flagsVariant) {
        checkNotNull(route, "Route cannot be null");
        checkNotNull(mappingContext, "Mapping Context cannot be null");
        checkNotNull(flagsVariant, "Flags variant cannot be null");

        final int parentProtocolTableId = getRoutingProtocolContext().getIndex(parentProtocolName, mappingContext);
        return resolveFlags(getSpecialHopRequest(add, route.getDestinationPrefix(), (byte) parentProtocolTableId, DEFAULT_VNI), flagsVariant);
    }

    public IpAddDelRoute createIpv6SpecialHopRequest(final boolean add,
                                                     @Nonnull final String parentProtocolName,
                                                     @Nonnull final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.Route route,
                                                     @Nonnull final MappingContext mappingContext,
                                                     @Nonnull final SpecialNextHop.SpecialNextHopEnum flagsVariant) {

        checkNotNull(route, "Route cannot be null");
        checkNotNull(mappingContext, "Mapping Context cannot be null");
        checkNotNull(flagsVariant, "Flags variant cannot be null");

        final int parentProtocolTableId = getRoutingProtocolContext().getIndex(parentProtocolName, mappingContext);
        return resolveFlags(getSpecialHopRequest(add, route.getDestinationPrefix(), (byte) parentProtocolTableId, DEFAULT_VNI), flagsVariant);
    }

    private IpAddDelRoute getSpecialHopRequest(final boolean isAdd, @Nonnull final Ipv6Prefix destinationAddress,
                                               final byte primaryTableId, final byte secondaryTableId) {

        return flaglessAddDelRouteRequest(booleanToByte(isAdd), 0, null, DEFAULT_HOP_WEIGHT, BYTE_TRUE,
                ipv6AddressPrefixToArray(destinationAddress), extractPrefix(destinationAddress.getValue()), BYTE_FALSE,
                primaryTableId, secondaryTableId, DEFAULT_CLASSIFY_TABLE_INDEX, BYTE_FALSE);
    }

    private IpAddDelRoute getSpecialHopRequest(final boolean isAdd, @Nonnull final Ipv4Prefix destinationAddress,
                                               final byte primaryTableId, final byte secondaryTableId) {
        return flaglessAddDelRouteRequest(booleanToByte(isAdd), 0, null, DEFAULT_HOP_WEIGHT, BYTE_FALSE,
                ipv4AddressPrefixToArray(destinationAddress), extractPrefix(destinationAddress.getValue()), BYTE_FALSE,
                primaryTableId, secondaryTableId, DEFAULT_CLASSIFY_TABLE_INDEX, BYTE_FALSE);
    }

    private IpAddDelRoute resolveFlags(IpAddDelRoute request,
                                       final SpecialNextHop.SpecialNextHopEnum flagsVariant) {
        switch (flagsVariant) {
            case Blackhole:
                return resolveAsBlackholeVariant(request);
            case Unreachable:
                return resolveAsUnreachableVariant(request);
            case Prohibit:
                return resolveAsProhibitedVariant(request);
            case Receive:
                return resolveAsReceiveVariant(request);
            default:
                throw new IllegalArgumentException("Unsupported type");
        }
    }

    private IpAddDelRoute resolveAsBlackholeVariant(IpAddDelRoute request) {
        return bindFlags(request, true, false, false, false);
    }

    private IpAddDelRoute resolveAsReceiveVariant(IpAddDelRoute request) {
        return bindFlags(request, false, true, false, false);
    }

    private IpAddDelRoute resolveAsUnreachableVariant(IpAddDelRoute request) {
        return bindFlags(request, false, false, true, false);
    }

    private IpAddDelRoute resolveAsProhibitedVariant(IpAddDelRoute request) {
        return bindFlags(request, false, false, false, true);
    }

    private IpAddDelRoute bindFlags(IpAddDelRoute request, final boolean isDrop, final boolean isReceive,
                                    final boolean isUnreachable, final boolean isProhibited) {
        request.isDrop = booleanToByte(isDrop);
        request.isLocal = booleanToByte(isReceive);
        request.isUnreach = booleanToByte(isUnreachable);
        request.isProhibit = booleanToByte(isProhibited);

        return request;
    }
}
