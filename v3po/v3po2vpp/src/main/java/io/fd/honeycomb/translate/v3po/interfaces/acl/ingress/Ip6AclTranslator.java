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

package io.fd.honeycomb.translate.v3po.interfaces.acl.ingress;

import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.AclIpHeaderFields;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.AclIpv6HeaderFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;
import org.slf4j.Logger;

public interface Ip6AclTranslator {

    int ETHER_TYPE_OFFSET = 12; // first 14 bytes represent L2 header (2x6)
    int IP_VERSION_OFFSET = ETHER_TYPE_OFFSET + 2;
    int DSCP_MASK1 = 0x0f;
    int DSCP_MASK2 = 0xc0;
    int IP_PROTOCOL_OFFSET = IP_VERSION_OFFSET + 6;
    int IP_PROTOCOL_MASK = 0xff;
    int IP6_LEN = 16;
    int SRC_IP_OFFSET = IP_VERSION_OFFSET + 8;
    int DST_IP_OFFSET = SRC_IP_OFFSET + IP6_LEN;

    default boolean ip6Mask(final int baseOffset, final InterfaceMode mode, final AclIpHeaderFields header,
                            final AclIpv6HeaderFields ip6, final ClassifyAddDelTable request, final Logger log) {
        boolean aceIsEmpty = true;
        if (InterfaceMode.L2.equals(mode)) {
            // in L2 mode we need to match ether type
            request.mask[baseOffset + ETHER_TYPE_OFFSET] = (byte) 0xff;
            request.mask[baseOffset + ETHER_TYPE_OFFSET + 1] = (byte) 0xff;
        }
        if (header.getDscp() != null) {
            aceIsEmpty = false;
            // DCSP (bits 4-9 of IP6 header)
            request.mask[baseOffset + IP_VERSION_OFFSET] |= DSCP_MASK1;
            request.mask[baseOffset + IP_VERSION_OFFSET + 1] |= DSCP_MASK2;
        }
        if (header.getProtocol() != null) { // Internet Protocol number
            aceIsEmpty = false;
            request.mask[baseOffset + IP_PROTOCOL_OFFSET] = (byte) IP_PROTOCOL_MASK;
        }
        if (ip6.getFlowLabel() != null) {
            aceIsEmpty = false;
            // bits 12-31
            request.mask[baseOffset + IP_VERSION_OFFSET + 1] |= (byte) 0x0f;
            request.mask[baseOffset + IP_VERSION_OFFSET + 2] = (byte) 0xff;
            request.mask[baseOffset + IP_VERSION_OFFSET + 3] = (byte) 0xff;
        }
        if (header.getSourcePortRange() != null) {
            log.warn("L4 Header fields are not supported. Ignoring {}", header.getSourcePortRange());
        }
        if (header.getDestinationPortRange() != null) {
            log.warn("L4 Header fields are not supported. Ignoring {}", header.getDestinationPortRange());
        }
        if (ip6.getSourceIpv6Network() != null) {
            aceIsEmpty = false;
            final byte[] mask = Impl.toByteMask(ip6.getSourceIpv6Network());
            System.arraycopy(mask, 0, request.mask, baseOffset + SRC_IP_OFFSET, mask.length);
        }
        if (ip6.getDestinationIpv6Network() != null) {
            aceIsEmpty = false;
            final byte[] mask = Impl.toByteMask(ip6.getDestinationIpv6Network());
            System.arraycopy(mask, 0, request.mask, baseOffset + DST_IP_OFFSET, mask.length);
        }
        return aceIsEmpty;
    }

    default boolean ip6Match(final int baseOffset, final InterfaceMode mode, final AclIpHeaderFields header,
                             final AclIpv6HeaderFields ip6, final ClassifyAddDelSession request, final Logger log) {
        boolean noMatch = true;
        if (InterfaceMode.L2.equals(mode)) {
            // match IP6 etherType (0x86dd)
            request.match[baseOffset + ETHER_TYPE_OFFSET] = (byte) 0x86;
            request.match[baseOffset + ETHER_TYPE_OFFSET + 1] = (byte) 0xdd;
        }
        if (header.getDscp() != null) {
            noMatch = false;
            final int dcsp = header.getDscp().getValue();
            // set bits 4-9 of IP6 header:
            request.match[baseOffset + IP_VERSION_OFFSET] |= (byte) (DSCP_MASK1 & (dcsp >> 2));
            request.match[baseOffset + IP_VERSION_OFFSET + 1] |= (byte) (DSCP_MASK2 & (dcsp << 6));
        }
        if (header.getProtocol() != null) { // Internet Protocol number
            noMatch = false;
            request.match[baseOffset + IP_PROTOCOL_OFFSET] = (byte) (IP_PROTOCOL_MASK & header.getProtocol());
        }
        if (ip6.getFlowLabel() != null) {
            noMatch = false;
            final int flowLabel = ip6.getFlowLabel().getValue().intValue();
            // bits 12-31
            request.match[baseOffset + IP_VERSION_OFFSET + 1] |= (byte) (0x0f & (flowLabel >> 16));
            request.match[baseOffset + IP_VERSION_OFFSET + 2] = (byte) (0xff & (flowLabel >> 8));
            request.match[baseOffset + IP_VERSION_OFFSET + 3] = (byte) (0xff & flowLabel);
        }
        if (header.getSourcePortRange() != null) {
            log.warn("L4 Header fields are not supported. Ignoring {}", header.getSourcePortRange());
        }
        if (header.getDestinationPortRange() != null) {
            log.warn("L4 Header fields are not supported. Ignoring {}", header.getDestinationPortRange());
        }
        if (ip6.getSourceIpv6Network() != null) {
            noMatch = false;
            final byte[] match = Impl.toMatchValue(ip6.getSourceIpv6Network());
            System.arraycopy(match, 0, request.match, baseOffset + SRC_IP_OFFSET, IP6_LEN);
        }
        if (ip6.getDestinationIpv6Network() != null) {
            noMatch = false;
            final byte[] match = Impl.toMatchValue(ip6.getDestinationIpv6Network());
            System.arraycopy(match, 0, request.match, baseOffset + DST_IP_OFFSET, IP6_LEN);
        }
        return noMatch;
    }

    class Impl {
        private static final int IP6_MASK_BIT_LENGTH = 128;

        private static byte[] toByteMask(final int prefixLength) {
            final BitSet mask = new BitSet(IP6_MASK_BIT_LENGTH);
            mask.set(0, prefixLength, true);
            if (prefixLength < IP6_MASK_BIT_LENGTH) {
                mask.set(prefixLength, IP6_MASK_BIT_LENGTH, false);
            }
            return mask.toByteArray();
        }

        private static byte[] toByteMask(final Ipv6Prefix ipv6Prefix) {
            final int prefixLength = Short.valueOf(ipv6Prefix.getValue().split("/")[1]);
            return toByteMask(prefixLength);
        }

        private static byte[] toMatchValue(final Ipv6Prefix ipv6Prefix) {
            final String[] split = ipv6Prefix.getValue().split("/");
            final byte[] addressBytes;
            try {
                addressBytes = InetAddress.getByName(split[0]).getAddress();
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Invalid IP6 address", e);
            }
            final byte[] mask = toByteMask(Short.valueOf(split[1]));
            int pos = 0;
            for (; pos < mask.length; ++pos) {
                addressBytes[pos] &= mask[pos];
            }
            // mask can be shorter that address, so we need to clear rest of the address:
            for (; pos < addressBytes.length; ++pos) {
                addressBytes[pos] = 0;
            }
            return addressBytes;
        }
    }
}
