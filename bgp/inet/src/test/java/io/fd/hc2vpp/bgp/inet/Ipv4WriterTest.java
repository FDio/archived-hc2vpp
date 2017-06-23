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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static io.fd.hc2vpp.bgp.inet.RouteRequestProducer.MPLS_LABEL_INVALID;

import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import io.fd.vpp.jvpp.core.dto.IpAddDelRouteReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv4WriterTest implements FutureProducer, ByteDataTranslator {
    private static final InstanceIdentifier<Tables> TABLE_ID = InstanceIdentifier.create(BgpRib.class)
        .child(Rib.class, new RibKey(new RibId("test-rib"))).child(LocRib.class)
        .child(Tables.class, new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));

    @Mock
    private FutureJVppCore vppApi;
    private Ipv4Writer writer;

    @Before
    public void setUp() {
        initMocks(this);
        writer = new Ipv4Writer(vppApi);
        when(vppApi.ipAddDelRoute(any())).thenReturn(future(new IpAddDelRouteReply()));
    }

    @SuppressWarnings("unchecked")
    private static InstanceIdentifier<Ipv4Route> id(final Ipv4Prefix destination, final PathId pathId) {
        return TABLE_ID.child((Class) Ipv4Routes.class)
            .child(Ipv4Route.class, new Ipv4RouteKey(pathId, destination));
    }

    private static Ipv4Route route(final Ipv4Prefix destination, final PathId pathId,
                                   final Ipv4Address nextHopAddress) {
        final Ipv4NextHopCase nextHop =
            new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(nextHopAddress).build())
                .build();
        return new Ipv4RouteBuilder()
            .setPrefix(destination)
            .setPathId(pathId)
            .setAttributes(new AttributesBuilder().setCNextHop(nextHop).build())
            .build();
    }

    @Test
    public void testCreate() throws WriteFailedException.CreateFailedException {
        final Ipv4Prefix destination = new Ipv4Prefix("1.2.3.4/24");
        final PathId pathId = new PathId(123L);
        final Ipv4Address nextHopAddress = new Ipv4AddressNoZone("5.6.7.8");

        writer.create(
            id(destination, pathId),
            route(destination, pathId, nextHopAddress)
        );
        verifyRequest(true);
    }

    @Test
    public void testDelete() throws WriteFailedException.DeleteFailedException {
        final Ipv4Prefix destination = new Ipv4Prefix("1.2.3.4/24");
        final PathId pathId = new PathId(456L);
        final Ipv4Address nextHopAddress = new Ipv4AddressNoZone("5.6.7.8");

        writer.delete(
            id(destination, pathId),
            route(destination, pathId, nextHopAddress)
        );
        verifyRequest(false);
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdate() throws WriteFailedException.UpdateFailedException {
        final Ipv4Prefix destination = new Ipv4Prefix("10.1.0.1/28");
        final PathId pathId = new PathId(456L);

        // update is not supported
        writer.update(id(destination, pathId), mock(Ipv4Route.class), mock(Ipv4Route.class));
    }

    private void verifyRequest(boolean isAdd) {
        final IpAddDelRoute request = new IpAddDelRoute();
        request.isAdd = booleanToByte(isAdd);
        request.nextHopSwIfIndex = -1;
        request.nextHopViaLabel = MPLS_LABEL_INVALID;
        request.nextHopAddress = new byte[] {5, 6, 7, 8};
        request.dstAddress = new byte[] {1, 2, 3, 4};
        request.dstAddressLength = 24;
        verify(vppApi).ipAddDelRoute(request);
    }
}
