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

package io.fd.hc2vpp.common.translate.util;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;

import static org.junit.Assert.*;

public class Ipv6TranslatorTest implements Ipv6Translator {

    private static final byte[] IPV6_BYTES = {32, 1, 13, -72, 10, 11, 18, -16, 0, 0, 0, 0, 0, 0, 0, 1};
    private static final String IPV6_FULL = "2001:0db8:0a0b:12f0:0000:0000:0000:0001";
    private static final String IPV6_COMPRESSED = "2001:db8:a0b:12f0::1";

    @Test
    public void testIpv6AddressNoZoneToArrayFull() throws Exception {
        assertArrayEquals(IPV6_BYTES, ipv6AddressNoZoneToArray(new Ipv6Address(IPV6_FULL)));
    }

    @Test
    public void testIpv6AddressNoZoneToArrayCompressed() throws Exception {
        assertArrayEquals(IPV6_BYTES, ipv6AddressNoZoneToArray(new Ipv6Address(IPV6_COMPRESSED)));
    }

    @Test
    public void testIpv6AddressPrefixToArrayFull() throws Exception {
        assertArrayEquals(IPV6_BYTES, ipv6AddressPrefixToArray(new Ipv6Prefix(IPV6_FULL + "/64")));
    }

    @Test
    public void testIpv6AddressPrefixToArrayCompressed() throws Exception {
        assertArrayEquals(IPV6_BYTES, ipv6AddressPrefixToArray(new Ipv6Prefix(IPV6_COMPRESSED + "/64")));
    }

    @Test
    public void testExtractPrefixFull() throws Exception {
        assertEquals(64, extractPrefix(new Ipv6Prefix(IPV6_FULL + "/64")));
    }

    @Test
    public void testExtractPrefixCompressed() throws Exception {
        assertEquals(64, extractPrefix(new Ipv6Prefix(IPV6_COMPRESSED + "/64")));
    }

    @Test
    public void testArrayToIpv6Prefix() throws Exception {
        assertEquals(IPV6_COMPRESSED + "/64", arrayToIpv6Prefix(IPV6_BYTES, (byte) 64).getValue());
    }

    @Test
    public void testArrayToIpv6AddressNoZone() throws Exception {
        assertEquals(IPV6_COMPRESSED, arrayToIpv6AddressNoZone(IPV6_BYTES).getValue());
    }

    @Test
    public void testIsIpv6Compressed() throws Exception {
        assertTrue(isIpv6(new IpAddress(new Ipv6Address(IPV6_COMPRESSED))));
    }

    @Test
    public void testIsIpv6Full() throws Exception {
        assertTrue(isIpv6(new IpAddress(new Ipv6Address(IPV6_FULL))));
    }

    @Test
    public void testTruncateIp4Array() throws Exception {
        assertArrayEquals(new byte[]{-64, -84, 2, 1}, truncateIp4Array(new byte[]{-64, -84, 2, 1, 0, 0, 0, 0}));
    }

    @Test
    public void testIpv6NoZone() {
        final Ipv6AddressNoZone ipv6Addr = new Ipv6AddressNoZone("3ffe:1900:4545:3:200:f8ff:fe21:67cf");
        byte[] bytes = ipv6AddressNoZoneToArray(ipv6Addr);
        assertEquals((byte) 0x3f, bytes[0]);
        final Ipv6AddressNoZone ivp6AddressNoZone = arrayToIpv6AddressNoZone(bytes);
        assertEquals(ipv6Addr, ivp6AddressNoZone);
    }

    @Test
    public void testIpv6NoZoneEmptyGroup() {
        final Ipv6AddressNoZone ipv6Addr = new Ipv6AddressNoZone("10::10");
        byte[] bytes = ipv6AddressNoZoneToArray(ipv6Addr);
        assertEquals((byte) 0, bytes[0]);
        assertEquals((byte) 0x10, bytes[1]);
        final Ipv6AddressNoZone ivp6AddressNoZone = arrayToIpv6AddressNoZone(bytes);
        assertEquals(ipv6Addr, ivp6AddressNoZone);
    }

    @Test
    public void testIpv6AddressPrefixToArray() {
        byte[] ip = ipv6AddressPrefixToArray(new Ipv6Prefix("3ffe:1900:4545:3:200:f8ff:fe21:67cf/48"));

        assertEquals("3ffe:1900:4545:3:200:f8ff:fe21:67cf", arrayToIpv6AddressNoZone(ip).getValue());
    }

    @Test
    public void testIpv4AddressPrefixToArray() {
        byte[] ip = ipv6AddressPrefixToArray(new Ipv6Prefix("2001:0db8:0a0b:12f0:0000:0000:0000:0001/128"));

        assertEquals("2001:db8:a0b:12f0::1", arrayToIpv6AddressNoZone(ip).getValue());
    }

    @Test
    public void testExtractPrefix() {
        assertEquals(48, extractPrefix(new Ipv6Prefix("3ffe:1900:4545:3:200:f8ff:fe21:67cf/48")));
    }

    @Test
    public void toPrefix() {
        assertEquals("2001:db8:a0b:12f0:0:0:0:1/48",
                toIpv6Prefix(new byte[]{32, 1, 13, -72, 10, 11, 18, -16, 0, 0, 0, 0, 0, 0, 0, 1},
                        (byte) 48).getValue());
    }
}