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

package io.fd.honeycomb.translate.vpp.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;

public class Ipv4TranslatorTest implements Ipv4Translator {

    @Test
    public void testIpv4NoZone() throws Exception {
        final Ipv4AddressNoZone ipv4Addr = new Ipv4AddressNoZone("192.168.1.1");
        byte[] bytes = ipv4AddressNoZoneToArray(ipv4Addr);
        assertEquals((byte) 192, bytes[0]);
        // Simulating the magic of VPP
        bytes = reverseBytes(bytes);
        final Ipv4AddressNoZone ipv4AddressNoZone = arrayToIpv4AddressNoZone(bytes);
        assertEquals(ipv4Addr, ipv4AddressNoZone);
    }

    @Test
    public void testIpv4AddressPrefixToArray() {
        byte[] ip = ipv4AddressPrefixToArray(new Ipv4Prefix("192.168.2.1/24"));

        assertEquals("1.2.168.192", arrayToIpv4AddressNoZone(ip).getValue());
    }

    @Test
    public void testExtractPrefix() {
        assertEquals(24, extractPrefix(new Ipv4Prefix("192.168.2.1/24")));
    }
}