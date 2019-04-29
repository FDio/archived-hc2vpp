/*
 * Copyright (c) 2019 Cisco and/or its affiliates.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.jvpp.core.dto.IpAddDelRoute;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.control.plane.protocol.StaticRoutes;

@RunWith(HoneycombTestRunner.class)
public class TableLookupRequestFactoryIpv4Test
        implements RouteRequestProducer, RoutingRequestTestHelper, ClassifyTableTestHelper,
        SchemaContextTestHelper, NamingContextHelper {

    private static final String PARENT_PROTOCOL_4 = "parent-protocol-2";
    private static final int PARENT_PROTOCOL_4_INDEX = 2;
    private static final byte[] DEAGRAGATION_ADDRESS = {0, 0, 0, 0};
    private static final Ipv4Prefix IPV_4_PREFIX = new Ipv4Prefix("192.168.2.1/24");
    private static final int DST_PREFIX = 24;

    @Mock
    private VppClassifierContextManager classifierContextManager;

    @Mock
    private WriteContext writeContext;

    @Mock
    private MappingContext mappingContext;

    private TableLookupRequestFactory factory;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        final NamingContext interfaceContext = new NamingContext("iface", "interface-context");
        final NamingContext routingProtocolContextContext =
                new NamingContext("routingProtocol", "routing-protocol-context");
        factory = new TableLookupRequestFactory(classifierContextManager, interfaceContext,
                routingProtocolContextContext);

        addMapping(classifierContextManager, CLASSIFY_TABLE_NAME, CLASSIFY_TABLE_INDEX, mappingContext);
        defineMapping(mappingContext, PARENT_PROTOCOL_4, PARENT_PROTOCOL_4_INDEX, "routing-protocol-context");
        when(writeContext.getMappingContext()).thenReturn(mappingContext);
        when(writeContext.readAfter(any())).thenReturn(Optional.of(new TableBuilder().build()));
    }

    @Test
    public void testIpv4WithClassifier(
            @InjectTestData(resourcePath = "/ipv4/tablehop/tableHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH)
                    StaticRoutes routes) {
        final IpAddDelRoute request = factory.createV4TableLookupRouteRequest(true, PARENT_PROTOCOL_4,
                extractSingleRoute(routes, IPV_4_PREFIX), writeContext);

        assertEquals(
                desiredFlaglessResult(1, 0, 0, Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY, DST_PREFIX, DEAGRAGATION_ADDRESS,
                        -1, 0, 2, 0, 2, 1), request);
    }

    @Test
    public void testIpv4WithoutClassifier(
            @InjectTestData(resourcePath = "/ipv4/tablehop/tableHopRouteWithoutClassifier.json", id = STATIC_ROUTE_PATH)
                    StaticRoutes routes) {
        final IpAddDelRoute request = factory.createV4TableLookupRouteRequest(true, PARENT_PROTOCOL_4,
                extractSingleRoute(routes, IPV_4_PREFIX), writeContext);

        assertEquals(
                desiredFlaglessResult(1, 0, 0, Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY, DST_PREFIX, DEAGRAGATION_ADDRESS,
                        -1, 0, 2, 0, 0, 0), request);
    }

    private Route extractSingleRoute(final StaticRoutes staticRoutes, final Ipv4Prefix id) {
        return staticRoutes.augmentation(StaticRoutes1.class).getIpv4().getRoute().stream()
                .filter(route -> route.getDestinationPrefix().getValue().equals(id.getValue()))
                .collect(RWUtils.singleItemCollector());
    }
}
