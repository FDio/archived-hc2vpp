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

package io.fd.hc2vpp.routing;

import static io.fd.hc2vpp.routing.Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY;
import static org.junit.Assert.assertEquals;

import io.fd.hc2vpp.common.test.util.NamingContextHelper;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.helpers.RoutingRequestTestHelper;
import io.fd.hc2vpp.routing.helpers.SchemaContextTestHelper;
import io.fd.hc2vpp.routing.naming.Ipv6RouteNamesFactory;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.jvpp.core.dto.Ip6FibDetails;
import io.fd.jvpp.core.types.FibPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipv6.unicast.routing.rev180319.VppIpv6NextHopAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipv6.unicast.routing.rev180319.VppIpv6NextHopAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.route.next.hop.NextHop1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.route.next.hop.NextHop1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.next.hop.list.NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.control.plane.protocol.StaticRoutes;

@RunWith(HoneycombTestRunner.class)
public class Ipv6RouteNamesFactoryTest implements RoutingRequestTestHelper, SchemaContextTestHelper,
        NamingContextHelper {

    @Mock
    private MappingContext mappingContext;

    private NamingContext interfaceContext;
    private NamingContext routingProtocolContext;
    private Ip6FibDetails vppRoute;
    private Ip6FibDetails vppRoute128;
    private FibPath vppPath;
    private Ipv6RouteNamesFactory factory;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        interfaceContext = new NamingContext("interface-", "interface-context");
        routingProtocolContext = new NamingContext("routing-protocol-", "routing-protocol-context");
        vppRoute = new Ip6FibDetails();
        vppRoute.address = FIRST_ADDRESS_AS_ARRAY;
        vppRoute.addressLength = 64;
        vppRoute.tableId = 1;

        vppRoute128 = new Ip6FibDetails();
        vppRoute128.address = FIRST_ADDRESS_AS_ARRAY;
        vppRoute128.addressLength = (byte) 128;
        vppRoute128.tableId = 1;

        vppPath = new FibPath();
        vppPath.nextHop = FIRST_ADDRESS_AS_ARRAY;
        vppPath.swIfIndex = 2;
        vppPath.weight = 3;
        factory = new Ipv6RouteNamesFactory(interfaceContext, routingProtocolContext);
        defineMapping(mappingContext, "iface", 2, "interface-context");
        defineMapping(mappingContext, ROUTE_PROTOCOL_NAME, 1, "routing-protocol-context");
    }

    @Test
    public void testUniqueRouteName(
            @InjectTestData(resourcePath = "/ipv6/simplehop/simpleHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH)
                StaticRoutes data) {
        assertEquals("tst-protocol_2001-db8-a0b-12f0--1_64",
                     factory.uniqueRouteName(ROUTE_PROTOCOL_NAME,
                                             getIpv6RouteWithId(data,
                                                new Ipv6Prefix("2001:0db8:0a0b:12f0:0000:0000:0000:0001/64"))));
        assertEquals("tst-protocol_2001-db8-a0b-12f0--1_64", factory.uniqueRouteName(vppRoute, mappingContext));
    }

    @Test
    public void testUniqueRouteName128(
            @InjectTestData(resourcePath = "/ipv6/simplehop/simpleHopRoute128.json", id = STATIC_ROUTE_PATH)
                    StaticRoutes data) {
        assertEquals("tst-protocol_2001-db8-a0b-12f0--1_128",
                factory.uniqueRouteName(ROUTE_PROTOCOL_NAME,
                        getIpv6RouteWithId(data,
                                new Ipv6Prefix("2001:0db8:0a0b:12f0:0000:0000:0000:0001/128"))));
        assertEquals("tst-protocol_2001-db8-a0b-12f0--1_128", factory.uniqueRouteName(vppRoute128, mappingContext));
    }

    @Test
    public void testUniqueRouteHopName() {
        assertEquals("iface_2001-db8-a0b-12f0--1_3", factory.uniqueRouteHopName(
            new NextHopBuilder()
                .setOutgoingInterface("iface")
                .setIndex("1")
                .addAugmentation(NextHop1.class,
                    new NextHop1Builder()
                        .setNextHopAddress(new Ipv6Address("2001:0db8:0a0b:12f0:0000:0000:0000:0001"))
                        .build())
                .addAugmentation(VppIpv6NextHopAugmentation.class,
                    new VppIpv6NextHopAugmentationBuilder()
                        .setWeight((short) 3)
                        .build())
                    .build()));
        assertEquals("iface_2001-db8-a0b-12f0--1_3",
                factory.uniqueRouteHopName(vppPath, mappingContext));
    }
}
