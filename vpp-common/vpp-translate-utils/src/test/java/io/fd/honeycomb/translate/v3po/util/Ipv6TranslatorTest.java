package io.fd.honeycomb.translate.v3po.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;

public class Ipv6TranslatorTest implements Ipv6Translator {

    @Test
    public void testIpv6NoZone() {
        final Ipv6AddressNoZone ipv6Addr = new Ipv6AddressNoZone("3ffe:1900:4545:3:200:f8ff:fe21:67cf");
        byte[] bytes = ipv6AddressNoZoneToArray(ipv6Addr);
        assertEquals((byte) 63, bytes[0]);

        bytes = reverseBytes(bytes);
        final Ipv6AddressNoZone ivp6AddressNoZone = arrayToIpv6AddressNoZone(bytes);
        assertEquals(ipv6Addr, ivp6AddressNoZone);
    }

    @Test
    public void testIpv6AddressPrefixToArray() {
        byte[] ip = ipv6AddressPrefixToArray(new Ipv6Prefix("3ffe:1900:4545:3:200:f8ff:fe21:67cf/48"));

        assertEquals("cf67:21fe:fff8:2:300:4545:19:fe3f", arrayToIpv6AddressNoZone(ip).getValue());
    }

    @Test
    public void testIpv4AddressPrefixToArray() {
        byte[] ip = ipv6AddressPrefixToArray(new Ipv6Prefix("2001:0db8:0a0b:12f0:0000:0000:0000:0001/128"));

        assertEquals("100::f012:b0a:b80d:120", arrayToIpv6AddressNoZone(ip).getValue());
    }

    @Test
    public void testExtractPrefix() {
        assertEquals(48, extractPrefix(new Ipv6Prefix("3ffe:1900:4545:3:200:f8ff:fe21:67cf/48")));
    }
}