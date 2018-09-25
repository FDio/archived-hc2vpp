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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import io.fd.vpp.jvpp.core.dto.IpAddDelRouteReply;
import io.fd.vpp.jvpp.core.dto.MplsRouteAddDel;
import io.fd.vpp.jvpp.core.dto.MplsRouteAddDelReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.core.types.FibMplsLabel;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.LabeledUnicastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.Srgb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.LabelStackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.list.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.list.LabeledUnicastRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.list.LabeledUnicastRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.list.labeled.unicast.route.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuOriginatorSrgbTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.originator.srgb.tlv.SrgbValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuLabelIndexTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.BgpPrefixSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.BgpPrefixSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.bgp.prefix.sid.BgpPrefixSidTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.bgp.prefix.sid.BgpPrefixSidTlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
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

    private static LabeledUnicastRoute route(final PathId pathId, final String routeKey) {
        final Ipv4Address nextHopAddress = new Ipv4AddressNoZone("5.6.7.8");

        final BgpPrefixSid bgpPrefixSid = new BgpPrefixSidBuilder()
            .setBgpPrefixSidTlvs(
                Lists.newArrayList(
                    labelIndexTlv(102L),
                    originatorSrgbTlv(16000, 800)
                ))
            .build();

        final Ipv4NextHopCase nextHop =
            new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(nextHopAddress).build())
                .build();
        final IpPrefix prefix = new IpPrefix(new Ipv4Prefix("1.2.3.4/24"));
        return new LabeledUnicastRouteBuilder()
            .withKey(new LabeledUnicastRouteKey(pathId, routeKey))
            .setPathId(pathId)
            .setPrefix(prefix)
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

    private static BgpPrefixSidTlvs originatorSrgbTlv(final long base, final long range) {
        return new BgpPrefixSidTlvsBuilder()
            .setBgpPrefixSidTlv(new LuOriginatorSrgbTlvBuilder()
                .setSrgbValue(Collections.singletonList(new SrgbValueBuilder()
                    .setBase(new Srgb(base))
                    .setRange(new Srgb(range))
                    .build()))
                .build())
            .build();
    }

    @Before
    public void setUp() {
        initMocks(this);
        writer = new BgpPrefixSidMplsWriter(vppApi);
        when(vppApi.mplsRouteAddDel(any())).thenReturn(future(new MplsRouteAddDelReply()));
        when(vppApi.ipAddDelRoute(any())).thenReturn(future(new IpAddDelRouteReply()));
    }

    @Test
    public void testCreate() throws WriteFailedException.CreateFailedException {
        final String routeKey = "route-key";
        final PathId pathId = new PathId(123L);
        writer.create(id(pathId, routeKey), route(pathId, routeKey));

        verify(vppApi, times(2)).mplsRouteAddDel(any());
        // BgpPrefixSidMplsWriter.create reuses DTO for two calls for performance reasons, but mockito
        // (InOrder, ArgumentCaptor) works with object references, not values. We are a bit too lazy to use thenAnswer,
        // so checking just second invocation:
        verify(vppApi, atLeastOnce()).mplsRouteAddDel(getRequest(true, true));

        verify(vppApi).ipAddDelRoute(getRequest(true));
    }

    @Test
    public void testDelete() throws WriteFailedException.DeleteFailedException {
        final String routeKey = "route-key";
        final PathId pathId = new PathId(123L);
        writer.delete(id(pathId, routeKey), route(pathId, routeKey));

        verify(vppApi, times(2)).mplsRouteAddDel(any());
        verify(vppApi, atLeastOnce()).mplsRouteAddDel(getRequest(false, true));
        verify(vppApi).ipAddDelRoute(getRequest(false));
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdate() throws WriteFailedException.UpdateFailedException {
        final String routeKey = "route-key";
        final PathId pathId = new PathId(123L);
        writer.update(id(pathId, routeKey), mock(LabeledUnicastRoute.class), mock(LabeledUnicastRoute.class));
        verifyZeroInteractions(vppApi);
    }

    private MplsRouteAddDel getRequest(boolean isAdd, boolean isEos) {
        final MplsRouteAddDel request = new MplsRouteAddDel();
        request.mrIsAdd = booleanToByte(isAdd);
        request.mrClassifyTableIndex = -1;
        request.mrNextHopWeight = 1;
        request.mrNextHopViaLabel = MPLS_LABEL_INVALID;

        request.mrNextHop = new byte[] {5, 6, 7, 8};
        request.mrNextHopSwIfIndex = -1;

        request.mrLabel = 16102;

        final FibMplsLabel mplsLabel = new FibMplsLabel();
        mplsLabel.label = 16101;
        request.mrNextHopOutLabelStack = new FibMplsLabel[] {mplsLabel};
        request.mrNextHopNOutLabels = 1;

        request.mrEos = booleanToByte(isEos);
        return request;
    }

    private IpAddDelRoute getRequest(boolean isAdd) {
        final IpAddDelRoute request = new IpAddDelRoute();
        request.isAdd = booleanToByte(isAdd);
        request.classifyTableIndex = -1;
        request.nextHopWeight = 1;
        request.nextHopViaLabel = MPLS_LABEL_INVALID;

        request.dstAddressLength = 24;
        request.dstAddress = new byte[] {1, 2, 3, 4};

        request.nextHopAddress = new byte[] {5, 6, 7, 8};
        request.nextHopSwIfIndex = -1;

        final FibMplsLabel mplsLabel = new FibMplsLabel();
        mplsLabel.label = 16101;
        request.nextHopOutLabelStack = new FibMplsLabel[] {mplsLabel};
        request.nextHopNOutLabels = 1;
        return request;
    }
}
