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

package io.fd.hc2vpp.dhcp.read;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.InitializingListReaderCustomizerTest;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.DhcpProxyDetails;
import io.fd.vpp.jvpp.core.dto.DhcpProxyDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.DhcpProxyDump;
import io.fd.vpp.jvpp.core.types.DhcpServer;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev180629.Dhcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev180629.dhcp.attributes.Relays;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev180629.dhcp.attributes.RelaysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev180629.dhcp.attributes.relays.Relay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev180629.dhcp.attributes.relays.RelayBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev180629.dhcp.attributes.relays.RelayKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev180629.relay.attributes.Server;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class DhcpRelayCustomizerTest extends InitializingListReaderCustomizerTest<Relay, RelayKey, RelayBuilder> {
    private static InstanceIdentifier<Relays> RELAYS = InstanceIdentifier.create(Dhcp.class).child(Relays.class);
    private KeyedInstanceIdentifier<Relay, RelayKey> IP4_IID =
        RELAYS.child(Relay.class, new RelayKey(Ipv4.class, new VniReference(123L)));
    private KeyedInstanceIdentifier<Relay, RelayKey> IP6_IID =
        RELAYS.child(Relay.class, new RelayKey(Ipv6.class, new VniReference(321L)));
    public DhcpRelayCustomizerTest() {
        super(Relay.class, RelaysBuilder.class);
    }

    @Override
    protected ReaderCustomizer<Relay, RelayBuilder> initCustomizer() {
        return new DhcpRelayCustomizer(api);
    }

    @Override
    protected void setUp() throws Exception {
        final DhcpProxyDetailsReplyDump ip4 = new DhcpProxyDetailsReplyDump();
        final DhcpProxyDetails ip4Proxy = new DhcpProxyDetails();
        ip4Proxy.rxVrfId = 123;
        ip4Proxy.dhcpSrcAddress = new byte[] {1, 2, 3, 4};
        final DhcpServer ip4server1 = new DhcpServer();
        ip4server1.serverVrfId = 11;
        ip4server1.dhcpServer = new byte[] {8, 8, 8, 8};
        final DhcpServer ip4server2 = new DhcpServer();
        ip4server2.serverVrfId = 12;
        ip4server2.dhcpServer = new byte[] {8, 8, 8, 4};
        ip4Proxy.servers = new DhcpServer[] {ip4server1, ip4server2};
        ip4.dhcpProxyDetails.add(ip4Proxy);
        when(api.dhcpProxyDump(new DhcpProxyDump())).thenReturn(future(ip4));

        final DhcpProxyDetailsReplyDump ip6 = new DhcpProxyDetailsReplyDump();
        final DhcpProxyDetails ip6Proxy = new DhcpProxyDetails();
        ip6Proxy.rxVrfId = 321;
        // 2001:0db8:0a0b:12f0:0000:0000:0000:0001
        ip6Proxy.dhcpSrcAddress =
            new byte[] {0x20, 0x01, 0x0d, (byte) 0xb8, 0x0a, 0x0b, 0x12, (byte) 0xf0, 0, 0, 0, 0, 0, 0, 0, 0x01};
        final DhcpServer ip6server = new DhcpServer();
        ip6server.serverVrfId = 22;
        // 2001:0db8:0a0b:12f0:0000:0000:0000:0002
        ip6server.dhcpServer =
            new byte[] {0x20, 0x01, 0x0d, (byte) 0xb8, 0x0a, 0x0b, 0x12, (byte) 0xf0, 0, 0, 0, 0, 0, 0, 0, 0x02};
        ip6Proxy.servers = new DhcpServer[] {ip6server};
        ip6Proxy.isIpv6 = 1;
        final DhcpProxyDump ip6Dump = new DhcpProxyDump();
        ip6Dump.isIp6 = 1;
        ip6.dhcpProxyDetails.add(ip6Proxy);
        when(api.dhcpProxyDump(ip6Dump)).thenReturn(future(ip6));
    }

    @Test
    public void testGetAllIds() throws ReadFailedException {
        final List<RelayKey> allIds = getCustomizer().getAllIds(RELAYS.child(Relay.class), ctx);
        assertEquals(2, allIds.size());
        assertThat(allIds, containsInAnyOrder(IP4_IID.getKey(), IP6_IID.getKey()));
    }

    @Test
    public void testReadIp4() throws ReadFailedException {
        final RelayBuilder builder = new RelayBuilder();
        getCustomizer().readCurrentAttributes(IP4_IID, builder, ctx);
        assertEquals(IP4_IID.getKey().getAddressFamily(), builder.getAddressFamily());
        assertEquals(IP4_IID.getKey().getRxVrfId(), builder.getRxVrfId());
        assertArrayEquals("1.2.3.4".toCharArray(), builder.getGatewayAddress().getValue());
        final List<Server> server = builder.getServer();
        assertEquals(2, server.size());
        assertEquals(11L, server.get(0).getVrfId().longValue());
        assertArrayEquals("8.8.8.8".toCharArray(), server.get(0).getAddress().getValue());
        assertEquals(12L, server.get(1).getVrfId().longValue());
        assertArrayEquals("8.8.8.4".toCharArray(), server.get(1).getAddress().getValue());
    }

    @Test
    public void testReadIp6() throws ReadFailedException {
        final RelayBuilder builder = new RelayBuilder();
        getCustomizer().readCurrentAttributes(IP6_IID, builder, ctx);
        assertEquals(IP6_IID.getKey().getAddressFamily(), builder.getAddressFamily());
        assertEquals(IP6_IID.getKey().getRxVrfId(), builder.getRxVrfId());
        assertEquals(22L, builder.getServer().get(0).getVrfId().longValue());
        assertArrayEquals("2001:db8:a0b:12f0::1".toCharArray(), builder.getGatewayAddress().getValue());
        assertArrayEquals("2001:db8:a0b:12f0::2".toCharArray(), builder.getServer().get(0).getAddress().getValue());
    }

    @Test
    public void testInit() {
        final Relay data = new RelayBuilder().build();
        invokeInitTest(IP4_IID, data, IP4_IID, data);
    }
}