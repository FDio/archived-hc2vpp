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