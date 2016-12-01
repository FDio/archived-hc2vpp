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

package io.fd.hc2vpp.routing.read;

import static io.fd.hc2vpp.routing.helpers.RoutingRequestTestHelper.ROUTE_PROTOCOL_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.Ipv4RouteData;
import io.fd.hc2vpp.routing.RoutingConfiguration;
import io.fd.hc2vpp.routing.naming.Ipv4RouteNamesFactory;
import io.fd.hc2vpp.routing.trait.RouteMapper;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.vpp.jvpp.core.dto.IpFibDetails;
import io.fd.vpp.jvpp.core.dto.IpFibDetailsReplyDump;
import io.fd.vpp.jvpp.core.types.FibPath;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.StaticRoutes2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.RouteKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.NextHopOptions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.NextHopList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SimpleNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SpecialNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.next.hop.list.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.next.hop.list.NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.next.hop.list.NextHopKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.SpecialNextHopGrouping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.RoutingProtocols;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.RoutingProtocolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol.StaticRoutes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv4RouteCustomizerTest extends ListReaderCustomizerTest<Route, RouteKey, RouteBuilder>
        implements RouteMapper {

    private final InstanceIdentifier<Ipv4> ipv4InstanceIdentifier = InstanceIdentifier.create(RoutingProtocols.class)
            .child(RoutingProtocol.class, new RoutingProtocolKey(ROUTE_PROTOCOL_NAME))
            .child(StaticRoutes.class)
            .augmentation(StaticRoutes2.class)
            .child(Ipv4.class);
    private DumpCacheManager<IpFibDetailsReplyDump, Void> manager;
    @Mock
    private RoutingConfiguration configuration;
    @Mock
    private MultiNamingContext routeHopContext;
    @Mock
    private EntityDumpExecutor<IpFibDetailsReplyDump, Void> executor;
    private NamingContext interfaceContext = new NamingContext("ifaces", "interface-context");
    private NamingContext routesContext = new NamingContext("routes", "route-context");
    private NamingContext routingProtocolContext = new NamingContext("routing-protocol", "routing-protocol-context");
    private InstanceIdentifier<Route> routeIdSpecialHop = ipv4InstanceIdentifier.child(Route.class, new RouteKey(1L));
    private InstanceIdentifier<Route> routeIdSimpleHop = ipv4InstanceIdentifier.child(Route.class, new RouteKey(2L));
    private InstanceIdentifier<Route> routeIdListHop = ipv4InstanceIdentifier.child(Route.class, new RouteKey(3L));


    public Ipv4RouteCustomizerTest() {
        super(Route.class, Ipv4Builder.class);
    }

    @Override
    public void setUp() throws ReadFailedException {
        manager = new DumpCacheManager.DumpCacheManagerBuilder<IpFibDetailsReplyDump, Void>()
                .withExecutor(executor)
                .acceptOnly(IpFibDetailsReplyDump.class)
                .build();

        final IpFibDetailsReplyDump replyDump = replyDump();
        when(executor.executeDump(routeIdSpecialHop, EntityDumpExecutor.NO_PARAMS)).thenReturn(replyDump);
        when(executor.executeDump(routeIdSimpleHop, EntityDumpExecutor.NO_PARAMS)).thenReturn(replyDump);
        when(executor.executeDump(routeIdListHop, EntityDumpExecutor.NO_PARAMS)).thenReturn(replyDump);

        IpFibDetails listRoute = replyDump.ipFibDetails.get(2);
        final Ipv4RouteNamesFactory factory = new Ipv4RouteNamesFactory(interfaceContext, routingProtocolContext);

        defineMapping(mappingContext, ROUTE_PROTOCOL_NAME, 1, "routing-protocol-context");
        defineMapping(mappingContext, "iface-1", 1, "interface-context");
        defineMapping(mappingContext, factory.uniqueRouteName(replyDump.ipFibDetails.get(0), mappingContext), 1,
                "route-context");
        defineMapping(mappingContext, factory.uniqueRouteName(replyDump.ipFibDetails.get(1), mappingContext), 2,
                "route-context");

        String listRouteName = factory.uniqueRouteName(listRoute, mappingContext);
        defineMapping(mappingContext, listRouteName, 3, "route-context");

        when(routeHopContext.getChildIndex(listRouteName, factory.uniqueRouteHopName(listRoute.path[0], mappingContext),
                mappingContext)).thenReturn(0);
        when(routeHopContext.getChildIndex(listRouteName, factory.uniqueRouteHopName(listRoute.path[1], mappingContext),
                mappingContext)).thenReturn(1);
    }

    private IpFibDetailsReplyDump replyDump() {
        IpFibDetailsReplyDump replyDump = new IpFibDetailsReplyDump();

        // first is special
        IpFibDetails detail1 = new IpFibDetails();
        detail1.tableId = 1;
        detail1.address = Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY;
        detail1.addressLength = 24;

        FibPath path1 = new FibPath();
        path1.isLocal = 1;
        detail1.path = new FibPath[]{path1};


        //second is simple
        IpFibDetails detail2 = new IpFibDetails();
        detail2.tableId = 1;
        detail2.address = Ipv4RouteData.SECOND_ADDRESS_AS_ARRAY;
        detail2.addressLength = 22;
        detail2.path = new FibPath[]{};

        FibPath path2 = new FibPath();
        path2.weight = 3;
        path2.nextHop = Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY;
        path2.afi = 0;
        path2.swIfIndex = 1;
        detail2.path = new FibPath[]{path2};

        // third is list
        IpFibDetails detail3 = new IpFibDetails();
        detail3.tableId = 1;
        detail3.address = Ipv4RouteData.SECOND_ADDRESS_AS_ARRAY;
        detail3.addressLength = 16;

        FibPath path3 = new FibPath();
        path3.swIfIndex = 1;
        path3.weight = 1;
        path3.nextHop = Ipv4RouteData.FIRST_ADDRESS_AS_ARRAY;
        path3.afi = 0;

        FibPath path4 = new FibPath();
        path4.swIfIndex = 1;
        path4.weight = 2;
        path4.nextHop = Ipv4RouteData.SECOND_ADDRESS_AS_ARRAY;
        path4.afi = 0;

        detail3.path = new FibPath[]{path3, path4};

        replyDump.ipFibDetails = Arrays.asList(detail1, detail2, detail3);
        return replyDump;
    }

    @Test
    public void getAllIds() throws Exception {
        final List<RouteKey> keys = getCustomizer().getAllIds(routeIdSpecialHop, ctx);

        assertThat(keys, hasSize(3));
        assertThat(keys, hasItems(new RouteKey(1L), new RouteKey(2L), new RouteKey(3L)));
    }

    @Test
    public void readCurrentAttributesSpecialHop() throws Exception {
        final RouteBuilder builder = new RouteBuilder();
        getCustomizer().readCurrentAttributes(routeIdSpecialHop, builder, ctx);

        assertEquals(1, builder.getId().intValue());
        assertEquals(1, builder.getKey().getId().intValue());
        assertEquals(new Ipv4Prefix("192.168.2.1/24"), builder.getDestinationPrefix());

        NextHopOptions hopOptions = builder.getNextHopOptions();
        assertTrue(hopOptions instanceof SpecialNextHop);

        SpecialNextHop hop = SpecialNextHop.class.cast(hopOptions);
        assertEquals(SpecialNextHopGrouping.SpecialNextHop.Receive, hop.getSpecialNextHop());
    }

    @Test
    public void readCurrentAttributesSimpleHop() throws Exception {
        final RouteBuilder builder = new RouteBuilder();
        getCustomizer().readCurrentAttributes(routeIdSimpleHop, builder, ctx);

        assertEquals(2, builder.getId().intValue());
        assertEquals(2, builder.getKey().getId().intValue());
        assertEquals(new Ipv4Prefix("192.168.2.2/22"), builder.getDestinationPrefix());

        NextHopOptions hopOptions = builder.getNextHopOptions();
        assertTrue(hopOptions instanceof SimpleNextHop);

        SimpleNextHop hop = SimpleNextHop.class.cast(hopOptions);
        assertEquals("192.168.2.1", hop.getNextHop().getValue());
        assertEquals("iface-1", hop.getOutgoingInterface());
    }

    @Test
    public void readCurrentAttributesListHop() throws Exception {

        final RouteBuilder builder = new RouteBuilder();
        getCustomizer().readCurrentAttributes(routeIdListHop, builder, ctx);

        assertEquals(3, builder.getId().intValue());
        assertEquals(3, builder.getKey().getId().intValue());
        assertEquals(new Ipv4Prefix("192.168.2.2/16"), builder.getDestinationPrefix());

        NextHopOptions hopOptions = builder.getNextHopOptions();
        assertTrue(hopOptions instanceof NextHopList);

        NextHopList hop = NextHopList.class.cast(hopOptions);
        List<NextHop> hops = hop.getNextHopList().getNextHop();

        assertThat(hops, hasSize(2));

        assertTrue(areEqual(hops.get(0), desiredHop(0L, "192.168.2.1", 1, "iface-1")));
        assertTrue(areEqual(hops.get(1), desiredHop(1L, "192.168.2.2", 2, "iface-1")));
    }

    private boolean areEqual(final NextHop first, final NextHop second) {
        return new EqualsBuilder()
                .append(true, first.getAddress().getValue().equals(second.getAddress().getValue()))
                .append(true, first.getId().equals(second.getId()))
                .append(true, first.getKey().equals(second.getKey()))
                .append(true, first.getOutgoingInterface().equals(second.getOutgoingInterface()))
                .isEquals();
    }

    private NextHop desiredHop(final long id, final String address, final int weight, final String iface) {
        return new NextHopBuilder()
                .setAddress(new Ipv4Address(address))
                .setWeight((short) weight)
                .setOutgoingInterface(iface)
                .setId(id)
                .setKey(new NextHopKey(id))
                .build();
    }

    @Override
    protected ReaderCustomizer<Route, RouteBuilder> initCustomizer() {
        return new Ipv4RouteCustomizer(manager, configuration, routeHopContext, interfaceContext,
                routesContext, routingProtocolContext);
    }
}