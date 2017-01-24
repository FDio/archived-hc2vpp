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

import io.fd.vpp.jvpp.acl.types.AclRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.DestinationPortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.DestinationPortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.SourcePortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.SourcePortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.icmp.header.fields.IcmpCodeRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.icmp.header.fields.IcmpCodeRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.icmp.header.fields.IcmpTypeRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.icmp.header.fields.IcmpTypeRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.IpProtocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.Icmp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.IcmpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.IcmpV6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.IcmpV6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.Other;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.OtherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.Tcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.TcpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.Udp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.UdpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.icmp.IcmpNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.icmp.v6.IcmpV6NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.other.OtherNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.tcp.TcpNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.udp.UdpNodesBuilder;

/**
 * Utility for parsing IpProtocol DO based on data returned by vpp as {@link AclRule}.
 */
public interface IpProtocolReader {

    default IpProtocol parseProtocol(final AclRule rule) {
        switch (rule.proto) {
            case ICMP_INDEX: {
                return Impl.parseIcmp(rule);
            }

            case TCP_INDEX: {
                return Impl.parseTcp(rule);
            }

            case UDP_INDEX: {
                return Impl.parseUdp(rule);
            }

            case ICMPV6_INDEX: {
                return Impl.parseIcmp6(rule);
            }
            default: {
                return Impl.parse(rule);
            }
        }
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
            final IcmpNodesBuilder nodes = new IcmpNodesBuilder();
            nodes.setIcmpCodeRange(parseIcmpCodeRange(rule));
            nodes.setIcmpTypeRange(parseIcmpTypeRange(rule));
            return new IcmpBuilder().setIcmpNodes(nodes.build()).build();
        }

        private static DestinationPortRange parseDstPortRange(final AclRule rule) {
            return new DestinationPortRangeBuilder()
                .setLowerPort(new PortNumber(Short.toUnsignedInt(rule.dstportOrIcmpcodeFirst)))
                .setUpperPort(new PortNumber(Short.toUnsignedInt(rule.dstportOrIcmpcodeLast))).build();
        }

        private static SourcePortRange parseSrcPortRange(final AclRule rule) {
            return new SourcePortRangeBuilder()
                .setLowerPort(new PortNumber(Short.toUnsignedInt(rule.srcportOrIcmptypeFirst)))
                .setUpperPort(new PortNumber(Short.toUnsignedInt(rule.srcportOrIcmptypeLast))).build();
        }

        private static Tcp parseTcp(final AclRule rule) {
            final TcpNodesBuilder nodes = new TcpNodesBuilder();
            nodes.setDestinationPortRange(parseDstPortRange(rule));
            nodes.setSourcePortRange(parseSrcPortRange(rule));
            nodes.setTcpFlagsMask((short) Byte.toUnsignedInt(rule.tcpFlagsMask));
            nodes.setTcpFlagsValue((short) Byte.toUnsignedInt(rule.tcpFlagsValue));
            return new TcpBuilder().setTcpNodes(nodes.build()).build();
        }

        private static Udp parseUdp(final AclRule rule) {
            final UdpNodesBuilder nodes = new UdpNodesBuilder();
            nodes.setDestinationPortRange(parseDstPortRange(rule));
            nodes.setSourcePortRange(parseSrcPortRange(rule));
            return new UdpBuilder().setUdpNodes(nodes.build()).build();
        }

        private static IcmpV6 parseIcmp6(final AclRule rule) {
            final IcmpV6NodesBuilder nodes = new IcmpV6NodesBuilder();
            nodes.setIcmpCodeRange(parseIcmpCodeRange(rule));
            nodes.setIcmpTypeRange(parseIcmpTypeRange(rule));
            return new IcmpV6Builder().setIcmpV6Nodes(nodes.build()).build();
        }

        private static Other parse(final AclRule rule) {
            final OtherNodesBuilder nodes = new OtherNodesBuilder();
            nodes.setProtocol((short) Short.toUnsignedInt(rule.proto));
            return new OtherBuilder().setOtherNodes(nodes.build()).build();
        }
    }
}
