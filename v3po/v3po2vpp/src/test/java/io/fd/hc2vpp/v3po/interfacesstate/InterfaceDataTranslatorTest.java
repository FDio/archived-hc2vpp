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

package io.fd.hc2vpp.v3po.interfacesstate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.jvpp.core.dto.SwInterfaceDetails;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.AfPacket;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.TapV2;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VhostUser;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VxlanGpeTunnel;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VxlanTunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev180703.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceDataTranslatorTest implements InterfaceDataTranslator {

    @Test
    public void testVppPhysAddrToYang() throws Exception {
        assertEquals("01:02:03:04:05:06", vppPhysAddrToYang(new byte[] {1, 2, 3, 4, 5, 6}));
        // Extended (64-bit) MAC addresses are currently not supported (it might require yang model update),
        // so test if extended part is ignored
        assertEquals("0a:0b:0c:0d:0e:0f", vppPhysAddrToYang(new byte[] {0xa, 0xb, 0xc, 0xd, 0xe, 0xf, 0, 0}));
    }

    @Test(expected = NullPointerException.class)
    public void testVppPhysAddrToYangFailNullArgument() throws Exception {
        vppPhysAddrToYang(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVppPhysAddrToYangInvalidByteArrayLength() throws Exception {
        vppPhysAddrToYang(new byte[] {1, 2, 3, 4, 5});
    }

    @Test
    public void testGetInterfaceType() {
        assertEquals(TapV2.class, getInterfaceType("tap0"));
        assertEquals(VxlanTunnel.class, getInterfaceType("vxlan0"));
        assertEquals(VxlanGpeTunnel.class, getInterfaceType("vxlan_gpe0"));
        assertEquals(VhostUser.class, getInterfaceType("VirtualEthernet0/0/0"));
        assertEquals(AfPacket.class, getInterfaceType("host-veth0"));
        assertEquals(EthernetCsmacd.class, getInterfaceType("eth0.0"));
        assertEquals(EthernetCsmacd.class, getInterfaceType("local0"));
    }

    @Test
    public void testIsInterfaceOfType() {
        assertTrue(isInterfaceOfType(TapV2.class, interfaceDetails("tap0")));
        assertTrue(isInterfaceOfType(VxlanTunnel.class, interfaceDetails("vxlan0")));
        assertTrue(isInterfaceOfType(VxlanGpeTunnel.class, interfaceDetails("vxlan_gpe0")));
        assertTrue(isInterfaceOfType(VhostUser.class, interfaceDetails("VirtualEthernet0/0/0")));
        assertTrue(isInterfaceOfType(AfPacket.class, interfaceDetails("host-veth0")));
        assertTrue(isInterfaceOfType(EthernetCsmacd.class, interfaceDetails("eth0.0")));
        assertTrue(isInterfaceOfType(EthernetCsmacd.class, interfaceDetails("local0")));
    }

    @Test
    public void testIsInterfaceOfTypeMissingIfc() throws ReadFailedException {
        final InterfaceCacheDumpManager dumpManager = mock(InterfaceCacheDumpManager.class);
        final ReadContext ctx = mock(ReadContext.class);
        final String ifcName = "tapThatDoesNotExists";
        final InstanceIdentifier<Interface> id =
            InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(ifcName));

        when(dumpManager.getInterfaceDetail(id, ctx, ifcName)).thenReturn(null);

        assertFalse(isInterfaceOfType(dumpManager, id, ctx, TapV2.class));
    }

    private SwInterfaceDetails interfaceDetails(final String interfaceName) {
        final SwInterfaceDetails details = new SwInterfaceDetails();
        details.interfaceName = interfaceName.getBytes(StandardCharsets.UTF_8);
        return details;
    }


}
