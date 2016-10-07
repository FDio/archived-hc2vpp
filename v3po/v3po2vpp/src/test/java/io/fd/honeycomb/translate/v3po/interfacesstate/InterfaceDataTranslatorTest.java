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

package io.fd.honeycomb.translate.v3po.interfacesstate;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VxlanGpeTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VxlanTunnel;

public class InterfaceDataTranslatorTest implements InterfaceDataTranslator {

    @Test
    public void testVppPhysAddrToYang() throws Exception {
        assertEquals("01:02:03:04:05:06", vppPhysAddrToYang(new byte[]{1, 2, 3, 4, 5, 6}));
        assertEquals("0a:0b:0c:0d:0e:0f", vppPhysAddrToYang(new byte[]{0xa, 0xb, 0xc, 0xd, 0xe, 0xf, 0, 0}));
    }

    @Test(expected = NullPointerException.class)
    public void testVppPhysAddrToYangFailNullArgument() throws Exception {
        vppPhysAddrToYang(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVppPhysAddrToYangInvalidByteArrayLength() throws Exception {
        vppPhysAddrToYang(new byte[]{1, 2, 3, 4, 5});
    }

    @Test
    public void testGetInterfaceType() {
        assertEquals(Tap.class, getInterfaceType("tap0"));
        assertEquals(VxlanTunnel.class, getInterfaceType("vxlan0"));
        assertEquals(VxlanGpeTunnel.class, getInterfaceType("vxlan_gpe0"));
        assertEquals(VhostUser.class, getInterfaceType("VirtualEthernet0/0/0"));
        assertEquals(EthernetCsmacd.class, getInterfaceType("eth0.0"));
        assertEquals(EthernetCsmacd.class, getInterfaceType("local0"));
    }
}