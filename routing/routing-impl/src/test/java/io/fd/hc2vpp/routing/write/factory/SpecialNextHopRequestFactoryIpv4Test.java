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

import static io.fd.hc2vpp.routing.write.factory.SpecialNextHopRequestFactory.forContexts;
import static org.junit.Assert.assertEquals;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.SpecialNextHop.SpecialNextHopEnum.Blackhole;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.SpecialNextHop.SpecialNextHopEnum.Prohibit;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.SpecialNextHop.SpecialNextHopEnum.Receive;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.SpecialNextHop.SpecialNextHopEnum.Unreachable;

import io.fd.hc2vpp.common.test.util.NamingContextHelper;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.Ipv4RouteData;
import io.fd.hc2vpp.routing.helpers.ClassifyTableTestHelper;
import io.fd.hc2vpp.routing.helpers.RoutingRequestTestHelper;
import io.fd.hc2vpp.routing.helpers.SchemaContextTestHelper;
import io.fd.hc2vpp.routing.write.trait.RouteRequestProducer;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.control.plane.protocol.StaticRoutes;

@RunWith(HoneycombTestRunner.class)
public class SpecialNextHopRequestFactoryIpv4Test
        implements RouteRequestProducer, RoutingRequestTestHelper, ClassifyTableTestHelper,
        SchemaContextTestHelper,NamingContextHelper {

    private static final String PARENT_PROTOCOL_4 = "parent-protocol-4";
    private static final int PARENT_PROTOCOL_4_INDEX = 4;
    public static final Ipv4Prefix IPV_4_PREFIX = new Ipv4Prefix("192.168.2.1/24");
    public static final int DST_PREFIX = 24;

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
        factory = forContexts(classifierContextManager, interfaceContext, routingProtocolContextContext);

        addMapping(classifierContextManager, CLASSIFY_TABLE_NAME, CLASSIFY_TABLE_INDEX, mappingContext);
        defineMapping(mappingContext, PARENT_PROTOCOL_4, PARENT_PROTOCOL_4_INDEX, "routing-protocol-context");
    }

    @Test
    public void testIpv4WithClassifierBlackhole(
            @InjectTestData(resourcePath = "/ipv4/specialhop/specialHopRouteBlackhole.json", id = STATIC_ROUTE_PATH)
                StaticRoutes routes) {
        final IpAddDelRoute request =
                factory.createIpv4SpecialHopRequest(true, PARENT_PROTOCOL_4, extractSingleRoute(routes, IPV_4_PREFIX),
                                                    mappingContext, Blackhole);

        assertEquals(desiredSpecialResult(1, 0, Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY, DST_PREFIX, 1, 0, 0, 0,
                                          PARENT_PROTOCOL_4_INDEX, DEFAULT_VNI),
                     request);
    }

    @Test
    public void testIpv4WithClassifierReceive(
            @InjectTestData(resourcePath = "/ipv4/specialhop/specialHopRouteReceive.json", id = STATIC_ROUTE_PATH)
                    StaticRoutes routes) {
        final IpAddDelRoute request =
                factory.createIpv4SpecialHopRequest(true, PARENT_PROTOCOL_4, extractSingleRoute(routes, IPV_4_PREFIX),
                                                    mappingContext, Receive);

        assertEquals(desiredSpecialResult(1, 0, Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY, DST_PREFIX, 0, 1, 0, 0,
                                          PARENT_PROTOCOL_4_INDEX, DEFAULT_VNI),
                     request);
    }

    @Test
    public void testIpv4WithClassifierUnreach(
            @InjectTestData(resourcePath = "/ipv4/specialhop/specialHopRouteUnreachable.json", id = STATIC_ROUTE_PATH)
                    StaticRoutes routes) {
        final IpAddDelRoute request =
                factory.createIpv4SpecialHopRequest(true, PARENT_PROTOCOL_4, extractSingleRoute(routes, IPV_4_PREFIX),
                                                    mappingContext, Unreachable);

        assertEquals(desiredSpecialResult(1, 0, Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY, DST_PREFIX, 0, 0, 1, 0,
                                          PARENT_PROTOCOL_4_INDEX, DEFAULT_VNI),
                     request);
    }

    @Test
    public void testIpv4WithClassifierProhibited(
            @InjectTestData(resourcePath = "/ipv4/specialhop/specialHopRouteProhibited.json", id = STATIC_ROUTE_PATH)
                    StaticRoutes routes) {
        final IpAddDelRoute request =
                factory.createIpv4SpecialHopRequest(true, PARENT_PROTOCOL_4, extractSingleRoute(routes, IPV_4_PREFIX),
                                                    mappingContext, Prohibit);

        assertEquals(desiredSpecialResult(1, 0, Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY, DST_PREFIX, 0, 0, 0, 1,
                                          PARENT_PROTOCOL_4_INDEX, DEFAULT_VNI),
                     request);
    }

    private Route extractSingleRoute(final StaticRoutes staticRoutes, final Ipv4Prefix id) {
        return staticRoutes.augmentation(StaticRoutes1.class).getIpv4().getRoute().stream()
                .filter(route -> route.getDestinationPrefix().getValue().equals(id.getValue()))
                .collect(RWUtils.singleItemCollector());
    }
}
