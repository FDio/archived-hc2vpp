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

package io.fd.hc2vpp.lisp.translate.util;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv6Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.MacAfi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.adjacency.LocalEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.adjacency.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.Eid;

import java.util.Arrays;

import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.*;
import static org.junit.Assert.*;

public class EidTranslatorTest implements EidTranslator {

    private static final String MAC_STRING = "bf:bf:bf:bf:bf:bf";
    private static final Mac MAC_ADDRES = new MacBuilder().setMac(
            new MacAddress(MAC_STRING))
            .build();
    private static final byte[] MAC_ADDRESS_BYTES = {-65, -65, -65, -65, -65, -65};
    private static final byte[] IPV6_ADDRESS_BYTES = {32, 1, 13, -72, 10, 11, 18, -16, 0, 0, 0, 0, 0, 0, 0, 1};
    private static final String IPV6_STRING = "2001:db8:a0b:12f0::1";
    private static final Ipv6 IPV6_ADDRESS = new Ipv6Builder().setIpv6(
            new Ipv6Address(IPV6_STRING))
            .build();
    private static final String IPV4_STRING = "192.168.2.1";
    private static final Ipv4 IPV4_ADDRESS = new Ipv4Builder().setIpv4(
            new Ipv4Address(IPV4_STRING))
            .build();
    private static final byte[] IPV_ADDRESS_BYTES = {-64, -88, 2, 1};

    @Test
    public void testGetEidType() {
        assertEquals(IPV4, getEidType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder()
                        .setAddress(IPV4_ADDRESS).build()));

        assertEquals(IPV6, getEidType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder()
                        .setAddress(IPV6_ADDRESS).build()));

        assertEquals(MAC, getEidType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder()
                        .setAddress(MAC_ADDRES).build()));
    }

    @Test
    public void testGetPrefixLength() {
        assertEquals(32, getPrefixLength(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.EidBuilder()
                .setAddress(IPV4_ADDRESS).build()));
        assertEquals(-128, getPrefixLength(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.EidBuilder()
                .setAddress(IPV6_ADDRESS).build()));
        assertEquals(0, getPrefixLength(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.EidBuilder()
                .setAddress(MAC_ADDRES).build()));
    }

    @Test
    public void testGetArrayAsEidRemoteIpv4() {
        final Eid eid = getArrayAsEidRemote(IPV4, IPV_ADDRESS_BYTES, 10);
        assertEquals(IPV4_STRING, ((Ipv4) eid.getAddress()).getIpv4().getValue());
        assertEquals(10, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv4Afi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsEidRemoteIpv6() {
        final Eid eid = getArrayAsEidRemote(IPV6, IPV6_ADDRESS_BYTES, 12);
        assertEquals(IPV6_STRING, ((Ipv6) eid.getAddress()).getIpv6().getValue());
        assertEquals(12, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv6Afi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsEidRemoteMac() {
        final Eid eid = getArrayAsEidRemote(MAC, MAC_ADDRESS_BYTES, 13);
        assertEquals(MAC_STRING, ((Mac) eid.getAddress()).getMac().getValue());
        assertEquals(13, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(MacAfi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsLocalEidIpv4() {
        final LocalEid eid = getArrayAsLocalEid(IPV4, IPV_ADDRESS_BYTES, 10);
        assertEquals(IPV4_STRING, ((Ipv4) eid.getAddress()).getIpv4().getValue());
        assertEquals(10, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv4Afi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsLocalEidIpv6() {
        final LocalEid eid = getArrayAsLocalEid(IPV6, IPV6_ADDRESS_BYTES, 12);
        assertEquals(IPV6_STRING, ((Ipv6) eid.getAddress()).getIpv6().getValue());
        assertEquals(12, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv6Afi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsLocalEidMac() {
        final LocalEid eid = getArrayAsLocalEid(MAC, MAC_ADDRESS_BYTES, 13);
        assertEquals(MAC_STRING, ((Mac) eid.getAddress()).getMac().getValue());
        assertEquals(13, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(MacAfi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsRemoteEidIpv4() {
        final RemoteEid eid = getArrayAsRemoteEid(IPV4, IPV_ADDRESS_BYTES, 10);
        assertEquals(IPV4_STRING, ((Ipv4) eid.getAddress()).getIpv4().getValue());
        assertEquals(10, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv4Afi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsRemoteEidIpv6() {
        final RemoteEid eid = getArrayAsRemoteEid(IPV6, IPV6_ADDRESS_BYTES, 12);
        assertEquals(IPV6_STRING, ((Ipv6) eid.getAddress()).getIpv6().getValue());
        assertEquals(12, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv6Afi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsRemoteEidMac() {
        final RemoteEid eid = getArrayAsRemoteEid(MAC, MAC_ADDRESS_BYTES, 13);
        assertEquals(MAC_STRING, ((Mac) eid.getAddress()).getMac().getValue());
        assertEquals(13, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(MacAfi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsEidStringIpv4() {
        assertEquals(IPV4_STRING, getArrayAsEidString(IPV4, IPV_ADDRESS_BYTES));
    }

    @Test
    public void testGetArrayAsEidStringIpv6() {
        assertEquals(IPV6_STRING, getArrayAsEidString(IPV6, IPV6_ADDRESS_BYTES));
    }

    @Test
    public void testGetArrayAsEidStringMac() {
        assertEquals(MAC_STRING, getArrayAsEidString(MAC, MAC_ADDRESS_BYTES));
    }

    @Test
    public void testResolveByteArrayIpv4() {
        assertTrue(Arrays.equals(IPV_ADDRESS_BYTES, resolveByteArray(IPV4, IPV4_ADDRESS)));
    }

    @Test
    public void testResolveByteArrayIpv6() {
        assertTrue(Arrays.equals(IPV6_ADDRESS_BYTES, resolveByteArray(IPV6, IPV6_ADDRESS)));
    }

    @Test
    public void testResolveByteArrayMac() {
        assertTrue(Arrays.equals(MAC_ADDRESS_BYTES, resolveByteArray(MAC, MAC_ADDRES)));
    }

    @Test
    public void testCompareAddressesPositive() {
        assertTrue(compareAddresses(MAC_ADDRES, MAC_ADDRES));
        assertTrue(compareAddresses(IPV4_ADDRESS, IPV4_ADDRESS));
        assertTrue(compareAddresses(IPV6_ADDRESS, IPV6_ADDRESS));
    }

    @Test
    public void testCompareAddressesNegative() {
        assertFalse(compareAddresses(MAC_ADDRES, IPV6_ADDRESS));
        assertFalse(compareAddresses(IPV4_ADDRESS, IPV6_ADDRESS));
        assertFalse(compareAddresses(IPV4_ADDRESS, MAC_ADDRES));
    }
}
