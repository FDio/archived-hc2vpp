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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.Ipv4RouteData;
import io.fd.hc2vpp.routing.Ipv6RouteData;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.jvpp.core.dto.Ip6FibDetails;
import io.fd.jvpp.core.dto.Ip6FibDetailsReplyDump;
import io.fd.jvpp.core.dto.IpFibDetails;
import io.fd.jvpp.core.dto.IpFibDetailsReplyDump;
import io.fd.jvpp.core.types.FibPath;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.ControlPlaneProtocols;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.ControlPlaneProtocolsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocolBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocolKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ControlPlaneProtocolCustomizerTest
        extends ListReaderCustomizerTest<ControlPlaneProtocol, ControlPlaneProtocolKey, ControlPlaneProtocolBuilder>
        implements ByteDataTranslator {

    private static final String VPP_PROTOCOL_PREFIX = "vpp-protocol";

    @Mock
    private EntityDumpExecutor<IpFibDetailsReplyDump, Void> ipv4Executor;

    @Mock
    private EntityDumpExecutor<Ip6FibDetailsReplyDump, Void> ipv6Executor;
    private DumpCacheManager<IpFibDetailsReplyDump, Void> ipv4RoutesDumpManager;
    private DumpCacheManager<Ip6FibDetailsReplyDump, Void> ipv6RoutesDumpManager;

    private NamingContext routingProtocolContext;

    public ControlPlaneProtocolCustomizerTest() { super(ControlPlaneProtocol.class, ControlPlaneProtocolsBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        when(ipv4Executor.executeDump(any(), any())).thenReturn(replyDumpIpv4());
        when(ipv6Executor.executeDump(any(), any())).thenReturn(replyDumpIpv6());
        when(ctx.getModificationCache()).thenReturn(cache);

        ipv4RoutesDumpManager = new DumpCacheManager.DumpCacheManagerBuilder<IpFibDetailsReplyDump, Void>()
                .withExecutor(ipv4Executor)
                .acceptOnly(IpFibDetailsReplyDump.class)
                .build();

        ipv6RoutesDumpManager = new DumpCacheManager.DumpCacheManagerBuilder<Ip6FibDetailsReplyDump, Void>()
                .withExecutor(ipv6Executor)
                .acceptOnly(Ip6FibDetailsReplyDump.class)
                .build();

        routingProtocolContext = new NamingContext("routing-protocol", "routing-protocol-context");
        defineMapping(mappingContext, ROUTE_PROTOCOL_NAME, 1, "routing-protocol-context");
        defineMapping(mappingContext, "tst-protocol-2", 2, "routing-protocol-context");
        defineMapping(mappingContext, "tst-protocol-3", 3, "routing-protocol-context");
    }

    @Test
    public void getAllIds() throws Exception {
        final List<ControlPlaneProtocolKey> keys =
                getCustomizer().getAllIds(InstanceIdentifier.create(ControlPlaneProtocol.class), ctx);

        assertThat(keys, hasSize(3));
        assertThat(keys, hasItems(new ControlPlaneProtocolKey(ROUTE_PROTOCOL_NAME, Static.class),
                                  new ControlPlaneProtocolKey("tst-protocol-2", Static.class),
                                  new ControlPlaneProtocolKey("tst-protocol-3", Static.class)));
    }

    @Test
    public void readCurrentAttributes() throws Exception {
        final InstanceIdentifier<ControlPlaneProtocol> identifier =
            InstanceIdentifier.create(ControlPlaneProtocols.class)
                .child(ControlPlaneProtocol.class, new ControlPlaneProtocolKey(ROUTE_PROTOCOL_NAME, Static.class));

        final ControlPlaneProtocolBuilder builder = new ControlPlaneProtocolBuilder();
        getCustomizer().readCurrentAttributes(identifier, builder, ctx);

        assertEquals(ROUTE_PROTOCOL_NAME, builder.getName());
        assertEquals(ROUTE_PROTOCOL_NAME, builder.key().getName());
        assertEquals(Static.class, builder.getType());
    }

    @Override
    protected ReaderCustomizer<ControlPlaneProtocol, ControlPlaneProtocolBuilder> initCustomizer() {
        return new ControlPlaneProtocolCustomizer(routingProtocolContext, ipv4RoutesDumpManager, ipv6RoutesDumpManager);
    }

    private Ip6FibDetailsReplyDump replyDumpIpv6() {
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
        detail2.tableId = 2;
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

    private IpFibDetailsReplyDump replyDumpIpv4() {
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
        detail2.tableId = 3;
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
}
