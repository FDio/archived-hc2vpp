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
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.SpecialNextHop.SpecialNextHopEnum.Receive;

import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.Ipv6RouteData;
import io.fd.hc2vpp.routing.RoutingConfiguration;
import io.fd.hc2vpp.routing.helpers.ClassifyTableTestHelper;
import io.fd.hc2vpp.routing.naming.Ipv6RouteNamesFactory;
import io.fd.hc2vpp.routing.trait.RouteMapper;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.RouteKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.route.next.hop.NextHop1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.route.next.hop.NextHop1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.route.next.hop.SimpleNextHop1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.NextHopOptions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.NextHopList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.SimpleNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.SpecialNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.next.hop.list.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.next.hop.list.NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.next.hop.list.NextHopKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.ControlPlaneProtocols;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.control.plane.protocol.StaticRoutes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.ipv6.unicast.routing.rev180319.VppIpv6NextHopAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.ipv6.unicast.routing.rev180319.VppIpv6NextHopAugmentationBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv6RouteCustomizerTest extends ListReaderCustomizerTest<Route, RouteKey, RouteBuilder>
        implements RouteMapper, ClassifyTableTestHelper {

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

    @Mock
    private VppClassifierContextManager classifyManager;

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

        final InstanceIdentifier<Ipv6> ipv6InstanceIdentifier = InstanceIdentifier.create(ControlPlaneProtocols.class)
                .child(ControlPlaneProtocol.class, new ControlPlaneProtocolKey(ROUTE_PROTOCOL_NAME, Static.class))
                .child(StaticRoutes.class)
                .augmentation(StaticRoutes1.class)
                .child(Ipv6.class);

        routeIdSpecialHop =
            ipv6InstanceIdentifier.child(Route.class, new RouteKey(new Ipv6Prefix("2001:db8:a0b:12f0::1/24")));
        routeIdSimpleHop =
            ipv6InstanceIdentifier.child(Route.class, new RouteKey(new Ipv6Prefix("2001:db8:a0b:12f0::2/22")));
        routeIdListHop =
            ipv6InstanceIdentifier.child(Route.class, new RouteKey(new Ipv6Prefix("2001:db8:a0b:12f0::2/16")));

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

        addMapping(classifyManager, CLASSIFY_TABLE_NAME, CLASSIFY_TABLE_INDEX, mappingContext);

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
        assertThat(keys, hasItems(new RouteKey(new Ipv6Prefix("2001:db8:a0b:12f0::1/24")),
                                  new RouteKey(new Ipv6Prefix("2001:db8:a0b:12f0::2/22")),
                                  new RouteKey(new Ipv6Prefix("2001:db8:a0b:12f0::2/16"))));
    }

    @Test
    public void readCurrentAttributesSpecialHop() throws Exception {
        final RouteBuilder builder = new RouteBuilder();
        getCustomizer().readCurrentAttributes(routeIdSpecialHop, builder, ctx);

        assertEquals(new Ipv6Prefix("2001:db8:a0b:12f0::1/24"), builder.getDestinationPrefix());
        assertEquals("2001:db8:a0b:12f0::1/24", builder.getDestinationPrefix().getValue());

        NextHopOptions hopOptions = builder.getNextHop().getNextHopOptions();
        assertTrue(hopOptions instanceof SpecialNextHop);

        SpecialNextHop hop = SpecialNextHop.class.cast(hopOptions);
        assertEquals(Receive, hop.getSpecialNextHopEnum());
    }

    @Test
    public void readCurrentAttributesSimpleHop() throws Exception {
        final RouteBuilder builder = new RouteBuilder();
        getCustomizer().readCurrentAttributes(routeIdSimpleHop, builder, ctx);

        assertEquals(new Ipv6Prefix("2001:db8:a0b:12f0::2/22"), builder.getDestinationPrefix());
        assertEquals("2001:db8:a0b:12f0::2/22", builder.getDestinationPrefix().getValue());

        NextHopOptions hopOptions = builder.getNextHop().getNextHopOptions();
        assertTrue(hopOptions instanceof SimpleNextHop);

        SimpleNextHop hop = SimpleNextHop.class.cast(hopOptions);
        assertEquals("2001:db8:a0b:12f0::1", hop.getAugmentation(SimpleNextHop1.class)
            .getNextHopAddress().getValue());
        assertEquals("iface-1", hop.getOutgoingInterface());
    }

    @Test
    public void readCurrentAttributesListHop() throws Exception {


        final RouteBuilder builder = new RouteBuilder();
        getCustomizer().readCurrentAttributes(routeIdListHop, builder, ctx);

        assertEquals(new Ipv6Prefix("2001:db8:a0b:12f0::2/16"), builder.getDestinationPrefix());
        assertEquals("2001:db8:a0b:12f0::2/16", builder.getDestinationPrefix().getValue());

        NextHopOptions hopOptions = builder.getNextHop().getNextHopOptions();
        assertTrue(hopOptions instanceof NextHopList);

        NextHopList hop = NextHopList.class.cast(hopOptions);
        List<NextHop> hops = hop.getNextHopList().getNextHop();

        assertThat(hops, hasSize(2));

        assertTrue(areEqual(hops.get(0), desiredHop("0", "2001:db8:a0b:12f0::1", 1, "iface-1")));
        assertTrue(areEqual(hops.get(1), desiredHop("1", "2001:db8:a0b:12f0::2", 2, "iface-1")));
    }

    private boolean areEqual(final NextHop first, final NextHop second) {
        return new EqualsBuilder()
                .append(true, first.getAugmentation(NextHop1.class).getNextHopAddress().getValue()
                    .equals(second.getAugmentation(NextHop1.class).getNextHopAddress().getValue()))
                .append(true, first.getIndex().equals(second.getIndex()))
                .append(true, first.getKey().equals(second.getKey()))
                .append(true, first.getOutgoingInterface().equals(second.getOutgoingInterface()))
                .isEquals();
    }

    private NextHop desiredHop(final String id, final String address, final int weight, final String iface) {
        return new NextHopBuilder()
            .setOutgoingInterface(iface)
            .setIndex(id)
            .setKey(new NextHopKey(id))
            .addAugmentation(NextHop1.class, new NextHop1Builder().setNextHopAddress(new Ipv6Address(address)).build())
            .addAugmentation(VppIpv6NextHopAugmentation.class,
                             new VppIpv6NextHopAugmentationBuilder().setWeight((short) weight).build())
            .build();
    }

    @Override
    protected ReaderCustomizer<Route, RouteBuilder> initCustomizer() {
        return new Ipv6RouteCustomizer(manager, configuration, routeHopContext,
                                       interfaceContext, routesContext, routingProtocolContext);
    }
}