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

package io.fd.hc2vpp.routing.naming;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.trait.RouteMapper;
import io.fd.hc2vpp.routing.write.trait.RouteRequestProducer;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.jvpp.core.dto.IpFibDetails;
import io.fd.jvpp.core.types.FibPath;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipv4.unicast.routing.rev180319.VppIpv4NextHopAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.next.hop.NextHop1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.next.hop.list.NextHop;

public final class Ipv4RouteNamesFactory implements RouteMapper, RouteRequestProducer {

    private static final String DOT = ".";
    private static final String DASH = "-";
    private static final String UNDERSCORE = "_";

    private final NamingContext interfaceContext;
    private final NamingContext routingProtocolContext;

    public Ipv4RouteNamesFactory(@Nonnull final NamingContext interfaceContext,
                                 @Nonnull final NamingContext routingProtocolContext) {
        this.interfaceContext = interfaceContext;
        this.routingProtocolContext = routingProtocolContext;
    }

    /**
     * Construct unique name from provided {@code Route}
     */
    public String uniqueRouteName(@Nonnull final String parentProtocolName, @Nonnull final Route route) {
        return bindName(parentProtocolName, dotlessAddress(route.getDestinationPrefix()),
                        String.valueOf(extractPrefix(route.getDestinationPrefix())));
    }

    /**
     * Construct unique name from provided {@code IpFibDetails}
     */
    public String uniqueRouteName(@Nonnull final IpFibDetails details, @Nonnull final MappingContext mappingContext) {
        return bindName(routingProtocolContext.getName(details.tableId, mappingContext),
                        dotlessAddress(details.address), String.valueOf(details.addressLength));
    }

    /**
     * Construct unique name from provided parentProtocolName and {@code Ipv4Prefix}
     */
    public String uniqueRouteName(@Nonnull final String parentProtocolName, @Nonnull final Ipv4Prefix prefix) {
        return bindName(parentProtocolName, dotlessAddress(prefix),
                String.valueOf(Byte.toUnsignedInt(extractPrefix(prefix))));
    }

    public Ipv4Prefix ipv4PrefixFromUniqueRouteName(@Nonnull final String uniqueName) {
        String[] parts = uniqueName.split(UNDERSCORE);
        return new Ipv4Prefix(dotted(parts[1]) + "/" + parts[2]);
    }

    public String uniqueRouteHopName(@Nonnull final NextHop hop) {
        return bindName(hop.getOutgoingInterface(),
                        dotlessAddress(hop.augmentation(NextHop1.class).getNextHopAddress()),
                        String.valueOf(hop.augmentation(VppIpv4NextHopAugmentation.class).getWeight()));
    }

    public String uniqueRouteHopName(@Nonnull final FibPath path, @Nonnull final MappingContext mappingContext) {
        return bindName(interfaceContext.getName(path.swIfIndex, mappingContext), dotlessAddress(path.nextHop),
                        String.valueOf(path.weight));
    }

    private String dotlessAddress(final byte[] address) {
        // trimming in case of ipv4 address beeing sent as 16 byte array
        byte[] trimmed = address;
        if (trimmed.length > 4) {
            trimmed = Arrays.copyOfRange(trimmed, 0, 4);
        }

        //no reverting, just takes address as it is and converts it
        try {
            return dotless(InetAddress.getByAddress(trimmed).getHostAddress());
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String dotlessAddress(@Nonnull final Ipv4Prefix address) {
        final String addressValue = address.getValue();
        return dotless(addressValue.substring(0, addressValue.indexOf("/")));
    }

    private String dotlessAddress(@Nonnull final Ipv4Address address) {
        return dotless(address.getValue());
    }

    private String dotless(@Nonnull final String input) {
        return input.replace(DOT, DASH);
    }

    private String dotted(@Nonnull final String input) {
        return input.replace(DASH, DOT);
    }
}
