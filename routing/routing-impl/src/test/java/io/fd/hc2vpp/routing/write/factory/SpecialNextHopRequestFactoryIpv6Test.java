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

package io.fd.hc2vpp.routing.write.factory;

import static io.fd.hc2vpp.routing.write.trait.RouteRequestProducer.DEFAULT_VNI;
import static org.junit.Assert.assertEquals;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.SpecialNextHopGrouping.SpecialNextHop.Blackhole;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.SpecialNextHopGrouping.SpecialNextHop.Prohibit;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.SpecialNextHopGrouping.SpecialNextHop.Receive;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.SpecialNextHopGrouping.SpecialNextHop.Unreachable;

import io.fd.hc2vpp.common.test.util.NamingContextHelper;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.Ipv6RouteData;
import io.fd.hc2vpp.routing.helpers.ClassifyTableTestHelper;
import io.fd.hc2vpp.routing.helpers.RoutingRequestTestHelper;
import io.fd.hc2vpp.routing.helpers.SchemaContextTestHelper;
import io.fd.hc2vpp.v3po.vppclassifier.VppClassifierContextManager;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol.StaticRoutes;

@RunWith(HoneycombTestRunner.class)
public class SpecialNextHopRequestFactoryIpv6Test
        implements RoutingRequestTestHelper, ClassifyTableTestHelper, SchemaContextTestHelper, NamingContextHelper {

    private static final String PARENT_PROTOCOL_6 = "parent-protocol-6";
    private static final int PARENT_PROTOCOL_6_INDEX = 6;

    @Mock
    private VppClassifierContextManager classifierContextManager;

    @Mock
    private MappingContext mappingContext;

    private NamingContext interfaceContext;
    private NamingContext routingProtocolContextContext;

    private SpecialNextHopRequestFactory factory;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        interfaceContext = new NamingContext("iface", "interface-context");
        routingProtocolContextContext = new NamingContext("routingProtocol", "routing-protocol-context");
        factory = SpecialNextHopRequestFactory.forContexts(classifierContextManager, interfaceContext, routingProtocolContextContext);

        addMapping(classifierContextManager, CLASSIFY_TABLE_NAME, CLASSIFY_TABLE_INDEX, mappingContext);
        defineMapping(mappingContext, PARENT_PROTOCOL_6, PARENT_PROTOCOL_6_INDEX, "routing-protocol-context");
    }

    @Test
    public void testIpv6Blackhole(
            @InjectTestData(resourcePath = "/ipv6/specialhop/specialHopRouteBlackhole.json", id = STATIC_ROUTE_PATH)
                    StaticRoutes routes) {
        final IpAddDelRoute request =
                factory.createIpv6SpecialHopRequest(true, PARENT_PROTOCOL_6, extractSingleRoute(routes, 1L), mappingContext, Blackhole);

        assertEquals(desiredSpecialResult(1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 24, 1, 0, 0, 0, PARENT_PROTOCOL_6_INDEX, DEFAULT_VNI),
                request);
    }

    @Test
    public void testIpv6Receive(
            @InjectTestData(resourcePath = "/ipv6/specialhop/specialHopRouteReceive.json", id = STATIC_ROUTE_PATH)
                    StaticRoutes routes) {
        final IpAddDelRoute request =
                factory.createIpv6SpecialHopRequest(true, PARENT_PROTOCOL_6, extractSingleRoute(routes, 1L), mappingContext, Receive);

        assertEquals(desiredSpecialResult(1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 24, 0, 1, 0, 0, PARENT_PROTOCOL_6_INDEX, DEFAULT_VNI), request);
    }

    @Test
    public void testIpv6Unreach(
            @InjectTestData(resourcePath = "/ipv6/specialhop/specialHopRouteUnreachable.json", id = STATIC_ROUTE_PATH)
                    StaticRoutes routes) {
        final IpAddDelRoute request =
                factory.createIpv6SpecialHopRequest(true, PARENT_PROTOCOL_6, extractSingleRoute(routes, 1L), mappingContext, Unreachable);

        assertEquals(desiredSpecialResult(1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 24, 0, 0, 1, 0, PARENT_PROTOCOL_6_INDEX, DEFAULT_VNI), request);
    }

    @Test
    public void testIpv6Prohibited(
            @InjectTestData(resourcePath = "/ipv6/specialhop/specialHopRouteProhibited.json", id = STATIC_ROUTE_PATH)
                    StaticRoutes routes) {
        final IpAddDelRoute request =
                factory.createIpv6SpecialHopRequest(true, PARENT_PROTOCOL_6, extractSingleRoute(routes, 1L), mappingContext, Prohibit);

        assertEquals(desiredSpecialResult(1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 24, 0, 0, 0, 1, PARENT_PROTOCOL_6_INDEX, DEFAULT_VNI), request);
    }

    private Route extractSingleRoute(final StaticRoutes staticRoutes, final long id) {
        return staticRoutes.getAugmentation(StaticRoutes1.class).getIpv6().getRoute().stream()
                .filter(route -> route.getId() == id).collect(
                        RWUtils.singleItemCollector());
    }

}
