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
import io.fd.hc2vpp.routing.Ipv6RouteData;
import io.fd.hc2vpp.routing.helpers.ClassifyTableTestHelper;
import io.fd.hc2vpp.routing.helpers.InterfaceTestHelper;
import io.fd.hc2vpp.routing.helpers.RoutingRequestTestHelper;
import io.fd.hc2vpp.routing.helpers.SchemaContextTestHelper;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.NextHopList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.next.hop.list.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol.StaticRoutes;

@RunWith(HoneycombTestRunner.class)
public class MultipathHopRequestFactoryIpv6Test
        implements RoutingRequestTestHelper, ClassifyTableTestHelper, InterfaceTestHelper, SchemaContextTestHelper {

    @Mock
    private VppClassifierContextManager classifierContextManager;

    @Mock
    private MappingContext mappingContext;

    @InjectTestData(resourcePath = "/ipv6/multihop/multiHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH)
    private StaticRoutes ipv6StaticRoutesWithClassifier;

    @InjectTestData(resourcePath = "/ipv6/multihop/multiHopRouteWithoutClassifier.json", id = STATIC_ROUTE_PATH)
    private StaticRoutes ipv6StaticRoutesWithoutClassifier;

    private Route ipv6MultipathRouteWithClassifier;
    private NextHop ipv6nextHopForClassified;

    private Route ipv6MultipathRouteWithoutClassifier;
    private NextHop ipv6nextHopForNonClassified;

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

        ipv6MultipathRouteWithClassifier = getIpv6RouteWithId(ipv6StaticRoutesWithClassifier, 1L);
        ipv6MultipathRouteWithoutClassifier = getIpv6RouteWithId(ipv6StaticRoutesWithoutClassifier, 1L);

        final List<NextHop> ipv6HopsClassified =
                NextHopList.class.cast(ipv6MultipathRouteWithClassifier.getNextHopOptions()).getNextHopList()
                        .getNextHop();

        final List<NextHop> ipv6HopsNonClassified =
                NextHopList.class.cast(ipv6MultipathRouteWithoutClassifier.getNextHopOptions()).getNextHopList()
                        .getNextHop();

        ipv6nextHopForClassified = ipv6HopsClassified.stream()
                .filter(nextHop -> nextHop.getId() == 1L)
                .findFirst().get();
        ipv6nextHopForNonClassified = ipv6HopsNonClassified.stream()
                .filter(nextHop -> nextHop.getId() == 1L)
                .findFirst().get();
    }

    @Test
    public void testIpv6WithClassifier() {
        final IpAddDelRoute request =
                factory.createIpv6MultipathHopRequest(false, ROUTE_PROTOCOL_NAME, ipv6MultipathRouteWithClassifier,
                        ipv6nextHopForClassified,
                        mappingContext);

        assertEquals(
                desiredFlaglessResult(0, 1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                        Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 2, 1, 1, 0, CLASSIFY_TABLE_INDEX, 1),
                request);
    }

    @Test
    public void testIpv6WithoutClassifier() {
        final IpAddDelRoute request =
                factory.createIpv6MultipathHopRequest(false, ROUTE_PROTOCOL_NAME, ipv6MultipathRouteWithoutClassifier,
                        ipv6nextHopForNonClassified,
                        mappingContext);

        assertEquals(
                desiredFlaglessResult(0, 1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                        Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 2, 1, 1, 0, 0, 0), request);
    }


}
