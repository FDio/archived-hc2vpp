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

package io.fd.hc2vpp.routing.write;

import static io.fd.hc2vpp.routing.Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY;
import static io.fd.hc2vpp.routing.Ipv4RouteData.SECOND_ADDRESS_AS_ARRAY;
import static io.fd.hc2vpp.routing.helpers.InterfaceTestHelper.INTERFACE_INDEX;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.hc2vpp.routing.naming.Ipv4RouteNamesFactory;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.RouteKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.TableLookupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.control.plane.protocol.StaticRoutes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class Ipv4RouteCustomizerTest extends RouteCustomizerTest {

    private static final TableKey
            SEC_TABLE_KEY = new TableKey(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv4.class,
            SEC_TABLE_ID);
    private static final Table
            IPV4_TABLE = new TableBuilder().setName("VRF-IPV4-4")
            .withKey(SEC_TABLE_KEY).setTableId(SEC_TABLE_KEY.getTableId())
            .setAddressFamily(SEC_TABLE_KEY.getAddressFamily()).build();

    private static final InstanceIdentifier<Route> ROUTE_IID = CONTROL_PROTOCOL_IID
            .child(StaticRoutes.class)
            .augmentation(StaticRoutes1.class)
            .child(Ipv4.class)
            .child(Route.class);
    private static final InstanceIdentifier<Route> ROUTE_INVALID_IID = CONTROL_PROTOCOL_INVALID_IID
            .child(StaticRoutes.class)
            .augmentation(StaticRoutes1.class)
            .child(Ipv4.class)
            .child(Route.class);

    @Captor
    private ArgumentCaptor<IpAddDelRoute> requestCaptor;

    private Ipv4RouteCustomizer customizer;
    private Ipv4RouteNamesFactory namesFactory;

    @Override
    protected void setUpTest() throws Exception {
        super.setUpTest();
        customizer = new Ipv4RouteCustomizer(api, interfaceContext, new NamingContext("route", "route-context"),
                routingProtocolContext, routeHopContext, classifyManager);
        namesFactory = new Ipv4RouteNamesFactory(interfaceContext, routingProtocolContext);

        KeyedInstanceIdentifier<Table, TableKey> secondaryFibIid =
                FibManagementIIds.FM_FIB_TABLES.child(Table.class, SEC_TABLE_KEY);

        when(writeContext.readAfter(secondaryFibIid)).thenReturn(Optional.of(IPV4_TABLE));
    }

    @Test(expected = WriteFailedException.class)
    public void testWriteSingleHopInvalid(
            @InjectTestData(resourcePath = "/ipv4/simplehop/simpleHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        final Route route1 = getIpv4RouteWithId(route, new Ipv4Prefix("192.168.2.1/24"));
        noMappingDefined(mappingContext, namesFactory.uniqueRouteName(ROUTE_PROTOCOL_INVALID_NAME, route1),
                "route-context");

        customizer.writeCurrentAttributes(ROUTE_INVALID_IID, route1, writeContext);
        verifyInvocation(0, ImmutableList.of(), api, requestCaptor);
    }

    @Test
    public void testWriteSingleHop(
            @InjectTestData(resourcePath = "/ipv4/simplehop/simpleHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        final Route route1 = getIpv4RouteWithId(route, new Ipv4Prefix("192.168.2.1/24"));
        noMappingDefined(mappingContext, namesFactory.uniqueRouteName(ROUTE_PROTOCOL_NAME, route1), "route-context");

        customizer.writeCurrentAttributes(ROUTE_IID, route1, writeContext);
        verifyInvocation(1, ImmutableList
                        .of(desiredFlaglessResult(1, 0, 0, FIRST_ADDRESS_AS_ARRAY, 24,
                                SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 0, TABLE_ID.intValue(), 0,
                                CLASSIFY_TABLE_INDEX, 1)),
                api, requestCaptor);
    }

    //TODO - https://jira.fd.io/browse/HONEYCOMB-396
    @Test
    public void testWriteTableLookup() throws WriteFailedException {
        final Route route = new RouteBuilder()
                .withKey(new RouteKey(new Ipv4Prefix("192.168.2.1/24")))
                .setDestinationPrefix(new Ipv4Prefix("192.168.2.1/24"))
                .setNextHop(new NextHopBuilder().setNextHopOptions(
                    new TableLookupCaseBuilder()
                        .setSecondaryVrf(new VniReference(4L))
                            .setAddressFamily(
                                    org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv4.class)
                                .build()).build())
                .build();
        noMappingDefined(mappingContext, namesFactory.uniqueRouteName(ROUTE_PROTOCOL_NAME, route), "route-context");
        customizer.writeCurrentAttributes(ROUTE_IID, route, writeContext);
        verifyInvocation(1, ImmutableList
                        .of(desiredFlaglessResult(1, 0, 0, FIRST_ADDRESS_AS_ARRAY, 24,
                                new byte[4], ~0, 0, TABLE_ID.intValue(), 4,
                                0, 0)),
                api, requestCaptor);
    }

    @Test
    public void testWriteHopList(
            @InjectTestData(resourcePath = "/ipv4/multihop/multiHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH)
                StaticRoutes route)
            throws WriteFailedException {
        final Route route1 = getIpv4RouteWithId(route, new Ipv4Prefix("192.168.2.1/24"));
        noMappingDefined(mappingContext, namesFactory.uniqueRouteName(ROUTE_PROTOCOL_NAME, route1), "route-context");

        customizer.writeCurrentAttributes(ROUTE_IID, route1, writeContext);
        verifyInvocation(2,
                ImmutableList.of(
                        desiredFlaglessResult(1, 0, 1, FIRST_ADDRESS_AS_ARRAY, 24,
                                FIRST_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 2, TABLE_ID.intValue(), 0,
                                CLASSIFY_TABLE_INDEX, 1),
                        desiredFlaglessResult(1, 0, 1, FIRST_ADDRESS_AS_ARRAY, 24,
                                SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 3, TABLE_ID.intValue(), 0,
                                CLASSIFY_TABLE_INDEX, 1)), api,
                requestCaptor);

        verify(routeHopContext, times(1))
                .addChild(
                        namesFactory.uniqueRouteName(ROUTE_PROTOCOL_NAME, route1), 1,
                        namesFactory.uniqueRouteHopName(getHopWithId(route1, 1)),
                        mappingContext);
        verify(routeHopContext, times(1))
                .addChild(
                        namesFactory.uniqueRouteName(ROUTE_PROTOCOL_NAME, route1), 2,
                        namesFactory.uniqueRouteHopName(getHopWithId(route1, 2)),
                        mappingContext);
    }

    @Test
    public void testWriteSpecialHop(
            @InjectTestData(resourcePath = "/ipv4/specialhop/specialHopRouteBlackhole.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        final Route route1 = getIpv4RouteWithId(route, new Ipv4Prefix("192.168.2.1/24"));
        noMappingDefined(mappingContext, namesFactory.uniqueRouteName(ROUTE_PROTOCOL_NAME, route1), "route-context");

        customizer.writeCurrentAttributes(ROUTE_IID, route1, writeContext);
        verifyInvocation(1, ImmutableList
                        .of(desiredSpecialResult(1, 0, FIRST_ADDRESS_AS_ARRAY, 24, 1, 0, 0, 0,
                                TABLE_ID.intValue(), 0)),
                         api, requestCaptor);
    }

    @Test
    public void testUpdate(
            @InjectTestData(resourcePath = "/ipv4/specialhop/specialHopRouteBlackhole.json", id = STATIC_ROUTE_PATH) StaticRoutes route) {
        try {
            customizer.updateCurrentAttributes(ROUTE_IID,
                                               new RouteBuilder().build(),
                                               getIpv4RouteWithId(route,
                                                                  new Ipv4Prefix("192.168.2.1/24")), writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof UnsupportedOperationException);
            verifyNotInvoked(api);
            return;
        }
        fail("Test should have thrown exception");
    }


    @Test
    public void testDeleteSingleHop(
            @InjectTestData(resourcePath = "/ipv4/simplehop/simpleHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        customizer.deleteCurrentAttributes(ROUTE_IID,
                                           getIpv4RouteWithId(route,
                                                              new Ipv4Prefix("192.168.2.1/24")), writeContext);
        verifyInvocation(1, ImmutableList
                .of(desiredFlaglessResult(0, 0, 0, FIRST_ADDRESS_AS_ARRAY, 24,
                        SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX,
                        0, TABLE_ID.intValue(), 0, CLASSIFY_TABLE_INDEX, 1)),
                         api, requestCaptor);
    }

    @Test
    public void testDeleteHopList(
            @InjectTestData(resourcePath = "/ipv4/multihop/multiHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        final Route route1 = getIpv4RouteWithId(route, new Ipv4Prefix("192.168.2.1/24"));
        noMappingDefined(mappingContext, namesFactory.uniqueRouteName(ROUTE_PROTOCOL_NAME, route1), "route-context");

        customizer.deleteCurrentAttributes(ROUTE_IID, route1, writeContext);
        verifyInvocation(2,
                ImmutableList.of(
                        desiredFlaglessResult(0, 0, 1, FIRST_ADDRESS_AS_ARRAY, 24,
                                FIRST_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 2, TABLE_ID.intValue(), 0,
                                CLASSIFY_TABLE_INDEX, 1),
                        desiredFlaglessResult(0, 0, 1, FIRST_ADDRESS_AS_ARRAY, 24,
                                new byte[]{-64, -88, 2, 2}, INTERFACE_INDEX, 3, TABLE_ID.intValue(), 0,
                                CLASSIFY_TABLE_INDEX, 1)), api, requestCaptor);

        verify(routeHopContext, times(1))
                .removeChild(
                        namesFactory.uniqueRouteName(ROUTE_PROTOCOL_NAME, route1),
                        namesFactory.uniqueRouteHopName(getHopWithId(route1, 1)),
                        mappingContext);
        verify(routeHopContext, times(1))
                .removeChild(
                        namesFactory.uniqueRouteName(ROUTE_PROTOCOL_NAME, route1),
                        namesFactory.uniqueRouteHopName(getHopWithId(route1, 2)),
                        mappingContext);
    }

    @Test
    public void testDeleteSpecialHop(
            @InjectTestData(resourcePath = "/ipv4/specialhop/specialHopRouteBlackhole.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        customizer.deleteCurrentAttributes(ROUTE_IID, getIpv4RouteWithId(route, new Ipv4Prefix("192.168.2.1/24")),
                writeContext);

        verifyInvocation(1,
                ImmutableList.of(desiredSpecialResult(0, 0, FIRST_ADDRESS_AS_ARRAY, 24, 1, 0, 0, 0,
                        TABLE_ID.intValue(), 0)),
                         api, requestCaptor);
    }
}
