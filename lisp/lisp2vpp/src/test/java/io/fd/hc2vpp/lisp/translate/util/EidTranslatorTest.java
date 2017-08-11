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

import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV4;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV4_PREFIX;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV6;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV6_PREFIX;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.MAC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4PrefixAfi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv6Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv6PrefixAfi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.MacAfi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv6PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Mac;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.MacBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.adjacencies.grouping.adjacencies.adjacency.LocalEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.adjacencies.grouping.adjacencies.adjacency.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.remote.mappings.remote.mapping.Eid;

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
    private static final String NORMALIZED_V6_PREFIX = "2001:db8:a0b:12f0::/64";
    private static final String NORMALIZED_V4_PREFIX = "192.168.2.0/24";

    @Test
    public void testGetEidType() {
        assertEquals(IPV4, getEidType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder()
                        .setAddress(IPV4_ADDRESS).build()));

        assertEquals(IPV6, getEidType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder()
                        .setAddress(IPV6_ADDRESS).build()));

        assertEquals(MAC, getEidType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder()
                        .setAddress(MAC_ADDRES).build()));
    }

    @Test
    public void testGetPrefixLength() {
        assertEquals(32, getPrefixLength(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.local.mappings.local.mapping.EidBuilder()
                        .setAddress(IPV4_ADDRESS).build()));
        assertEquals(-128, getPrefixLength(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.local.mappings.local.mapping.EidBuilder()
                        .setAddress(IPV6_ADDRESS).build()));
        assertEquals(0, getPrefixLength(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.local.mappings.local.mapping.EidBuilder()
                        .setAddress(MAC_ADDRES).build()));
    }

    @Test
    public void testGetArrayAsEidRemoteIpv4() {
        final Eid eid = getArrayAsEidRemote(IPV4, IPV_ADDRESS_BYTES, DEFAULT_V4_PREFIX, 10);
        assertEquals(IPV4_STRING, ((Ipv4) eid.getAddress()).getIpv4().getValue());
        assertEquals(10, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv4Afi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsEidRemoteIpv4Prefix() {
        final Eid eid = getArrayAsEidRemote(IPV4_PREFIX, IPV_ADDRESS_BYTES, (byte) 24, 10);
        assertEquals(NORMALIZED_V4_PREFIX, ((Ipv4Prefix) eid.getAddress()).getIpv4Prefix().getValue());
        assertEquals(10, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv4PrefixAfi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsEidRemoteIpv6() {
        final Eid eid = getArrayAsEidRemote(IPV6, IPV6_ADDRESS_BYTES, DEFAULT_V6_PREFIX, 12);
        assertEquals(IPV6_STRING, ((Ipv6) eid.getAddress()).getIpv6().getValue());
        assertEquals(12, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv6Afi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsEidRemoteIpv6Prefix() {
        final Eid eid = getArrayAsEidRemote(IPV6_PREFIX, IPV6_ADDRESS_BYTES, (byte) 64, 12);
        assertEquals(NORMALIZED_V6_PREFIX, ((Ipv6Prefix) eid.getAddress()).getIpv6Prefix().getValue());
        assertEquals(12, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv6PrefixAfi.class, eid.getAddressType());
    }


    @Test
    public void testGetArrayAsEidRemoteMac() {
        final Eid eid = getArrayAsEidRemote(MAC, MAC_ADDRESS_BYTES, (byte) 0, 13);
        assertEquals(MAC_STRING, ((Mac) eid.getAddress()).getMac().getValue());
        assertEquals(13, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(MacAfi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsLocalEidIpv4() {
        final LocalEid eid = getArrayAsLocalEid(IPV4, IPV_ADDRESS_BYTES, DEFAULT_V4_PREFIX, 10);
        assertEquals(IPV4_STRING, ((Ipv4) eid.getAddress()).getIpv4().getValue());
        assertEquals(10, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv4Afi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsLocalEidIpv4Prefix() {
        final LocalEid eid = getArrayAsLocalEid(IPV4_PREFIX, IPV_ADDRESS_BYTES, (byte) 24, 10);
        assertEquals(NORMALIZED_V4_PREFIX, ((Ipv4Prefix) eid.getAddress()).getIpv4Prefix().getValue());
        assertEquals(10, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv4PrefixAfi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsLocalEidIpv6() {
        final LocalEid eid = getArrayAsLocalEid(IPV6, IPV6_ADDRESS_BYTES, DEFAULT_V6_PREFIX, 12);
        assertEquals(IPV6_STRING, ((Ipv6) eid.getAddress()).getIpv6().getValue());
        assertEquals(12, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv6Afi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsLocalEidIpv6Prefix() {
        final LocalEid eid = getArrayAsLocalEid(IPV6_PREFIX, IPV6_ADDRESS_BYTES, (byte) 64, 12);
        assertEquals(NORMALIZED_V6_PREFIX, ((Ipv6Prefix) eid.getAddress()).getIpv6Prefix().getValue());
        assertEquals(12, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv6PrefixAfi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsLocalEidMac() {
        final LocalEid eid = getArrayAsLocalEid(MAC, MAC_ADDRESS_BYTES, (byte) 0, 13);
        assertEquals(MAC_STRING, ((Mac) eid.getAddress()).getMac().getValue());
        assertEquals(13, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(MacAfi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsRemoteEidIpv4() {
        final RemoteEid eid = getArrayAsRemoteEid(IPV4, IPV_ADDRESS_BYTES, DEFAULT_V4_PREFIX, 10);
        assertEquals(IPV4_STRING, ((Ipv4) eid.getAddress()).getIpv4().getValue());
        assertEquals(10, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv4Afi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsRemoteEidIpv4Prefix() {
        final RemoteEid eid = getArrayAsRemoteEid(IPV4_PREFIX, IPV_ADDRESS_BYTES, (byte) 24, 10);
        assertEquals(NORMALIZED_V4_PREFIX, ((Ipv4Prefix) eid.getAddress()).getIpv4Prefix().getValue());
        assertEquals(10, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv4PrefixAfi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsRemoteEidIpv6() {
        final RemoteEid eid = getArrayAsRemoteEid(IPV6, IPV6_ADDRESS_BYTES, DEFAULT_V6_PREFIX, 12);
        assertEquals(IPV6_STRING, ((Ipv6) eid.getAddress()).getIpv6().getValue());
        assertEquals(12, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv6Afi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsRemoteEidIpv6Prefix() {
        final RemoteEid eid = getArrayAsRemoteEid(IPV6_PREFIX, IPV6_ADDRESS_BYTES, (byte) 64, 12);
        assertEquals(NORMALIZED_V6_PREFIX, ((Ipv6Prefix) eid.getAddress()).getIpv6Prefix().getValue());
        assertEquals(12, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(Ipv6PrefixAfi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsRemoteEidMac() {
        final RemoteEid eid = getArrayAsRemoteEid(MAC, MAC_ADDRESS_BYTES, (byte) 0, 13);
        assertEquals(MAC_STRING, ((Mac) eid.getAddress()).getMac().getValue());
        assertEquals(13, eid.getVirtualNetworkId().getValue().intValue());
        assertEquals(MacAfi.class, eid.getAddressType());
    }

    @Test
    public void testGetArrayAsEidStringIpv4() {
        assertEquals(IPV4_STRING, getArrayAsEidString(IPV4, IPV_ADDRESS_BYTES, DEFAULT_V4_PREFIX));
    }

    @Test
    public void testGetArrayAsEidStringIpv4Prefix() {
        assertEquals(NORMALIZED_V4_PREFIX, getArrayAsEidString(IPV4, IPV_ADDRESS_BYTES, (byte) 24));
    }

    @Test
    public void testGetArrayAsEidStringIpv6() {
        assertEquals(IPV6_STRING, getArrayAsEidString(IPV6, IPV6_ADDRESS_BYTES, DEFAULT_V6_PREFIX));
    }

    @Test
    public void testGetArrayAsEidStringIpv6Prefix() {
        assertEquals(NORMALIZED_V6_PREFIX, getArrayAsEidString(IPV6, IPV6_ADDRESS_BYTES, (byte) 64));
    }

    @Test
    public void testGetArrayAsEidStringMac() {
        assertEquals(MAC_STRING, getArrayAsEidString(MAC, MAC_ADDRESS_BYTES, (byte) 0));
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

    @Test
    public void testCompareV4AddressFromSameSubnetPositive() {
        final Ipv4Prefix firstV4 = new Ipv4PrefixBuilder().setIpv4Prefix(v4Prefix("192.168.2.1/24")).build();
        final Ipv4Prefix secondV4 = new Ipv4PrefixBuilder().setIpv4Prefix(v4Prefix("192.168.2.2/24")).build();

        assertTrue(compareAddresses(firstV4, secondV4));
    }

    @Test
    public void testCompareV4AddressFromSameSubnetNegative() {
        final Ipv4Prefix firstV4 = new Ipv4PrefixBuilder().setIpv4Prefix(v4Prefix("192.168.2.1/24")).build();
        final Ipv4Prefix secondV4 = new Ipv4PrefixBuilder().setIpv4Prefix(v4Prefix("192.168.2.1/16")).build();

        assertFalse(compareAddresses(firstV4, secondV4));
    }

    @Test
    public void testCompareV6AddressesFromSameSubnetPositive() {
        final Ipv6Prefix firstV6 = new Ipv6PrefixBuilder().setIpv6Prefix(v6Prefix("2001:db8:a0b:12f0::1/64")).build();
        final Ipv6Prefix secondV6 = new Ipv6PrefixBuilder().setIpv6Prefix(v6Prefix("2001:db8:a0b:12f0::4/64")).build();

        assertTrue(compareAddresses(firstV6, secondV6));
    }

    @Test
    public void testCompareV6AddressesFromSameSubnetNegative() {
        final Ipv6Prefix firstV6 = new Ipv6PrefixBuilder().setIpv6Prefix(v6Prefix("2001:0db8:85a3:0000:0000:8a2e:0370:7334/64")).build();
        final Ipv6Prefix secondV6 = new Ipv6PrefixBuilder().setIpv6Prefix(v6Prefix("2001:0db8:85a3:0000:0000:8a2e:0370:7334/48")).build();

        assertFalse(compareAddresses(firstV6, secondV6));
    }

    private static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix v6Prefix(
            String v6Prefix) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix(
                v6Prefix);
    }

    private static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix v4Prefix(
            String v4Prefix) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix(
                v4Prefix);
    }
}
