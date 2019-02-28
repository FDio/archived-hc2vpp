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

import static io.fd.hc2vpp.routing.helpers.InterfaceTestHelper.INTERFACE_INDEX;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.hc2vpp.routing.Ipv6RouteData;
import io.fd.hc2vpp.routing.naming.Ipv6RouteNamesFactory;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.IpAddDelRoute;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.RouteKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.route.NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.TableLookupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.control.plane.protocol.StaticRoutes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class Ipv6RouteCustomizerTest extends RouteCustomizerTest {

    private static final TableKey
            SEC_TABLE_KEY = new TableKey(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv6.class,
            SEC_TABLE_ID);
    private static final Table
            IPV6_TABLE = new TableBuilder().setName("VRF-IPV6-4")
            .withKey(SEC_TABLE_KEY).setTableId(SEC_TABLE_KEY.getTableId())
            .setAddressFamily(SEC_TABLE_KEY.getAddressFamily()).build();
    private static final Ipv6Prefix IPV_6_PREFIX = new Ipv6Prefix("2001:0db8:0a0b:12f0:0000:0000:0000:0001/64");
    private static final Ipv6Prefix IPV_6_PREFIX_128 = new Ipv6Prefix("2001:0db8:0a0b:12f0:0000:0000:0000:0001/128");

    private static final InstanceIdentifier<Route> ROUTE_IID = CONTROL_PROTOCOL_IID
            .child(StaticRoutes.class)
            .augmentation(StaticRoutes1.class)
            .child(Ipv6.class)
            .child(Route.class);
    private static final InstanceIdentifier<Route> ROUTE_INVALID_IID = CONTROL_PROTOCOL_INVALID_IID
            .child(StaticRoutes.class)
            .augmentation(StaticRoutes1.class)
            .child(Ipv6.class)
            .child(Route.class);
    @Captor
    private ArgumentCaptor<IpAddDelRoute> requestCaptor;

    private Ipv6RouteCustomizer customizer;
    private Ipv6RouteNamesFactory namesFactory;

    @Override
    protected void setUpTest() throws Exception {
        super.setUpTest();
        customizer = new Ipv6RouteCustomizer(api, interfaceContext, routingProtocolContext, routingProtocolContext,
                routeHopContext, classifyManager);
        namesFactory = new Ipv6RouteNamesFactory(interfaceContext, routingProtocolContext);

        KeyedInstanceIdentifier<Table, TableKey> secondaryFibIid =
                FibManagementIIds.FM_FIB_TABLES.child(Table.class, SEC_TABLE_KEY);
        when(writeContext.readAfter(secondaryFibIid)).thenReturn(Optional.of(IPV6_TABLE));
    }

    @Test(expected = WriteFailedException.class)
    public void testWriteSingleInvalidHop(
            @InjectTestData(resourcePath = "/ipv6/simplehop/simpleHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        whenAddRouteThenSuccess(api);
        customizer.writeCurrentAttributes(ROUTE_INVALID_IID, getIpv6RouteWithId(route, IPV_6_PREFIX), writeContext);
        verifyInvocation(0, ImmutableList.of(), api, requestCaptor);
    }

    @Test
    public void testWriteSingleHop(
            @InjectTestData(resourcePath = "/ipv6/simplehop/simpleHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        whenAddRouteThenSuccess(api);
        customizer.writeCurrentAttributes(ROUTE_IID, getIpv6RouteWithId(route, IPV_6_PREFIX), writeContext);
        verifyInvocation(1, ImmutableList
                .of(desiredFlaglessResult(1, 1, 0, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                        Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 0, TABLE_ID.intValue(),
                        0, CLASSIFY_TABLE_INDEX, 1)), api, requestCaptor);
    }

    @Test
    public void testWriteSingleHop128(
            @InjectTestData(resourcePath = "/ipv6/simplehop/simpleHopRoute128.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        whenAddRouteThenSuccess(api);
        customizer.writeCurrentAttributes(ROUTE_IID, getIpv6RouteWithId(route, IPV_6_PREFIX_128), writeContext);
        verifyInvocation(1, ImmutableList
                .of(desiredFlaglessResult(1, 1, 0, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 128,
                        Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 0, TABLE_ID.intValue(),
                        0, CLASSIFY_TABLE_INDEX, 1)), api, requestCaptor);
    }

    //TODO - https://jira.fd.io/browse/HONEYCOMB-396
    @Test
    public void testWriteTableLookup() throws WriteFailedException {
        final Route route = new RouteBuilder()
                .withKey(new RouteKey(IPV_6_PREFIX))
                .setDestinationPrefix(IPV_6_PREFIX)
                .setNextHop(new NextHopBuilder().setNextHopOptions(new TableLookupCaseBuilder()
                        .setSecondaryVrf(new VniReference(4L))
                        .setAddressFamily(
                                org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv6.class)
                        .build()).build())
                .build();
        whenAddRouteThenSuccess(api);
        noMappingDefined(mappingContext, namesFactory.uniqueRouteName(ROUTE_PROTOCOL_NAME, route), "route-context");
        customizer.writeCurrentAttributes(ROUTE_IID, route, writeContext);
        verifyInvocation(1, ImmutableList
                        .of(desiredFlaglessResult(1, 1, 0, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                                new byte[4], ~0, 0, TABLE_ID.intValue(), 4,
                                0, 0)),
                api, requestCaptor);
    }

    @Test
    public void testWriteHopList(
            @InjectTestData(resourcePath = "/ipv6/multihop/multiHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        whenAddRouteThenSuccess(api);
        customizer.writeCurrentAttributes(ROUTE_IID, getIpv6RouteWithId(route, IPV_6_PREFIX), writeContext);
        verifyInvocation(2,
                ImmutableList.of(
                        desiredFlaglessResult(1, 1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                                Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 2, TABLE_ID.intValue(), 0,
                                CLASSIFY_TABLE_INDEX, 1),
                        desiredFlaglessResult(1, 1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                                Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 2, TABLE_ID.intValue(), 0,
                                CLASSIFY_TABLE_INDEX, 1)), api,
                requestCaptor);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() throws WriteFailedException {
        customizer.updateCurrentAttributes(ROUTE_IID, Ipv6RouteData.IPV6_ROUTE_WITH_CLASSIFIER_BLACKHOLE_HOP,
                Ipv6RouteData.IPV6_ROUTE_WITH_CLASSIFIER_RECEIVE_HOP, writeContext);
    }

    @Test
    public void testDeleteSpecialHop(
            @InjectTestData(resourcePath = "/ipv6/specialhop/specialHopRouteBlackhole.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        whenAddRouteThenSuccess(api);
        customizer.deleteCurrentAttributes(ROUTE_IID, getIpv6RouteWithId(route, IPV_6_PREFIX),
                writeContext);
        verifyInvocation(1, ImmutableList
                        .of(desiredSpecialResult(0, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                                1, 0, 0, 0, TABLE_ID.intValue(), 0)),
                         api, requestCaptor);
    }

    @Test
    public void testDeleteSingleHop(
            @InjectTestData(resourcePath = "/ipv6/simplehop/simpleHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        whenAddRouteThenSuccess(api);
        customizer.deleteCurrentAttributes(ROUTE_IID, getIpv6RouteWithId(route, IPV_6_PREFIX), writeContext);
        verifyInvocation(1, ImmutableList
                .of(desiredFlaglessResult(0, 1, 0, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                        Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 0, 1,
                    0, CLASSIFY_TABLE_INDEX, 1)), api, requestCaptor);
    }

    @Test
    public void testDeleteHopList(
            @InjectTestData(resourcePath = "/ipv6/multihop/multiHopRouteWithClassifier.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        whenAddRouteThenSuccess(api);
        customizer.deleteCurrentAttributes(ROUTE_IID, getIpv6RouteWithId(route, IPV_6_PREFIX), writeContext);
        verifyInvocation(2,
                ImmutableList.of(
                        desiredFlaglessResult(0, 1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                                Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 2, 1, 0,
                                CLASSIFY_TABLE_INDEX, 1),
                        desiredFlaglessResult(0, 1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                                Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY, INTERFACE_INDEX, 2, 1, 0,
                                CLASSIFY_TABLE_INDEX, 1)), api,
                requestCaptor);
    }

    @Test
    public void testWriteSpecialHop(
            @InjectTestData(resourcePath = "/ipv6/specialhop/specialHopRouteBlackhole.json", id = STATIC_ROUTE_PATH) StaticRoutes route)
            throws WriteFailedException {
        whenAddRouteThenSuccess(api);
        customizer.writeCurrentAttributes(ROUTE_IID, getIpv6RouteWithId(route, IPV_6_PREFIX),
                writeContext);
        verifyInvocation(1, ImmutableList
                        .of(desiredSpecialResult(1, 1, Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY, 64,
                                1, 0, 0, 0, TABLE_ID.intValue(), 0)),
                         api, requestCaptor);
    }
}
