/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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
package io.fd.hc2vpp.v3po.interfacesstate.ip;

import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.hc2vpp.common.test.util.NamingContextHelper;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.vpp.jvpp.core.dto.*;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public interface InterfaceChildNodeTest extends NamingContextHelper, FutureProducer {

    String INTERFACE_CONTEXT_NAME = "iface-context";
    NamingContext INTERFACE_CONTEXT = new NamingContext("prefix", INTERFACE_CONTEXT_NAME);

    String IFACE_NAME = "eth0";
    int IFACE_ID = 1;

    String SUB_IFACE_NAME = "eth0.4";
    int SUB_IFACE_ID = 4;

    String IFACE_2_NAME = "eth1";
    int IFACE_2_ID = 2;

    String SUB_IFACE_2_NAME = "eth1.7";
    int SUB_IFACE_2_ID = 7;

    String IPV6_ONE_COMPRESSED = "2001:db8:a0b:12f0::1";
    byte[] IPV6_ONE_BYTES = {32, 1, 13, -72, 10, 11, 18, -16, 0, 0, 0, 0, 0, 0, 0, 1};
    byte IPV6_ONE_PREFIX = 64;
    Ipv6AddressNoZone IPV6_ONE_ADDRESS_COMPRESSED = new Ipv6AddressNoZone(IPV6_ONE_COMPRESSED);

    String IPV6_TWO_COMPRESSED = "2001:db8:a0b:12f0::2";
    byte[] IPV6_TWO_BYTES = {32, 1, 13, -72, 10, 11, 18, -16, 0, 0, 0, 0, 0, 0, 0, 2};
    byte IPV6_TWO_PREFIX = 48;
    Ipv6AddressNoZone IPV6_TWO_ADDRESS_COMPRESSED = new Ipv6AddressNoZone(IPV6_TWO_COMPRESSED);

    String IPV4_ONE = "192.168.2.1";
    byte[] IPV4_ONE_BYTES = {-64, -88, 2, 1};
    byte IPV4_ONE_PREFIX = 24;
    Ipv4AddressNoZone IPV4_ONE_ADDRESS = new Ipv4AddressNoZone(IPV4_ONE);

    String IPV4_TWO = "192.168.2.2";
    byte[] IPV4_TWO_BYTES = {-64, -88, 2, 2};
    byte IPV4_TWO_PREFIX = 24;
    Ipv4AddressNoZone IPV4_TWO_ADDRESS = new Ipv4AddressNoZone(IPV4_TWO);

    String MAC_ONE = "00:00:00:00:00:00";
    byte[] MAC_ONE_BYTES = {0, 0, 0, 0, 0, 0};
    PhysAddress MAC_ONE_ADDRESS = new PhysAddress(MAC_ONE);

    String MAC_TWO = "00:00:00:00:00:01";
    byte[] MAC_TWO_BYTES = {0, 0, 0, 0, 0, 1};
    PhysAddress MAC_TWO_ADDRESS = new PhysAddress(MAC_TWO);

    String MAC_THREE = "00:00:00:00:00:02";
    byte[] MAC_THREE_BYTES = {0, 0, 0, 0, 0, 2};
    PhysAddress MAC_THREE_ADDRESS = new PhysAddress(MAC_THREE);

    String MAC_FOUR = "00:00:00:00:00:03";
    byte[] MAC_FOUR_BYTES = {0, 0, 0, 0, 0, 3};
    PhysAddress MAC_FOUR_ADDRESS = new PhysAddress(MAC_FOUR);

    default void mockNeighborDump(@Nonnull final FutureJVppCore api,
                                  @Nonnull final IpNeighborDump request,
                                  @Nonnull final IpNeighborDetailsReplyDump reply) {
        when(api.ipNeighborDump(request)).thenReturn(future(reply));
    }

    default void mockAddressDump(@Nonnull final FutureJVppCore api,
                                 @Nonnull final IpAddressDump request,
                                 @Nonnull final IpAddressDetailsReplyDump reply) {
        when(api.ipAddressDump(request)).thenReturn(future(reply));
    }

    default IpAddressDump dumpV6AddressesIfaceTwo() {
        IpAddressDump request = new IpAddressDump();
        request.isIpv6 = 1;
        request.swIfIndex = IFACE_2_ID;
        return request;
    }

    default IpAddressDump dumpV6AddressesSubIfaceOne() {
        IpAddressDump request = new IpAddressDump();
        request.swIfIndex = SUB_IFACE_ID;
        request.isIpv6 = 1;
        return request;
    }


    default IpNeighborDump dumpV4NeighborIfaceOne() {
        IpNeighborDump request = new IpNeighborDump();
        request.swIfIndex = IFACE_ID;
        request.isIpv6 = 0;
        return request;
    }

    default IpNeighborDump dumpV6NeighborsIfaceOne() {
        IpNeighborDump request = new IpNeighborDump();
        request.swIfIndex = IFACE_ID;
        request.isIpv6 = 1;
        return request;
    }

    default IpNeighborDump dumpV6NeighborsSubIfaceTwo() {
        IpNeighborDump request = new IpNeighborDump();
        request.swIfIndex = SUB_IFACE_2_ID;
        request.isIpv6 = 1;
        return request;
    }

    default IpNeighborDump dumpV4NeighborsSubIfaceOne() {
        IpNeighborDump request = new IpNeighborDump();
        request.swIfIndex = SUB_IFACE_ID;
        request.isIpv6 = 0;
        return request;
    }

    default void verifyList(@Nonnull final List<?> expected, @Nonnull final List<?> current) {
        assertThat(current, hasSize(expected.size()));
        assertTrue(expected.containsAll(current));
    }

    default IpNeighborDetailsReplyDump v4Neighbors() {
        IpNeighborDetailsReplyDump fullDump = new IpNeighborDetailsReplyDump();
        fullDump.ipNeighborDetails = Arrays.asList(
                neighborDump(IPV4_ONE_BYTES, 0, MAC_ONE_BYTES),
                neighborDump(IPV4_TWO_BYTES, 0, MAC_TWO_BYTES));
        return fullDump;
    }

    default IpNeighborDetailsReplyDump v6Neighbors() {
        IpNeighborDetailsReplyDump fullDump = new IpNeighborDetailsReplyDump();
        fullDump.ipNeighborDetails = Arrays.asList(
                neighborDump(IPV6_ONE_BYTES, 1, MAC_THREE_BYTES),
                neighborDump(IPV6_TWO_BYTES, 1, MAC_FOUR_BYTES));
        return fullDump;
    }

    default IpAddressDetailsReplyDump v4Addresses() {
        IpAddressDetailsReplyDump fullDump = new IpAddressDetailsReplyDump();
        fullDump.ipAddressDetails = Arrays.asList(
                addressDump(IPV4_ONE_BYTES, IPV4_ONE_PREFIX),
                addressDump(IPV4_TWO_BYTES, IPV4_TWO_PREFIX));
        return fullDump;
    }

    default IpAddressDetailsReplyDump v6Addresses() {
        IpAddressDetailsReplyDump fullDump = new IpAddressDetailsReplyDump();
        fullDump.ipAddressDetails = Arrays.asList(
                addressDump(IPV6_ONE_BYTES, IPV6_ONE_PREFIX),
                addressDump(IPV6_TWO_BYTES, IPV6_TWO_PREFIX));
        return fullDump;
    }

    static IpNeighborDetails neighborDump(byte[] address, int isIpv6, byte[] mac) {
        IpNeighborDetails detail = new IpNeighborDetails();
        detail.ipAddress = address;
        detail.isIpv6 = (byte) isIpv6;
        detail.macAddress = mac;
        return detail;
    }

    static IpAddressDetails addressDump(byte[] address, byte prefix) {
        IpAddressDetails details = new IpAddressDetails();
        details.ip = address;
        details.prefixLength = prefix;
        return details;
    }
}
