package io.fd.honeycomb.lisp.translate.util;

import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV4;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV6;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.MAC;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.MacBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;

public class EidConverterTest {

    @Test
    public void testGetEidType() {
        assertEquals(IPV4, EidConverter
                .getEidType(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.EidBuilder()
                                .setAddress(
                                        new Ipv4Builder().setIpv4(
                                                new Ipv4Address("192.168.2.1"))
                                                .build())
                                .build()));

        assertEquals(IPV6, EidConverter
                .getEidType(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.EidBuilder()
                                .setAddress(
                                        new Ipv6Builder().setIpv6(
                                                new Ipv6Address("2001:0db8:0a0b:12f0:0000:0000:0000:0001"))
                                                .build())
                                .build()));

        assertEquals(MAC, EidConverter
                .getEidType(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.EidBuilder()
                                .setAddress(
                                        new MacBuilder().setMac(
                                                new MacAddress("aa:bb:cc:dd:ee:ff"))
                                                .build())
                                .build()));

        //TODO  testing of other types when they are implemented
    }
}
