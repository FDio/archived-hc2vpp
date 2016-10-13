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

import com.google.common.primitives.Ints;
import io.fd.honeycomb.translate.vpp.util.Ipv4Translator;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.AclIpHeaderFields;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.AclIpv4HeaderFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;

public interface Ip4AclTranslator extends Ipv4Translator {
    int ETHER_TYPE_OFFSET = 12; // first 14 bytes represent L2 header (2x6)
    int DSCP_OFFSET = 15;
    int DSCP_MASK = 0xfc;

    int IP_PROTOCOL_OFFSET = ETHER_TYPE_OFFSET + 11;
    int IP_PROTOCOL_MASK = 0xff;

    int IP4_LEN = 4;
    int IP4_MASK_BIT_LENGTH = 32;
    int SRC_IP_OFFSET = ETHER_TYPE_OFFSET + 14;
    int DST_IP_OFFSET = SRC_IP_OFFSET + IP4_LEN;
    int SRC_PORT_OFFSET = DST_IP_OFFSET + IP4_LEN;
    int DST_PORT_OFFSET = SRC_PORT_OFFSET + 2;

    default boolean ip4Mask(final int baseOffset, final InterfaceMode mode, final AclIpHeaderFields header,
                            final AclIpv4HeaderFields ip4, final ClassifyAddDelTable request) {
        boolean aceIsEmpty = true;
        if (InterfaceMode.L2.equals(mode)) {
            // in L2 mode we need to match ether type
            request.mask[baseOffset + ETHER_TYPE_OFFSET] = (byte) 0xff;
            request.mask[baseOffset + ETHER_TYPE_OFFSET + 1] = (byte) 0xff;
        }
        if (header.getDscp() != null) {
            aceIsEmpty = false;
            request.mask[baseOffset + DSCP_OFFSET] = (byte) DSCP_MASK; // first 6 bits
        }
        if (header.getProtocol() != null) { // Internet Protocol number
            aceIsEmpty = false;
            request.mask[baseOffset + IP_PROTOCOL_OFFSET] = (byte) IP_PROTOCOL_MASK;
        }
        if (header.getSourcePortRange() != null) {
            // TODO (HONEYCOMB-253): port matching will not work correctly if Options are present
            aceIsEmpty = false;
            request.mask[baseOffset + SRC_PORT_OFFSET] = (byte) 0xff;
            request.mask[baseOffset + SRC_PORT_OFFSET + 1] = (byte) 0xff;
        }
        if (header.getDestinationPortRange() != null) {
            // TODO (HONEYCOMB-253): port matching will not work correctly if Options are present
            aceIsEmpty = false;
            request.mask[baseOffset + DST_PORT_OFFSET] = (byte) 0xff;
            request.mask[baseOffset + DST_PORT_OFFSET + 1] = (byte) 0xff;
        }
        if (ip4.getSourceIpv4Network() != null) {
            aceIsEmpty = false;
            System.arraycopy(Impl.toByteMask(ip4.getSourceIpv4Network()), 0, request.mask,
                baseOffset + SRC_IP_OFFSET, IP4_LEN);
        }
        if (ip4.getDestinationIpv4Network() != null) {
            aceIsEmpty = false;
            System.arraycopy(Impl.toByteMask(ip4.getDestinationIpv4Network()), 0, request.mask,
                baseOffset + DST_IP_OFFSET, IP4_LEN);
        }
        return aceIsEmpty;
    }

    default boolean ip4Match(final int baseOffset, final InterfaceMode mode, final AclIpHeaderFields header,
                             final AclIpv4HeaderFields ip4, final Integer srcPort,
                             final Integer dstPort, final ClassifyAddDelSession request) {
        boolean noMatch = true;
        if (InterfaceMode.L2.equals(mode)) {
            // match IP4 etherType (0x0800)
            request.match[baseOffset + ETHER_TYPE_OFFSET] = 0x08;
            request.match[baseOffset + ETHER_TYPE_OFFSET + 1] = 0x00;
        }
        if (header.getDscp() != null) {
            noMatch = false;
            request.match[baseOffset + DSCP_OFFSET] = (byte) (DSCP_MASK & (header.getDscp().getValue() << 2));
        }
        if (header.getProtocol() != null) { // Internet Protocol number
            noMatch = false;
            request.match[baseOffset + IP_PROTOCOL_OFFSET] = (byte) (IP_PROTOCOL_MASK & header.getProtocol());
        }
        if (srcPort != null) {
            // TODO (HONEYCOMB-253): port matching will not work correctly if Options are present
            noMatch = false;
            request.match[baseOffset + SRC_PORT_OFFSET] = (byte) (0xff & srcPort >> 8);
            request.match[baseOffset + SRC_PORT_OFFSET + 1] = (byte) (0xff & srcPort);
        }
        if (header.getDestinationPortRange() != null) {
            // TODO (HONEYCOMB-253): port matching will not work correctly if Options are present
            noMatch = false;
            request.match[baseOffset + DST_PORT_OFFSET] = (byte) (0xff & dstPort >> 8);
            request.match[baseOffset + DST_PORT_OFFSET + 1] = (byte) (0xff & dstPort);
        }
        if (ip4.getSourceIpv4Network() != null) {
            noMatch = false;
            System.arraycopy(Impl.toMatchValue(ip4.getSourceIpv4Network()), 0, request.match,
                baseOffset + SRC_IP_OFFSET, IP4_LEN);

        }
        if (ip4.getDestinationIpv4Network() != null) {
            noMatch = false;
            System.arraycopy(Impl.toMatchValue(ip4.getDestinationIpv4Network()), 0, request.match,
                baseOffset + DST_IP_OFFSET, IP4_LEN);

        }
        return noMatch;
    }

    class Impl {
        private static byte[] toByteMask(final int prefixLength) {
            final long mask = ((1L << prefixLength) - 1) << (IP4_MASK_BIT_LENGTH - prefixLength);
            return Ints.toByteArray((int) mask);
        }

        private static byte[] toByteMask(final Ipv4Prefix ipv4Prefix) {
            final int prefixLength = Byte.valueOf(ipv4Prefix.getValue().split("/")[1]);
            return toByteMask(prefixLength);
        }

        private static byte[] toMatchValue(final Ipv4Prefix ipv4Prefix) {
            final String[] split = ipv4Prefix.getValue().split("/");
            final byte[] addressBytes = Ipv4Translator.INSTANCE.ipv4AddressNoZoneToArray(split[0]);
            final byte[] mask = Impl.toByteMask(Byte.valueOf(split[1]));
            for (int i = 0; i < addressBytes.length; ++i) {
                addressBytes[i] &= mask[i];
            }
            return addressBytes;
        }
    }
}
