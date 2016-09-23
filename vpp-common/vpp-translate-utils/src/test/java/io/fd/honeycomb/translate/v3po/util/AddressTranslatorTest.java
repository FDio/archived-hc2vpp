package io.fd.honeycomb.translate.v3po.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;

public class AddressTranslatorTest implements AddressTranslator {

    @Test
    public void testRevertAddress() {
        assertEquals("1.2.168.192",
                reverseAddress(new IpAddress(new Ipv4Address("192.168.2.1"))).getIpv4Address().getValue());
        assertEquals("3473:7003:2e8a::a385:b80d:120",
                reverseAddress(new IpAddress(new Ipv6Address("2001:db8:85a3:0:0:8a2e:370:7334"))).getIpv6Address()
                        .getValue());
    }
}