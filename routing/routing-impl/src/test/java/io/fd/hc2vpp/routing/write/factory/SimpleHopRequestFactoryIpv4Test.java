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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol.StaticRoutes;

@RunWith(HoneycombTestRunner.class)
public class SimpleHopRequestFactoryIpv4Test
        implements RoutingRequestTestHelper, ClassifyTableTestHelper, InterfaceTestHelper, SchemaContextTestHelper {
    @Mock
    private VppClassifierContextManager classifierContextManager;

    @Mock
    private MappingContext mappingContext;

    private NamingContext interfaceContext;
    private NamingContext routingProtocolContext;
    private SimpleHopRequestFactory factory;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        interfaceContext = new NamingContext("interface", "interface-context");
        routingProtocolContext = new NamingContext("routing-protocol", "routing-protocol-context");
        factory =
                SimpleHopRequestFactory.forContexts(classifierContextManager, interfaceContext, routingProtocolContext);

        addMapping(classifierContextManager, CLASSIFY_TABLE_NAME, CLASSIFY_TABLE_INDEX, mappingContext);
        defineMapping(mappingContext, INTERFACE_NAME, INTERFACE_INDEX, "interface-context");
        defineMapping(mappingContext, ROUTE_PROTOCOL_NAME, 1, "routing-protocol-context");
    }

    @Test
    public void testIpv4WithClassifier(
            @InjectTestData(resourcePath = "/ipv4/simplehop/simpleHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH)
                    StaticRoutes ipv4StaticRouteWithClassifier) {
        final IpAddDelRoute request =
                factory.createIpv4SimpleHopRequest(false, ROUTE_PROTOCOL_NAME,
                        getIpv4RouteWithId(ipv4StaticRouteWithClassifier, 1L),
                        mappingContext);

        assertEquals(desiredFlaglessResult(0, 0, 0, Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY, 24,
                Ipv4RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 0,
                1, 1, 0, CLASSIFY_TABLE_INDEX, 1), request);
    }

    @Test
    public void testIpv4WithoutClassifier(
            @InjectTestData(resourcePath = "/ipv4/simplehop/simpleHopRouteWithoutClassifier.json", id = STATIC_ROUTE_PATH)
                    StaticRoutes ipv4StaticRouteWithoutClassifier) {
        final IpAddDelRoute request =
                factory.createIpv4SimpleHopRequest(false, ROUTE_PROTOCOL_NAME,
                        ipv4StaticRouteWithoutClassifier.getAugmentation(StaticRoutes1.class).getIpv4().getRoute()
                                .get(0), mappingContext);

        assertEquals(
                desiredFlaglessResult(0, 0, 0, Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY, 24,
                        Ipv4RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 0, 1, 1, 0, 0, 0), request);
    }

    @Test
    public void testIpv4WithoutVppAttrs(
            @InjectTestData(resourcePath = "/ipv4/simplehop/simpleHopRouteNoRouteAttrs.json", id = STATIC_ROUTE_PATH)
                    StaticRoutes ipv4StaticRouteWithoutRouteAttrs) {
        final IpAddDelRoute request =
                factory.createIpv4SimpleHopRequest(false, ROUTE_PROTOCOL_NAME,
                        ipv4StaticRouteWithoutRouteAttrs.getAugmentation(StaticRoutes1.class).getIpv4().getRoute()
                                .get(0), mappingContext);

        assertEquals(
                desiredFlaglessResult(0, 0, 0, Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY, 24,
                        Ipv4RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 0, 1, 1, 0, 0, 0), request);
    }
}
