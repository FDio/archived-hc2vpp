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
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.sun.istack.internal.Nullable;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.bgp.RouteWriter;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.MplsRouteAddDel;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.LabelIndexTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.LabeledUnicastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.OriginatorSrgbTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.list.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.originator.srgb.tlv.SrgbValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.BgpPrefixSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.bgp.prefix.sid.BgpPrefixSidTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Programs VPP according to draft-ietf-idr-bgp-prefix-sid.
 *
 * @see <a href="https://tools.ietf.org/html/draft-ietf-idr-bgp-prefix-sid-07#section-4.1">Receiving BGP-Prefix-SID
 * attribute</a>
 */
final class BgpPrefixSidMplsWriter implements RouteWriter<LabeledUnicastRoute>, Ipv4Translator, JvppReplyConsumer {

    /**
     * Constant used by VPP to disable optional parameters of mpls label type.
     */
    @VisibleForTesting
    static final int MPLS_LABEL_INVALID = 0x100000;

    private static final Logger LOG = LoggerFactory.getLogger(BgpPrefixSidMplsWriter.class);

    @SuppressWarnings("unchecked")
    private static final InstanceIdentifier<LabeledUnicastRoute> ID =
        InstanceIdentifier.create(BgpRib.class).child(Rib.class)
            .child(LocRib.class).child(Tables.class).child((Class) LabeledUnicastRoutes.class)
            .child(LabeledUnicastRoute.class);

    private final FutureJVppCore vppApi;

    BgpPrefixSidMplsWriter(@Nonnull final FutureJVppCore vppApi) {
        this.vppApi = checkNotNull(vppApi, "vppApi should not be null");
    }

    @Override
    public void create(@Nonnull final InstanceIdentifier<LabeledUnicastRoute> id,
                       @Nullable final LabeledUnicastRoute route)
        throws WriteFailedException.CreateFailedException {
        final MplsRouteAddDel request = request(route, true);
        LOG.debug("Translating id={}, route={} to {}", id, route, request);
        getReplyForCreate(vppApi.mplsRouteAddDel(request).toCompletableFuture(), id, route);


        // TODO(HC2VPP-268): except for SWAP EOS label entry, we should also create:
        // 1) SWAP NON-EOS label
        // 2) Push label to handle situations when non MPLS packet goes in and its destination is equals to
        // the prefix that is being announced (in the example from the draft, it is BGP-Prefix-SID originator loopback):
        // https://tools.ietf.org/html/draft-ietf-spring-segment-routing-msdc-06#section-4.2.2

        LOG.debug("VPP FIB updated successfully (added id={}).", id);
    }

    @Override
    public void delete(@Nonnull final InstanceIdentifier<LabeledUnicastRoute> id,
                       @Nullable final LabeledUnicastRoute route)
        throws WriteFailedException.DeleteFailedException {
        LOG.debug("Removing id={}, route={}", id, route);
        getReplyForDelete(vppApi.mplsRouteAddDel(request(route, false)).toCompletableFuture(), id);
        LOG.debug("VPP FIB updated successfully (removed id={}).", id);
    }

    @Override
    public void update(@Nonnull final InstanceIdentifier<LabeledUnicastRoute> id,
                       @Nullable final LabeledUnicastRoute routeBefore,
                       @Nullable final LabeledUnicastRoute routeAfter)
        throws WriteFailedException.UpdateFailedException {
        throw new WriteFailedException.UpdateFailedException(id, routeBefore, routeAfter,
            new UnsupportedOperationException("Operation not supported"));
    }

    private MplsRouteAddDel request(final LabeledUnicastRoute route, boolean isAdd) {
        final MplsRouteAddDel request = mplsRouteAddDel(isAdd);


        translate(route.getAttributes().getCNextHop(), request);
        translate(route.getAttributes().getBgpPrefixSid(), request);
        translate(route.getLabelStack(), request);

        request.mrEos = 1;
        return request;
    }

    private MplsRouteAddDel mplsRouteAddDel(final boolean isAdd) {
        final MplsRouteAddDel request = new MplsRouteAddDel();
        request.mrIsAdd = booleanToByte(isAdd);

        // default values based on inspecting VPP's CLI and make test code
        request.mrClassifyTableIndex = -1;
        request.mrNextHopWeight = 1;
        request.mrNextHopViaLabel = MPLS_LABEL_INVALID;
        return request;
    }

    private void translate(@Nonnull final CNextHop cNextHop, @Nonnull final MplsRouteAddDel request) {
        checkArgument(cNextHop instanceof Ipv4NextHopCase,
            "only ipv4 next hop is supported, but was %s (cNextHop = %s)", cNextHop, cNextHop);
        final Ipv4Address nextHop = ((Ipv4NextHopCase) cNextHop).getIpv4NextHop().getGlobal();
        request.mrNextHop = ipv4AddressNoZoneToArray(nextHop.getValue());

        // We create recursive route. In order to make everything work,
        // operator needs to manually map next hop address to proper interface.
        // Either via CLI or HC.
        //
        // VPP can't recursively resolve a route that has out labels via a route that does not have out labels.
        // Implicit null label is trick to get around it (no more labels will be added to the package).
        // CLI example:
        //
        // ip route add <next-hop-ip> via <next-hop-ifc> out-labels 3
        request.mrNextHopSwIfIndex = -1;
    }

    private void translate(@Nonnull final BgpPrefixSid bgpPrefixSid, @Nonnull final MplsRouteAddDel request) {
        Long labelIndex = null;
        OriginatorSrgbTlv originatorSrgb = null;
        for (BgpPrefixSidTlvs entry : bgpPrefixSid.getBgpPrefixSidTlvs()) {
            final BgpPrefixSidTlv tlv = entry.getBgpPrefixSidTlv();
            if (tlv instanceof LabelIndexTlv) {
                if (labelIndex != null) {
                    LOG.warn("        More than one label-index-tlv encountered while parsing bgp-prefix-sid-tlvs: %s."
                        + "Ignoring all but %s", bgpPrefixSid, labelIndex);
                } else {
                    labelIndex = ((LabelIndexTlv) tlv).getLabelIndexTlv();
                }
            } else if (tlv instanceof OriginatorSrgbTlv) {
                if (originatorSrgb != null) {
                    LOG.warn("More than one originator-srgb-tlv encountered while parsing bgp-prefix-sid-tlvs: %s."
                        + "Ignoring all but %s", bgpPrefixSid, originatorSrgb);
                } else {
                    originatorSrgb = (OriginatorSrgbTlv) tlv;
                }
            }
        }

        // TODO(HC2VPP-272): add support for dynamic (random) label (RFC3107)

        checkArgument(labelIndex != null, "Missing label-index-tlv");
        // TODO(HC2VPP-272): the originator-srgb-tlv is optional, make SRGB range configurable via netconf (requires writeConfig)
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

    private void translate(@Nonnull final List<LabelStack> labelStack, @Nonnull final MplsRouteAddDel request) {
        final int labelCount = labelStack.size();
        checkArgument(labelCount == 1, "Single label expected, but labelStack.size()==%s", labelCount);
        final int label = labelStack.get(0).getLabelValue().getValue().intValue();

        // TODO(HC2VPP-271): add support for special labels, e.g. implicit null (for PHP).

        // swap one label to another
        request.mrNextHopOutLabelStack = new int[] {label};
        request.mrNextHopNOutLabels = 1;
    }

    // TODO(HC2VPP-268): add test which checks if ID is serializable
    @Nonnull
    @Override
    public InstanceIdentifier<LabeledUnicastRoute> getManagedDataObjectType() {
        return ID;
    }
}
