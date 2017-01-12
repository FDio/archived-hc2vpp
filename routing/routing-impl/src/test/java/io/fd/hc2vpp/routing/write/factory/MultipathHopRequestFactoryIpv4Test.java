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


import static org.junit.Assert.assertEquals;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.Ipv4RouteData;
import io.fd.hc2vpp.routing.helpers.ClassifyTableTestHelper;
import io.fd.hc2vpp.routing.helpers.InterfaceTestHelper;
import io.fd.hc2vpp.routing.helpers.RoutingRequestTestHelper;
import io.fd.hc2vpp.routing.helpers.SchemaContextTestHelper;
import io.fd.hc2vpp.v3po.vppclassifier.VppClassifierContextManager;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.NextHopList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.next.hop.list.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol.StaticRoutes;

@RunWith(HoneycombTestRunner.class)
public class MultipathHopRequestFactoryIpv4Test
        implements RoutingRequestTestHelper, ClassifyTableTestHelper, SchemaContextTestHelper, InterfaceTestHelper {

    @Mock
    private VppClassifierContextManager classifierContextManager;

    @Mock
    private MappingContext mappingContext;

    @InjectTestData(resourcePath = "/ipv4/multihop/multiHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH)
    private StaticRoutes ipv4StaticRoutesWithClassifier;

    @InjectTestData(resourcePath = "/ipv4/multihop/multiHopRouteWithoutClassifier.json", id = STATIC_ROUTE_PATH)
    private StaticRoutes ipv4StaticRoutesWithoutClassifier;

    @InjectTestData(resourcePath = "/ipv4/multihop/multiHopRouteWithNoRouteAttrs.json", id = STATIC_ROUTE_PATH)
    private StaticRoutes ipv4StaticRoutesWithoutRouteAttrs;

    private Route ipv4MutlipathRouteWithClassifier;
    private NextHop ipv4nextHopWithClassifier;

    private Route ipv4MutlipathRouteWithoutClassifier;
    private NextHop ipv4nextHopWithoutClassifier;

    private Route ipv4MutlipathRouteWithoutRouteAtts;
    private NextHop ipv4nextHopWithoutRouteAtts;

    private NamingContext interfaceContext;
    private NamingContext routingProtocolContext;
    private MultipathHopRequestFactory factory;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        interfaceContext = new NamingContext("interface", "interface-context");
        routingProtocolContext = new NamingContext("routing-protocol", "routing-protocol-context");
        factory = MultipathHopRequestFactory
                .forContexts(classifierContextManager, interfaceContext, routingProtocolContext);

        addMapping(classifierContextManager, CLASSIFY_TABLE_NAME, CLASSIFY_TABLE_INDEX, mappingContext);
        defineMapping(mappingContext, INTERFACE_NAME, INTERFACE_INDEX, "interface-context");
        defineMapping(mappingContext, ROUTE_PROTOCOL_NAME, 1, "routing-protocol-context");

        ipv4MutlipathRouteWithClassifier = getIpv4RouteWithId(ipv4StaticRoutesWithClassifier, 1L);
        final List<NextHop> ipv4HopsClassified =
                NextHopList.class.cast(ipv4MutlipathRouteWithClassifier.getNextHopOptions()).getNextHopList()
                        .getNextHop();
        ipv4nextHopWithClassifier =
                ipv4HopsClassified.stream().filter(nextHop -> nextHop.getId() == 1L).findFirst().get();

        ipv4MutlipathRouteWithoutClassifier = getIpv4RouteWithId(ipv4StaticRoutesWithoutClassifier, 1L);
        final List<NextHop> ipv4HopsNonClassified =
                NextHopList.class.cast(ipv4MutlipathRouteWithoutClassifier.getNextHopOptions()).getNextHopList()
                        .getNextHop();
        ipv4nextHopWithoutClassifier =
                ipv4HopsNonClassified.stream().filter(nextHop -> nextHop.getId() == 1L).findFirst().get();

        ipv4MutlipathRouteWithoutRouteAtts = getIpv4RouteWithId(ipv4StaticRoutesWithoutRouteAttrs, 1L);
        final List<NextHop> ipv4HopsNonRouteAttrs =
                NextHopList.class.cast(ipv4MutlipathRouteWithoutRouteAtts.getNextHopOptions()).getNextHopList()
                        .getNextHop();
        ipv4nextHopWithoutRouteAtts =
                ipv4HopsNonClassified.stream().filter(nextHop -> nextHop.getId() == 1L).findFirst().get();
    }

    @Test
    public void testIpv4WithClassifier() {
        final IpAddDelRoute request =
                factory.createIpv4MultipathHopRequest(false, ROUTE_PROTOCOL_NAME, ipv4MutlipathRouteWithClassifier,
                        ipv4nextHopWithClassifier,
                        mappingContext);

        assertEquals(desiredFlaglessResult(0, 0, 1, Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY, 24,
                Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 2, 1, 1, 0, CLASSIFY_TABLE_INDEX, 1), request);
    }

    @Test
    public void testIpv4WithoutClassifier() {
        final IpAddDelRoute request =
                factory.createIpv4MultipathHopRequest(false, ROUTE_PROTOCOL_NAME, ipv4MutlipathRouteWithoutClassifier,
                        ipv4nextHopWithoutClassifier,
                        mappingContext);

        assertEquals(
                desiredFlaglessResult(0, 0, 1, Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY, 24,
                        Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 2, 1, 1, 0, 0, 0), request);
    }

    @Test
    public void testIpv4WithoutRouteAttrs() {
        final IpAddDelRoute request =
                factory.createIpv4MultipathHopRequest(false, ROUTE_PROTOCOL_NAME, ipv4MutlipathRouteWithoutRouteAtts,
                        ipv4nextHopWithoutRouteAtts,
                        mappingContext);

        assertEquals(
                desiredFlaglessResult(0, 0, 1, Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY, 24,
                        Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 2, 1, 1, 0, 0, 0), request);
    }
}
