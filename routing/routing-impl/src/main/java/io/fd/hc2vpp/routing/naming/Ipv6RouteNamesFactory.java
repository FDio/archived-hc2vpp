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

import com.google.common.net.InetAddresses;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.trait.RouteMapper;
import io.fd.hc2vpp.routing.write.trait.RouteRequestProducer;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.vpp.jvpp.core.dto.Ip6FibDetails;
import io.fd.vpp.jvpp.core.types.FibPath;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.route.next.hop.NextHop1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.next.hop.list.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.ipv6.unicast.routing.rev180319.VppIpv6NextHopAugmentation;

public final class Ipv6RouteNamesFactory implements RouteMapper, RouteRequestProducer {

    private static final String COLON = ":";
    private static final String DASH = "-";
    private static final String UNDERSCORE = "_";

    private final NamingContext interfaceContext;
    private final NamingContext routingProtocolContext;

    public Ipv6RouteNamesFactory(@Nonnull final NamingContext interfaceContext,
                                 @Nonnull final NamingContext routingProtocolContext) {
        this.interfaceContext = interfaceContext;
        this.routingProtocolContext = routingProtocolContext;
    }

    /**
     * Construct unique name from provided {@code Route}
     */
    public String uniqueRouteName(@Nonnull final String parentProtocolName,
                                  @Nonnull final Route route) {
        return bindName(parentProtocolName,
                // to have address in compressed form
                doubleDotlessAddress(route.getDestinationPrefix()),
                route.getDestinationPrefix().getValue().substring(route.getDestinationPrefix().getValue().indexOf('/') + 1));
    }

    /**
     * Construct unique name from provided {@code IpFibDetails}
     */
    public String uniqueRouteName(@Nonnull final Ip6FibDetails details, @Nonnull final MappingContext mappingContext) {
        return bindName(routingProtocolContext.getName(details.tableId, mappingContext),
                doubleDotlessAddress(details.address),
                String.valueOf(Byte.toUnsignedInt(details.addressLength)));
    }

    /**
     * Construct unique name from provided parentProtocolName and {@code Ipv6Prefix}
     */
    public String uniqueRouteName(@Nonnull final String parentProtocolName, @Nonnull final Ipv6Prefix prefix) {
        return bindName(parentProtocolName, doubleDotlessAddress(prefix),
                prefix.getValue().substring(prefix.getValue().indexOf('/') + 1));
    }


    public Ipv6Prefix ipv6PrefixFromUniqueRouteName(@Nonnull final String uniqueName) {
        String[] parts = uniqueName.split(UNDERSCORE);
        return new Ipv6Prefix(coloned(parts[1]) + "/" + parts[2]);
    }

    public String uniqueRouteHopName(@Nonnull final NextHop hop) {
        return bindName(hop.getOutgoingInterface(),
                doubleDotlessAddress(hop.augmentation(NextHop1.class).getNextHopAddress()),
                String.valueOf(hop.augmentation(VppIpv6NextHopAugmentation.class).getWeight()));
    }

    public String uniqueRouteHopName(@Nonnull final FibPath path, @Nonnull final MappingContext mappingContext) {
        return bindName(interfaceContext.getName(path.swIfIndex, mappingContext),
                doubleDotlessAddress(path.nextHop),
                String.valueOf(path.weight));
    }

    /**
     * Uses combination of standard java.net.InetAddress and com.google.common.net.InetAddresses for following reasons
     * <ul>
     * <li>
     * InetAddresses.toAddrString uses maximal ipv6 compression - eliminate possibility of mismatch between same
     * addresses with different compression
     * </li>
     * <li>
     * InetAddress.getByAddress just converts byte array to address, that's
     * why InetAddresses.fromLittleEndianByteArray is not used, because it internaly reverts order of address
     * bytes,which is something that is not needed here
     * </li>
     * </ul>
     */
    private String doubleDotlessAddress(final byte[] address) {
        try {
            return doubleDotless(InetAddresses.toAddrString(InetAddress.getByAddress(address)));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String doubleDotlessAddress(@Nonnull final Ipv6Prefix address) {
        final String addressValue = address.getValue();
        return doubleDotless(compressedIpv6(addressValue.substring(0, addressValue.indexOf("/"))));
    }

    private String doubleDotlessAddress(@Nonnull final Ipv6Address address) {
        // converted to use maximal compression
        // for details - https://google.github.io/guava/releases/snapshot/api/docs/com/google/common/net/InetAddresses.html#toAddrString-java.net.InetAddress
        return doubleDotless(compressedIpv6(address.getValue()));
    }

    /**
     * Use maximal compresion of ipv6 address string
     */
    private String compressedIpv6(@Nonnull final String input) {
        return InetAddresses.toAddrString(InetAddresses.forString(input));
    }

    private String doubleDotless(@Nonnull final String input) {
        return input.replace(COLON, DASH);
    }

    private String coloned(@Nonnull final String input) {
        return input.replace(DASH, COLON);
    }
}
