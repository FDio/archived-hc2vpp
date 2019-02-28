/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.bgp.prefix.sid;

import static com.google.common.base.Preconditions.checkArgument;
import static io.fd.hc2vpp.bgp.prefix.sid.MplsRouteRequestProducer.MPLS_LABEL_INVALID;

import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.MplsLabelTranslator;
import io.fd.jvpp.core.dto.IpAddDelRoute;
import io.fd.jvpp.core.types.FibMplsLabel;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.list.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCase;

interface IpRouteRequestProducer {
    /**
     * Produces {@link IpAddDelRoute} request that imposes MPLS label received via BGP LU on packets
     * destined to the prefix the label was assigned to.
     *
     * @param route BPG LU route received via BGP
     * @param isAdd determines whether to produce request for adding or removing VPP config
     * @return jVpp request for updating VPP IP FIB.
     * @see <a href="https://tools.ietf.org/html/rfc3107">Carrying Label Information in BGP-4</a>
     * @see <a href="https://tools.ietf.org/html/draft-ietf-idr-bgp-prefix-sid-07#page-10">BGP Prefix SID: programming
     * outgoing label</a>
     * @see <a href="https://tools.ietf.org/html/draft-ietf-spring-segment-routing-msdc-08#section-4.2.2">FIB example
     * for SR in the DC usecase</a>
     */
    default IpAddDelRoute ipAddDelRouteFor(@Nonnull final LabeledUnicastRoute route, final boolean isAdd) {
        final IpAddDelRoute request = Impl.ipAddDelRoute(isAdd);
        Impl.translate(route.getPrefix(), request);
        Impl.translate(route.getAttributes().getCNextHop(), request);
        Impl.translate(route.getLabelStack(), request);
        return request;
    }

    final class Impl {
        private static IpAddDelRoute ipAddDelRoute(final boolean isAdd) {
            final IpAddDelRoute request = new IpAddDelRoute();
            request.isAdd = Ipv4Translator.INSTANCE.booleanToByte(isAdd);

            // default values based on inspecting VPP's CLI and make test code
            request.classifyTableIndex = -1;
            request.nextHopWeight = 1;
            request.nextHopViaLabel = MPLS_LABEL_INVALID;
            return request;
        }

        private static void translate(@Nonnull final IpPrefix prefix, final IpAddDelRoute request) {
            // BGP Prefix SID for v6 is not supported
            final Ipv4Prefix ipv4Prefix = prefix.getIpv4Prefix();
            checkArgument(ipv4Prefix != null, "Unsupported IpPrefix: %s, ipv4Prefix is missing.", prefix);
            request.dstAddressLength = Ipv4Translator.INSTANCE.extractPrefix(ipv4Prefix);
            request.dstAddress = Ipv4Translator.INSTANCE.ipv4AddressPrefixToArray(ipv4Prefix);
        }

        private static void translate(@Nonnull final CNextHop nextHop, @Nonnull final IpAddDelRoute request) {
            checkArgument(nextHop instanceof Ipv4NextHopCase, "only ipv4 next hop is supported, but was %s", nextHop);

            final Ipv4Address nextHopAddress = ((Ipv4NextHopCase) nextHop).getIpv4NextHop().getGlobal();
            request.nextHopAddress = Ipv4Translator.INSTANCE.ipv4AddressNoZoneToArray(nextHopAddress.getValue());

            // We create recursive route. In order to make everything work,
            // operator needs to manually map next hop address to proper interface.
            // Either via CLI or HC.
            //
            // VPP can't recursively resolve a route that has out labels via a route that does not have out labels.
            // Implicit null(3) label is trick to get around it (no more labels will be added to the package).
            // CLI example:
            //
            // ip route add <next-hop-prefix> via <next-hop-ifc> out-labels 3
            request.nextHopSwIfIndex = -1;
        }

        private static void translate(@Nonnull final List<LabelStack> labelStack,
                                      @Nonnull final IpAddDelRoute request) {
            // It is quite possible we could support multiple labels here, but it was never tested
            // so it is not supported currently.
            final int labelCount = labelStack.size();
            checkArgument(labelCount == 1, "Single label expected, but labelStack.size()==%s", labelCount);
            final int label = labelStack.get(0).getLabelValue().getValue().intValue();

            // TODO(HC2VPP-271): add support for special labels, e.g. implicit null (for PHP).

            // Push label received via BGP on packets destined to the prefix it was assigned to:
            request.nextHopOutLabelStack =  new FibMplsLabel[] {MplsLabelTranslator.INSTANCE.translate(label)};
            request.nextHopNOutLabels = 1;
        }
    }
}
