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

import static io.fd.hc2vpp.bgp.prefix.sid.BgpPrefixSidMplsWriter.MPLS_LABEL_INVALID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.MplsRouteAddDel;
import io.fd.vpp.jvpp.core.dto.MplsRouteAddDelReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.LabeledUnicastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.Srgb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.list.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.list.LabeledUnicastRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.list.LabeledUnicastRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.list.labeled.unicast.route.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuOriginatorSrgbTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.originator.srgb.tlv.SrgbValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuLabelIndexTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.BgpPrefixSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.BgpPrefixSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.bgp.prefix.sid.BgpPrefixSidTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.bgp.prefix.sid.BgpPrefixSidTlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BgpPrefixSidMplsWriterTest implements FutureProducer, ByteDataTranslator {
    private static final InstanceIdentifier<Tables> TABLE_ID = InstanceIdentifier.create(BgpRib.class)
        .child(Rib.class, new RibKey(new RibId("test-rib"))).child(LocRib.class)
        .child(Tables.class, new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));

    @Mock
    private FutureJVppCore vppApi;
    private BgpPrefixSidMplsWriter writer;

    @SuppressWarnings("unchecked")
    private static InstanceIdentifier<LabeledUnicastRoute> id(final PathId pathId, final String routeKey) {
        return TABLE_ID.child((Class) LabeledUnicastRoutes.class)
            .child(LabeledUnicastRoute.class, new LabeledUnicastRouteKey(pathId, routeKey));
    }

    private static LabeledUnicastRoute route(final String routeKey, final PathId pathId,
                                             final Ipv4Address nextHopAddress,
                                             final BgpPrefixSid bgpPrefixSid) {
        final Ipv4NextHopCase nextHop =
            new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(nextHopAddress).build())
                .build();
        return new LabeledUnicastRouteBuilder()
            .setKey(new LabeledUnicastRouteKey(pathId, routeKey))
            .setPathId(pathId)
            .setAttributes(new AttributesBuilder()
                .setCNextHop(nextHop)
                .setBgpPrefixSid(bgpPrefixSid)
                .build())
            .setLabelStack(
                Collections.singletonList(new LabelStackBuilder().setLabelValue(new MplsLabel(16101L)).build()))
            .build();
    }

    private static BgpPrefixSidTlvs labelIndexTlv(final long label) {
        return new BgpPrefixSidTlvsBuilder()
            .setBgpPrefixSidTlv(new LuLabelIndexTlvBuilder()
                .setLabelIndexTlv(label)
                .build())
            .build();
    }

    @Before
    public void setUp() {
        initMocks(this);
        writer = new BgpPrefixSidMplsWriter(vppApi);
        when(vppApi.mplsRouteAddDel(any())).thenReturn(future(new MplsRouteAddDelReply()));
    }

    private BgpPrefixSidTlvs originatorSrgbTlv(final long base, final long range) {
        return new BgpPrefixSidTlvsBuilder()
            .setBgpPrefixSidTlv(new LuOriginatorSrgbTlvBuilder()
                .setSrgbValue(Collections.singletonList(new SrgbValueBuilder()
                    .setBase(new Srgb(base))
                    .setRange(new Srgb(range))
                    .build()))
                .build())
            .build();
    }

    @Test
    public void testCreate() throws WriteFailedException.CreateFailedException {
        final String routeKey = "route-key";
        final PathId pathId = new PathId(123L);
        final Ipv4Address nextHopAddress = new Ipv4AddressNoZone("5.6.7.8");

        final BgpPrefixSid bgpPrefixSid = new BgpPrefixSidBuilder()
            .setBgpPrefixSidTlvs(
                Lists.newArrayList(
                    labelIndexTlv(102L),
                    originatorSrgbTlv(16000, 800)
                ))
            .build();
        writer.create(
            id(pathId, routeKey),
            route(routeKey, pathId, nextHopAddress, bgpPrefixSid)
        );
        verifyRequest(true);
    }

    private void verifyRequest(boolean isAdd) {
        final MplsRouteAddDel request = new MplsRouteAddDel();
        request.mrIsAdd = booleanToByte(isAdd);
        request.mrClassifyTableIndex = -1;
        request.mrNextHopWeight = 1;
        request.mrNextHopViaLabel = MPLS_LABEL_INVALID;

        request.mrNextHop = new byte[] {5, 6, 7, 8};
        request.mrNextHopSwIfIndex = -1;

        request.mrLabel = 16102;

        request.mrNextHopOutLabelStack = new int[] {16101};
        request.mrNextHopNOutLabels = 1;

        request.mrEos = 1;
        verify(vppApi).mplsRouteAddDel(request);
    }
}
