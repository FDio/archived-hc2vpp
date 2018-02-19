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

package io.fd.hc2vpp.bgp.inet;

import static io.fd.hc2vpp.bgp.inet.RouteRequestProducer.MPLS_LABEL_INVALID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import io.fd.vpp.jvpp.core.dto.IpAddDelRouteReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.ipv6.routes.Ipv6RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv6.routes.ipv6.routes.Ipv6RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv6WriterTest implements FutureProducer, ByteDataTranslator {
    private static final InstanceIdentifier<Tables> TABLE_ID = InstanceIdentifier.create(BgpRib.class)
        .child(Rib.class, new RibKey(new RibId("test-rib"))).child(LocRib.class)
        .child(Tables.class, new TablesKey(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class));

    @Mock
    private FutureJVppCore vppApi;
    private Ipv6Writer writer;

    @Before
    public void setUp() {
        initMocks(this);
        writer = new Ipv6Writer(vppApi);
        when(vppApi.ipAddDelRoute(any())).thenReturn(future(new IpAddDelRouteReply()));
    }

    @SuppressWarnings("unchecked")
    private static InstanceIdentifier<Ipv6Route> id(final Ipv6Prefix destination, final PathId pathId) {
        return TABLE_ID.child((Class) Ipv6Routes.class)
            .child(Ipv6Route.class, new Ipv6RouteKey(pathId, destination));
    }

    private static Ipv6Route route(final Ipv6Prefix destination, final PathId pathId,
                                   final Ipv6Address nextHopAddress) {
        final Ipv6NextHopCase nextHop =
            new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder().setGlobal(nextHopAddress).build())
                .build();
        return new Ipv6RouteBuilder()
            .setPrefix(destination)
            .setPathId(pathId)
            .setAttributes(new AttributesBuilder().setCNextHop(nextHop).build())
            .build();
    }

    @Test
    public void testCreate() throws WriteFailedException.CreateFailedException {
        final Ipv6Prefix destination = new Ipv6Prefix("2001:db8:a0b:12f0:0:0:0:1/64");
        final PathId pathId = new PathId(123L);
        final Ipv6Address nextHopAddress = new Ipv6AddressNoZone("2001:db8:a0b:12f0:0:0:0:2");

        writer.create(
            id(destination, pathId),
            route(destination, pathId, nextHopAddress)
        );
        verifyRequest(true);
    }

    @Test
    public void testDelete() throws WriteFailedException.DeleteFailedException {
        final Ipv6Prefix destination = new Ipv6Prefix("2001:db8:a0b:12f0:0:0:0:1/64");
        final PathId pathId = new PathId(456L);
        final Ipv6Address nextHopAddress = new Ipv6AddressNoZone("2001:db8:a0b:12f0:0:0:0:2");

        writer.delete(
            id(destination, pathId),
            route(destination, pathId, nextHopAddress)
        );
        verifyRequest(false);
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdate() throws WriteFailedException.UpdateFailedException {
        final Ipv6Prefix destination = new Ipv6Prefix("2001:db8:a0b:12f0:0:0:0:1/64");
        final PathId pathId = new PathId(456L);

        // update is not supported
        writer.update(id(destination, pathId), mock(Ipv6Route.class), mock(Ipv6Route.class));
    }

    private void verifyRequest(boolean isAdd) {
        final IpAddDelRoute request = new IpAddDelRoute();
        request.isAdd = booleanToByte(isAdd);
        request.isIpv6 = 1;
        request.nextHopSwIfIndex = -1;
        request.nextHopViaLabel = MPLS_LABEL_INVALID;
        request.nextHopAddress = new byte[] {
            0x20, 0x01, 0x0d, (byte) 0xb8, 0x0a, 0x0b, 0x12, (byte) 0xf0,
            0, 0, 0, 0, 0, 0, 0, 2};
        request.dstAddress = new byte[] {
            0x20, 0x01, 0x0d, (byte) 0xb8, 0x0a, 0x0b, 0x12, (byte) 0xf0,
            0, 0, 0, 0, 0, 0, 0, 1};
        request.dstAddressLength = 64;
        verify(vppApi).ipAddDelRoute(request);
    }
}
