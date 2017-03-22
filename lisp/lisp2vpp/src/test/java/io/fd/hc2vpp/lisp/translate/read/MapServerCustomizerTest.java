/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.lisp.translate.read;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.LispMapServerDetails;
import io.fd.vpp.jvpp.core.dto.LispMapServerDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.LispMapServerDump;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.MapServers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.MapServersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.map.servers.MapServer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.map.servers.MapServerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.map.servers.MapServerKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MapServerCustomizerTest
        extends LispInitializingListReaderCustomizerTest<MapServer, MapServerKey, MapServerBuilder>
        implements LispInitTest {

    private static final MapServerKey
            SERVER_KEY = new MapServerKey(new IpAddress(new Ipv4Address("192.168.2.1")));
    private static final InstanceIdentifier<MapServer> STATE_IID = LISP_STATE_FTR_IID.child(MapServers.class)
            .child(MapServer.class, SERVER_KEY);
    private static final InstanceIdentifier<MapServer> CONFIG_IID = LISP_FTR_IID.child(MapServers.class)
            .child(MapServer.class, SERVER_KEY);

    public MapServerCustomizerTest() {
        super(MapServer.class, MapServersBuilder.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        final LispMapServerDetailsReplyDump reply = new LispMapServerDetailsReplyDump();
        LispMapServerDetails server1 = new LispMapServerDetails();
        //192.168.2.2
        server1.ipAddress = new byte[]{-64, -88, 2, 1};
        server1.isIpv6 = 0;

        LispMapServerDetails server2 = new LispMapServerDetails();
        //192.168.2.2
        server2.ipAddress = new byte[]{-64, -88, 2, 2};
        server2.isIpv6 = 0;

        LispMapServerDetails server3 = new LispMapServerDetails();
        //2001:0db8:0a0b:12f0:0000:0000:0000:0001
        server3.ipAddress = new byte[]{32, 1, 13, -72, 10, 11, 18, -16, 0, 0, 0, 0, 0, 0, 0, 1};
        server3.isIpv6 = 1;

        reply.lispMapServerDetails = Arrays.asList(server1, server2, server3);
        when(api.lispMapServerDump(any(LispMapServerDump.class))).thenReturn(future(reply));
        mockLispEnabled();
    }

    @Test
    public void testGetAllIds() throws Exception {
        final List<MapServerKey> allIds = getCustomizer().getAllIds(STATE_IID, ctx);
        assertThat(allIds, hasSize(3));
        assertThat(allIds, containsInAnyOrder(
                new MapServerKey(new IpAddress(new Ipv4AddressNoZone("192.168.2.1"))),
                new MapServerKey(new IpAddress(new Ipv4AddressNoZone("192.168.2.2"))),
                new MapServerKey(new IpAddress(new Ipv6AddressNoZone("2001:db8:a0b:12f0::1")))));
    }

    @Test
    public void testReadCurrentAttributes() throws Exception {
        final MapServerBuilder builder = new MapServerBuilder();
        getCustomizer().readCurrentAttributes(STATE_IID, builder, ctx);
        assertEquals("192.168.2.1", builder.getIpAddress().getIpv4Address().getValue());
    }

    @Test
    public void testInit() {
        final MapServer data = new MapServerBuilder().setIpAddress(
                new IpAddress(new Ipv4Address("192.168.2.1"))).build();
        invokeInitTest(STATE_IID, data, CONFIG_IID, data);
    }

    @Override
    protected ReaderCustomizer<MapServer, MapServerBuilder> initCustomizer() {
        return new MapServerCustomizer(api, lispStateCheckService);
    }
}