/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;

public class AddressTranslatorTest {

    private final static IpAddress IPV6_ADDR = new IpAddress(new Ipv6Address("a::"));
    private final static IpAddressNoZone IPV6_ADDR_NO_ZONE = new IpAddressNoZone(new Ipv6AddressNoZone("a::"));
    private final static IpPrefix IPV6_PREFIX = new IpPrefix(new Ipv6Prefix("a::/48"));
    private final static byte IPV6_BYTE_PREFIX = 48;
    private final static IpAddress IPV4_ADDR = new IpAddress(new Ipv4Address("10.0.0.1"));
    private final static IpAddressNoZone IPV4_ADDR_NO_ZONE = new IpAddressNoZone(new Ipv4AddressNoZone("10.0.0.1"));
    private final static IpPrefix IPV4_PREFIX = new IpPrefix(new Ipv4Prefix("10.0.0.1/24"));
    private final static byte IPV4_BYTE_PREFIX = 24;
    private static final byte[] IPV6_BYTE_ARRAY =
            {(byte) 0, (byte) 10, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
                    (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
    private static final byte[] IPV4_BYTE_ARRAY = {(byte) 10, (byte) 0, (byte) 0, (byte) 1};
    private static final AddressTranslator ADDR_TRANSLATOR = new AddressTranslator() {};

    @Test
    public void ipAddressToArrayTest() {
        Assert.assertTrue(Arrays.equals(IPV6_BYTE_ARRAY, ADDR_TRANSLATOR.ipAddressToArray(IPV6_ADDR)));
        Assert.assertTrue(Arrays.equals(IPV6_BYTE_ARRAY, ADDR_TRANSLATOR.ipAddressToArray(true, IPV6_ADDR)));
        Assert.assertTrue(Arrays.equals(IPV4_BYTE_ARRAY, ADDR_TRANSLATOR.ipAddressToArray(IPV4_ADDR)));
        Assert.assertTrue(Arrays.equals(IPV4_BYTE_ARRAY, ADDR_TRANSLATOR.ipAddressToArray(false, IPV4_ADDR)));
    }

    @Test
    public void ipAddressNoZoneToArrayTest() {
        Assert.assertTrue(Arrays.equals(IPV6_BYTE_ARRAY, ADDR_TRANSLATOR.ipAddressToArray(IPV6_ADDR_NO_ZONE)));
        Assert.assertTrue(Arrays.equals(IPV4_BYTE_ARRAY, ADDR_TRANSLATOR.ipAddressToArray(IPV4_ADDR_NO_ZONE)));
    }

    @Test
    public void arrayToIpAddressTest() {
        Assert.assertTrue(
                ADDR_TRANSLATOR.addressesEqual(IPV6_ADDR, ADDR_TRANSLATOR.arrayToIpAddress(true, IPV6_BYTE_ARRAY)));
        Assert.assertTrue(
                ADDR_TRANSLATOR.addressesEqual(IPV4_ADDR, ADDR_TRANSLATOR.arrayToIpAddress(false, IPV4_BYTE_ARRAY)));
    }

    @Test
    public void extractPrefixTest() {
        Assert.assertEquals(IPV6_BYTE_PREFIX, ADDR_TRANSLATOR.extractPrefix(IPV6_PREFIX));
        Assert.assertEquals(IPV4_BYTE_PREFIX, ADDR_TRANSLATOR.extractPrefix(IPV4_PREFIX));
    }

    @Test
    public void ipPrefixToArrayTest() {
        Assert.assertTrue(Arrays.equals(IPV6_BYTE_ARRAY, ADDR_TRANSLATOR.ipPrefixToArray(IPV6_PREFIX)));
        Assert.assertTrue(Arrays.equals(IPV4_BYTE_ARRAY, ADDR_TRANSLATOR.ipPrefixToArray(IPV4_PREFIX)));
    }
}
