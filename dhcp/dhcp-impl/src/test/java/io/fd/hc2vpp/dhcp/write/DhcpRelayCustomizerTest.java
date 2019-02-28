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

package io.fd.hc2vpp.dhcp.write;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.dhcp.helpers.SchemaContextTestHelper;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.DhcpProxyConfig;
import io.fd.jvpp.core.dto.DhcpProxyConfigReply;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.dhcp.rev180629.Dhcp;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.dhcp.rev180629.dhcp.attributes.Relays;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.dhcp.rev180629.dhcp.attributes.relays.Relay;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.dhcp.rev180629.dhcp.attributes.relays.RelayKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.AddressFamilyIdentity;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv4;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class DhcpRelayCustomizerTest extends WriterCustomizerTest implements SchemaContextTestHelper {

    private static final String RELAYS_PATH = "/dhcp:dhcp/dhcp:relays";
    private static final InstanceIdentifier<Relays> RELAYS_IID =
        InstanceIdentifier.create(Dhcp.class).child(Relays.class);

    private DhcpRelayCustomizer customizer;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new DhcpRelayCustomizer(api);
        when(api.dhcpProxyConfig(any())).thenReturn(future(new DhcpProxyConfigReply()));
    }

    @Test
    public void testWrite(@InjectTestData(resourcePath = "/relay/ipv4DhcpRelay.json", id = RELAYS_PATH) Relays relays)
        throws WriteFailedException {
        final Relay data = relays.getRelay().get(0);
        final int rxVrfId = 0;
        customizer.writeCurrentAttributes(getId(rxVrfId, Ipv4.class), data, writeContext);
        final DhcpProxyConfig request = new DhcpProxyConfig();
        request.rxVrfId = rxVrfId;
        request.isIpv6 = 0;
        request.isAdd = 1;
        request.dhcpServer = new byte[] {1, 2, 3, 4};
        request.dhcpSrcAddress = new byte[] {5, 6, 7, 8};
        verify(api).dhcpProxyConfig(request);
        request.dhcpServer = new byte[] {1, 2, 3, 5};
        verify(api).dhcpProxyConfig(request);
    }

    @Test
    public void testUpdate(
        @InjectTestData(resourcePath = "/relay/ipv6DhcpRelayBefore.json", id = RELAYS_PATH) Relays relaysBefore,
        @InjectTestData(resourcePath = "/relay/ipv6DhcpRelayAfter.json", id = RELAYS_PATH) Relays relayAfter)
        throws WriteFailedException {
        final Relay before = relaysBefore.getRelay().get(0);
        final Relay after = relayAfter.getRelay().get(0);
        final int rxVrfId = 1;
        customizer.updateCurrentAttributes(getId(rxVrfId, Ipv6.class), before, after, writeContext);
        final DhcpProxyConfig request = new DhcpProxyConfig();
        request.rxVrfId = rxVrfId;
        request.serverVrfId = 2;
        request.isIpv6 = 1;
        request.isAdd = 0;
        request.dhcpServer = new byte[] {0x20, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x01};
        request.dhcpSrcAddress = new byte[] {0x20, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x02};
        verify(api).dhcpProxyConfig(request);
    }

    @Test
    public void testDelete(@InjectTestData(resourcePath = "/relay/ipv4DhcpRelay.json", id = RELAYS_PATH) Relays relays)
        throws WriteFailedException {
        final Relay data = relays.getRelay().get(0);
        final int rxVrfId = 0;
        customizer.deleteCurrentAttributes(getId(rxVrfId, Ipv4.class), data, writeContext);
        final DhcpProxyConfig request = new DhcpProxyConfig();
        request.rxVrfId = rxVrfId;
        request.isIpv6 = 0;
        request.isAdd = 0;
        request.dhcpServer = new byte[] {1, 2, 3, 4};
        request.dhcpSrcAddress = new byte[] {5, 6, 7, 8};
        verify(api).dhcpProxyConfig(request);
        request.dhcpServer = new byte[] {1, 2, 3, 5};
        verify(api).dhcpProxyConfig(request);
    }

    private InstanceIdentifier<Relay> getId(final long rxVrfId, final Class<? extends AddressFamilyIdentity> addressType) {
        return RELAYS_IID.child(Relay.class, new RelayKey(addressType, new VniReference(rxVrfId)));
    }
}
