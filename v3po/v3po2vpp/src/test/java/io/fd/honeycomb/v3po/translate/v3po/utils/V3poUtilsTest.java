package io.fd.honeycomb.v3po.translate.v3po.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;

public class V3poUtilsTest {

    @Test
    public void testRemoveIpv4AddressNoZoneFromIpv4WithZone() throws Exception {
        String ipWithZone = "1.2.3.4%20";
        String ipNoZone = "1.2.3.4";
        final Ipv4Address expectedIp = new Ipv4Address(ipNoZone);
        final Ipv4AddressNoZone actualIp = V3poUtils.removeIpv4AddressNoZone(new Ipv4Address(ipWithZone));
        assertEquals(expectedIp.getValue(), actualIp.getValue());
    }

    @Test
    public void testRemoveIpv4AddressNoZoneFromIpv4WithoutZone() throws Exception {
        String ipNoZone = "1.2.3.4";
        final Ipv4Address expectedIp = new Ipv4Address(ipNoZone);
        final Ipv4AddressNoZone actualIp = V3poUtils.removeIpv4AddressNoZone(expectedIp);
        assertEquals(expectedIp.getValue(), actualIp.getValue());
    }

    @Test
    public void testRemoveIpv4AddressNoZoneNop() throws Exception {
        String ipNoZone = "1.2.3.4";
        final Ipv4Address expectedIp = new Ipv4AddressNoZone(ipNoZone);
        final Ipv4AddressNoZone actualIp = V3poUtils.removeIpv4AddressNoZone(expectedIp);
        assertEquals(expectedIp, actualIp);
    }
}