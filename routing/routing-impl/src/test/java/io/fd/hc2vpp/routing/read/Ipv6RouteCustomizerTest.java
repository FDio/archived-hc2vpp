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
import io.fd.hc2vpp.routing.Ipv6RouteData;
import io.fd.hc2vpp.routing.RoutingConfiguration;
import io.fd.hc2vpp.routing.naming.Ipv6RouteNamesFactory;
import io.fd.hc2vpp.routing.trait.RouteMapper;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.vpp.jvpp.core.dto.Ip6FibDetails;
import io.fd.vpp.jvpp.core.dto.Ip6FibDetailsReplyDump;
import io.fd.vpp.jvpp.core.types.FibPath;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.StaticRoutes2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.RouteKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.NextHopOptions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.NextHopList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.SimpleNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.SpecialNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.next.hop.list.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.next.hop.list.NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.next.hop.list.NextHopKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.SpecialNextHopGrouping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.RoutingProtocols;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.RoutingProtocolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol.StaticRoutes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv6RouteCustomizerTest extends ListReaderCustomizerTest<Route, RouteKey, RouteBuilder>
        implements RouteMapper {

    private static final String ROUTING_PROTOCOL_PREFIX = "route-p-";
    private DumpCacheManager<Ip6FibDetailsReplyDump, Void> manager;

    @Mock
    private RoutingConfiguration configuration;

    @Mock
    private MultiNamingContext routeHopContext;

    @Mock
    private EntityDumpExecutor<Ip6FibDetailsReplyDump, Void> executor;

    @Mock
    private ModificationCache cache;

    private NamingContext interfaceContext;
    private NamingContext routesContext;
    private NamingContext routingProtocolContext;

    private InstanceIdentifier<Route> routeIdSpecialHop;
    private InstanceIdentifier<Route> routeIdSimpleHop;
    private InstanceIdentifier<Route> routeIdListHop;
    private Ipv6RouteNamesFactory factory;

    public Ipv6RouteCustomizerTest() {
        super(Route.class, Ipv6Builder.class);
    }

    @Override
    public void setUp() throws ReadFailedException {
        manager = new DumpCacheManager.DumpCacheManagerBuilder<Ip6FibDetailsReplyDump, Void>()
                .withExecutor(executor)
                .acceptOnly(Ip6FibDetailsReplyDump.class)
                .build();

        interfaceContext = new NamingContext("ifaces", "interface-context");
        routesContext = new NamingContext("routes", "route-context");
        routingProtocolContext = new NamingContext("routing-protocol", "routing-protocol-context");

        final InstanceIdentifier<Ipv6> ipv6InstanceIdentifier = InstanceIdentifier.create(RoutingProtocols.class)
                .child(RoutingProtocol.class, new RoutingProtocolKey(ROUTE_PROTOCOL_NAME))
                .child(StaticRoutes.class)
                .augmentation(StaticRoutes2.class)
                .child(Ipv6.class);

        routeIdSpecialHop = ipv6InstanceIdentifier.child(Route.class, new RouteKey(1L));
        routeIdSimpleHop = ipv6InstanceIdentifier.child(Route.class, new RouteKey(2L));
        routeIdListHop = ipv6InstanceIdentifier.child(Route.class, new RouteKey(3L));

        factory = new Ipv6RouteNamesFactory(interfaceContext, routingProtocolContext);


        final Ip6FibDetailsReplyDump replyDump = replyDump();
        when(executor.executeDump(routeIdSpecialHop, EntityDumpExecutor.NO_PARAMS)).thenReturn(replyDump);
        when(executor.executeDump(routeIdSimpleHop, EntityDumpExecutor.NO_PARAMS)).thenReturn(replyDump);
        when(executor.executeDump(routeIdListHop, EntityDumpExecutor.NO_PARAMS)).thenReturn(replyDump);

        defineMapping(mappingContext, "iface-1", 1, "interface-context");
        defineMapping(mappingContext, ROUTE_PROTOCOL_NAME, 1, "routing-protocol-context");
        defineMapping(mappingContext, factory.uniqueRouteName(replyDump.ip6FibDetails.get(0), mappingContext), 1,
                "route-context");
        defineMapping(mappingContext, factory.uniqueRouteName(replyDump.ip6FibDetails.get(1), mappingContext), 2,
                "route-context");

        Ip6FibDetails listRoute = replyDump.ip6FibDetails.get(2);
        String listRouteName = factory.uniqueRouteName(listRoute, mappingContext);
        defineMapping(mappingContext, listRouteName, 3, "route-context");

        when(routeHopContext.getChildIndex(listRouteName, factory.uniqueRouteHopName(listRoute.path[0], mappingContext),
                mappingContext))
                .thenReturn(0);
        when(routeHopContext.getChildIndex(listRouteName, factory.uniqueRouteHopName(listRoute.path[1], mappingContext),
                mappingContext))
                .thenReturn(1);
    }

    private Ip6FibDetailsReplyDump replyDump() {
        Ip6FibDetailsReplyDump replyDump = new Ip6FibDetailsReplyDump();

        // first is special
        Ip6FibDetails detail1 = new Ip6FibDetails();
        detail1.tableId = 1;
        detail1.address = Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY;
        detail1.addressLength = 24;

        FibPath path1 = new FibPath();
        path1.isLocal = 1;
        detail1.path = new FibPath[]{path1};


        //second is simple
        Ip6FibDetails detail2 = new Ip6FibDetails();
        detail2.tableId = 1;
        detail2.address = Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY;
        detail2.addressLength = 22;
        detail2.path = new FibPath[]{};

        FibPath path2 = new FibPath();
        path2.weight = 3;
        path2.nextHop = Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY;
        path2.afi = 0;
        path2.swIfIndex = 1;
        detail2.path = new FibPath[]{path2};

        // third is list
        Ip6FibDetails detail3 = new Ip6FibDetails();
        detail3.tableId = 1;
        detail3.address = Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY;
        detail3.addressLength = 16;

        FibPath path3 = new FibPath();
        path3.swIfIndex = 1;
        path3.weight = 1;
        path3.nextHop = Ipv6RouteData.FIRST_ADDRESS_AS_ARRAY;
        path3.afi = 0;

        FibPath path4 = new FibPath();
        path4.swIfIndex = 1;
        path4.weight = 2;
        path4.nextHop = Ipv6RouteData.SECOND_ADDRESS_AS_ARRAY;
        path4.afi = 0;

        detail3.path = new FibPath[]{path3, path4};

        replyDump.ip6FibDetails = Arrays.asList(detail1, detail2, detail3);
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
        assertEquals("2001:db8:a0b:12f0:0:0:0:1/24", builder.getDestinationPrefix().getValue());

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
        assertEquals("2001:db8:a0b:12f0:0:0:0:2/22", builder.getDestinationPrefix().getValue());

        NextHopOptions hopOptions = builder.getNextHopOptions();
        assertTrue(hopOptions instanceof SimpleNextHop);

        SimpleNextHop hop = SimpleNextHop.class.cast(hopOptions);
        assertEquals("2001:db8:a0b:12f0::1", hop.getNextHop().getValue());
        assertEquals("iface-1", hop.getOutgoingInterface());
    }

    @Test
    public void readCurrentAttributesListHop() throws Exception {


        final RouteBuilder builder = new RouteBuilder();
        getCustomizer().readCurrentAttributes(routeIdListHop, builder, ctx);

        assertEquals(3, builder.getId().intValue());
        assertEquals(3, builder.getKey().getId().intValue());
        assertEquals("2001:db8:a0b:12f0:0:0:0:2/16", builder.getDestinationPrefix().getValue());

        NextHopOptions hopOptions = builder.getNextHopOptions();
        assertTrue(hopOptions instanceof NextHopList);

        NextHopList hop = NextHopList.class.cast(hopOptions);
        List<NextHop> hops = hop.getNextHopList().getNextHop();

        assertThat(hops, hasSize(2));

        assertTrue(areEqual(hops.get(0), desiredHop(0L, "2001:db8:a0b:12f0::1", 1, "iface-1")));
        assertTrue(areEqual(hops.get(1), desiredHop(1L, "2001:db8:a0b:12f0::2", 2, "iface-1")));
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
                .setAddress(new Ipv6Address(address))
                .setWeight((short) weight)
                .setOutgoingInterface(iface)
                .setId(id)
                .setKey(new NextHopKey(id))
                .build();
    }

    @Override
    protected ReaderCustomizer<Route, RouteBuilder> initCustomizer() {
        return new Ipv6RouteCustomizer(manager, configuration, routeHopContext,
                interfaceContext, routesContext, routingProtocolContext);
    }
}