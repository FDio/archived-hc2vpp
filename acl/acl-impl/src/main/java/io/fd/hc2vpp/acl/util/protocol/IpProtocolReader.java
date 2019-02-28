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

package io.fd.hc2vpp.acl.util.protocol;

import static io.fd.hc2vpp.acl.util.protocol.ProtoPreBindRuleProducer.ICMPV6_INDEX;
import static io.fd.hc2vpp.acl.util.protocol.ProtoPreBindRuleProducer.ICMP_INDEX;
import static io.fd.hc2vpp.acl.util.protocol.ProtoPreBindRuleProducer.TCP_INDEX;
import static io.fd.hc2vpp.acl.util.protocol.ProtoPreBindRuleProducer.UDP_INDEX;

import com.google.common.annotations.VisibleForTesting;
import io.fd.jvpp.acl.types.AclRule;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppIcmpAceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppIcmpAceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppTcpAceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppTcpAceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.acl.icmp.header.fields.IcmpCodeRange;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.acl.icmp.header.fields.IcmpCodeRangeBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.acl.icmp.header.fields.IcmpTypeRange;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.acl.icmp.header.fields.IcmpTypeRangeBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.acls.acl.aces.ace.matches.l4.icmp.icmp.VppIcmpAceBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.acls.acl.aces.ace.matches.l4.tcp.tcp.VppTcpAceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.L4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.Icmp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.IcmpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.Tcp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.TcpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.Udp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.UdpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.DestinationPortBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.SourcePortBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.source.port.source.port.RangeOrOperatorBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.AclTcpHeaderFields;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.port.range.or.operator.PortRangeOrOperator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.port.range.or.operator.port.range.or.operator.Range;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.port.range.or.operator.port.range.or.operator.RangeBuilder;

/**
 * Utility for parsing IpProtocol DO based on data returned by vpp as {@link AclRule}.
 */
public interface IpProtocolReader {

    default L4 parseProtocol(final AclRule rule) {
        switch (rule.proto) {
            case ICMPV6_INDEX:
            case ICMP_INDEX: {
                return Impl.parseIcmp(rule);
            }

            case TCP_INDEX: {
                return Impl.parseTcp(rule);
            }

            case UDP_INDEX: {
                return Impl.parseUdp(rule);
            }
        }
        return null;
    }

    class Impl {

        private static IcmpCodeRange parseIcmpCodeRange(final AclRule rule) {
            return new IcmpCodeRangeBuilder()
                .setFirst(rule.dstportOrIcmpcodeFirst)
                .setLast(rule.dstportOrIcmpcodeLast).build();
        }

        private static IcmpTypeRange parseIcmpTypeRange(final AclRule rule) {
            return new IcmpTypeRangeBuilder()
                .setFirst(rule.srcportOrIcmptypeFirst)
                .setLast(rule.srcportOrIcmptypeLast).build();
        }

        private static Icmp parseIcmp(final AclRule rule) {
            return new IcmpBuilder().setIcmp(
                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.icmp.IcmpBuilder()
                            .addAugmentation(VppIcmpAceAugmentation.class,
                                    new VppIcmpAceAugmentationBuilder()
                                            .setVppIcmpAce(new VppIcmpAceBuilder()
                                                    .setIcmpCodeRange(parseIcmpCodeRange(rule))
                                                    .setIcmpTypeRange(parseIcmpTypeRange(rule))
                                                    .build())
                                            .build())
                            .build())
                    .build();
        }

        private static PortRangeOrOperator parseDstPortRange(final AclRule rule) {
            return new RangeBuilder()
                    .setLowerPort(new PortNumber(Short.toUnsignedInt(rule.dstportOrIcmpcodeFirst)))
                    .setUpperPort(new PortNumber(Short.toUnsignedInt(rule.dstportOrIcmpcodeLast))).build();
        }

        private static Range parseSrcPortRange(final AclRule rule) {
            return new RangeBuilder()
                .setLowerPort(new PortNumber(Short.toUnsignedInt(rule.srcportOrIcmptypeFirst)))
                .setUpperPort(new PortNumber(Short.toUnsignedInt(rule.srcportOrIcmptypeLast))).build();
        }

