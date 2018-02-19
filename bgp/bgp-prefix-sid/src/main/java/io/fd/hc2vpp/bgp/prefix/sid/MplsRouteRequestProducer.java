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

import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.MplsLabelTranslator;
import io.fd.vpp.jvpp.core.dto.MplsRouteAddDel;
import io.fd.vpp.jvpp.core.types.FibMplsLabel;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.LabelIndexTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.OriginatorSrgbTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.routes.list.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.originator.srgb.tlv.SrgbValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.BgpPrefixSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.bgp.prefix.sid.BgpPrefixSidTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.slf4j.Logger;

interface MplsRouteRequestProducer extends Ipv4Translator {
    /**
     * Constant used by VPP to disable optional parameters of mpls label type.
     */
    int MPLS_LABEL_INVALID = 0x100000;

    /**
     * Produces {@link MplsRouteAddDel} request for derived local label entry
     * that swaps it for the received outbound label.
     *
     * @param route BPG LU route received via BGP
     * @param isAdd determines whether to produce request for adding or removing VPP config
     * @return jVpp request for updating VPP MPLS FIB.
     * @see <a href="https://tools.ietf.org/html/rfc3107">Carrying Label Information in BGP-4</a>
     * @see <a href="https://tools.ietf.org/html/draft-ietf-idr-bgp-prefix-sid-07#page-10">BGP Prefix SID: programming
     * outgoing label</a>
     * @see <a href="https://tools.ietf.org/html/draft-ietf-spring-segment-routing-msdc-08#section-4.2.2">FIB example
     * for SR in the DC usecase</a>
     */
    default MplsRouteAddDel mplsRouteAddDelFor(@Nonnull final LabeledUnicastRoute route, final boolean isAdd,
                                               @Nonnull final Logger logger) {
        final MplsRouteAddDel request = Impl.mplsRouteAddDel(isAdd);
        Impl.translate(route.getAttributes().getCNextHop(), request);
        Impl.translate(route.getAttributes().getBgpPrefixSid(), request, logger);
        Impl.translate(route.getLabelStack(), request);
        return request;
    }

    final class Impl implements Ipv4Translator, MplsLabelTranslator {
        private static MplsRouteAddDel mplsRouteAddDel(final boolean isAdd) {
            final MplsRouteAddDel request = new MplsRouteAddDel();
            request.mrIsAdd = Ipv4Translator.INSTANCE.booleanToByte(isAdd);

            // default values based on inspecting VPP's CLI and make test code
            request.mrClassifyTableIndex = -1;
            request.mrNextHopWeight = 1;
            request.mrNextHopViaLabel = MPLS_LABEL_INVALID;
            return request;
        }

        private static void translate(@Nonnull final CNextHop nextHop, @Nonnull final MplsRouteAddDel request) {
            checkArgument(nextHop instanceof Ipv4NextHopCase, "only ipv4 next hop is supported, but was %s", nextHop);
            final Ipv4Address nextHopAddress = ((Ipv4NextHopCase) nextHop).getIpv4NextHop().getGlobal();
            request.mrNextHop = Ipv4Translator.INSTANCE.ipv4AddressNoZoneToArray(nextHopAddress.getValue());

            // We create recursive route. In order to make everything work,
            // operator needs to manually map next hop address to proper interface.
            // Either via CLI or HC.
            //
            // VPP can't recursively resolve a route that has out labels via a route that does not have out labels.
            // Implicit null(3) label is trick to get around it (no more labels will be added to the package).
            // CLI example:
            //
            // ip route add <next-hop-prefix> via <next-hop-ifc> out-labels 3
            request.mrNextHopSwIfIndex = -1;
        }

        private static void translate(@Nonnull final BgpPrefixSid bgpPrefixSid, @Nonnull final MplsRouteAddDel request,
                                      @Nonnull final Logger logger) {
            Long labelIndex = null;
            OriginatorSrgbTlv originatorSrgb = null;
            for (BgpPrefixSidTlvs entry : bgpPrefixSid.getBgpPrefixSidTlvs()) {
                final BgpPrefixSidTlv tlv = entry.getBgpPrefixSidTlv();
                if (tlv instanceof LabelIndexTlv) {
                    if (labelIndex != null) {
                        logger.warn("More than one label-index-tlv encountered while parsing bgp-prefix-sid-tlvs: %s."
                            + "Ignoring all but %s", bgpPrefixSid, labelIndex);
                    } else {
                        labelIndex = ((LabelIndexTlv) tlv).getLabelIndexTlv();
                    }
                } else if (tlv instanceof OriginatorSrgbTlv) {
                    if (originatorSrgb != null) {
                        logger
                            .warn("More than one originator-srgb-tlv encountered while parsing bgp-prefix-sid-tlvs: %s."
                                + "Ignoring all but %s", bgpPrefixSid, originatorSrgb);
                    } else {
                        originatorSrgb = (OriginatorSrgbTlv) tlv;
                    }
                }
            }

            // TODO(HC2VPP-272): add support for dynamic (random) label (RFC3107)

            checkArgument(labelIndex != null, "Missing label-index-tlv");
            // TODO(HC2VPP-272): the originator-srgb-tlv is optional,
            // make SRGB range configurable via netconf (requires writeConfig)
            checkArgument(originatorSrgb != null, "Missing originator-srgb-tlv");
            // TODO(HC2VPP-272): add support for more than one SRGB
            checkArgument(originatorSrgb.getSrgbValue().size() == 1,
                "Only one SRGB range is currently supported, but more than one was defined: %s", originatorSrgb);
            // Compute local label based on labelIndex value:
            final SrgbValue srgbValue = originatorSrgb.getSrgbValue().get(0);
            final long srgbStart = srgbValue.getBase().getValue();
            final long localLabel = srgbStart + labelIndex;
            final long srgbEnd = srgbStart + srgbValue.getRange().getValue();
            checkArgument(localLabel <= srgbEnd && localLabel >= srgbStart);
            request.mrLabel = (int) localLabel;
        }

        private static void translate(@Nonnull final List<LabelStack> labelStack,
                                      @Nonnull final MplsRouteAddDel request) {
            // It is quite possible we could support multiple labels here, but it was never tested
            // so it is not supported currently.
            final int labelCount = labelStack.size();
            checkArgument(labelCount == 1, "Single label expected, but labelStack.size()==%s", labelCount);
            final int label = labelStack.get(0).getLabelValue().getValue().intValue();

            // TODO(HC2VPP-271): add support for special labels, e.g. implicit null (for PHP).

            // swap one label to another
            request.mrNextHopOutLabelStack = new FibMplsLabel[] {MplsLabelTranslator.INSTANCE.translate(label)};
            request.mrNextHopNOutLabels = 1;
        }
    }
}