        private static Tcp parseTcp(final AclRule rule) {
            return new TcpBuilder().setTcp(
                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.TcpBuilder()
                            .setFlags(parseTcpFlags((short) Byte.toUnsignedInt(rule.tcpFlagsValue)))
                            .addAugmentation(VppTcpAceAugmentation.class, new VppTcpAceAugmentationBuilder()
                                    .setVppTcpAce(new VppTcpAceBuilder()
                                            .setFlagsMask(
                                                    parseTcpFlagsMask((short) Byte.toUnsignedInt(rule.tcpFlagsMask)))
                                            .build())
                                    .build())
                            .setSourcePort(new SourcePortBuilder()
                                    .setSourcePort(new RangeOrOperatorBuilder()
                                            .setPortRangeOrOperator(parseSrcPortRange(rule))
                                            .build())
                                    .build())
                            .setDestinationPort(new DestinationPortBuilder()
                                    .setDestinationPort(
                                            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.destination.port.destination.port.RangeOrOperatorBuilder()
                                                    .setPortRangeOrOperator(parseDstPortRange(rule))
                                                    .build())
                                    .build())
                            .build())
                    .build();
        }

        @VisibleForTesting
        private static org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.AclTcpHeaderFields.FlagsMask parseTcpFlagsMask(
                final short tcpFlagsMask) {
            // Flags from bit on position 1 to 8
            final Boolean cwr = (tcpFlagsMask & 0b00000001) == 1;
            final Boolean ece = (tcpFlagsMask & 0b00000010) >> 1 == 1;
            final Boolean urg = (tcpFlagsMask & 0b00000100) >> 2 == 1;
            final Boolean ack = (tcpFlagsMask & 0b00001000) >> 3 == 1;
            final Boolean psh = (tcpFlagsMask & 0b00010000) >> 4 == 1;
            final Boolean rst = (tcpFlagsMask & 0b00100000) >> 5 == 1;
            final Boolean syn = (tcpFlagsMask & 0b01000000) >> 6 == 1;
            final Boolean fin = (tcpFlagsMask & 0b10000000) >> 7 == 1;

            return new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.AclTcpHeaderFields.FlagsMask(
                    ack, cwr, ece, fin, psh, rst, syn, urg);
        }

        @VisibleForTesting
        private static AclTcpHeaderFields.Flags parseTcpFlags(final short tcpFlagsValue) {
            // Flags from bit on position 1 to 8
            final Boolean cwr = (tcpFlagsValue & 0b00000001) == 1;
            final Boolean ece = (tcpFlagsValue & 0b00000010) >> 1 == 1;
            final Boolean urg = (tcpFlagsValue & 0b00000100) >> 2 == 1;
            final Boolean ack = (tcpFlagsValue & 0b00001000) >> 3 == 1;
            final Boolean psh = (tcpFlagsValue & 0b00010000) >> 4 == 1;
            final Boolean rst = (tcpFlagsValue & 0b00100000) >> 5 == 1;
            final Boolean syn = (tcpFlagsValue & 0b01000000) >> 6 == 1;
            final Boolean fin = (tcpFlagsValue & 0b10000000) >> 7 == 1;

            return new AclTcpHeaderFields.Flags(ack, cwr, ece, fin, psh, rst, syn, urg);
        }

        private static Udp parseUdp(final AclRule rule) {
            return new UdpBuilder().setUdp(
                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.UdpBuilder()
                            .setSourcePort(
                                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.SourcePortBuilder()
                                            .setSourcePort(
                                                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.source.port.source.port.RangeOrOperatorBuilder()
                                                            .setPortRangeOrOperator(parseSrcPortRange(rule))
                                                            .build())
                                            .build())
                            .setDestinationPort(
                                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.DestinationPortBuilder()
                                            .setDestinationPort(
                                                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.destination.port.destination.port.RangeOrOperatorBuilder()
                                                            .setPortRangeOrOperator(parseDstPortRange(rule))
                                                            .build())
                                            .build())
                            .build())
                    .build();
        }
    }
}
