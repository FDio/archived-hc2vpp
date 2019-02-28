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

import com.google.common.base.Preconditions;
import io.fd.jvpp.acl.types.AclRule;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.ValueRange;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppIcmpAceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppTcpAceAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.L4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.icmp.Icmp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.Tcp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.destination.port.DestinationPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.source.port.SourcePort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.source.port.source.port.RangeOrOperator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.Udp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.AclTcpHeaderFields;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.port.range.or.operator.PortRangeOrOperator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.port.range.or.operator.port.range.or.operator.Range;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 * Creates ACL rules pre-bind with protocol-related fields.<br>
 * Support TCP, UDP, ICMP and ICMPv6 protocol numbers according to
 * <a href="http://www.iana.org/assignments/protocol-numbers/protocol-numbers.xhtml"> this document </a>
 */
public interface ProtoPreBindRuleProducer {

    int ICMP_INDEX = 1;
    int TCP_INDEX = 6;
    int UDP_INDEX = 17;
    int ICMPV6_INDEX = 58;
    short MAX_PORT_NUMBER = (short) 65535;

    static AclRule bindIcmpVppFields(AclRule rule, Icmp icmp) {
        VppIcmpAceAugmentation vppIcmp = icmp.augmentation(VppIcmpAceAugmentation.class);
        Preconditions.checkNotNull(vppIcmp.getVppIcmpAce(), "Cannot determine VPP ICMP attributes!");
        final ValueRange typesRange = vppIcmp.getVppIcmpAce().getIcmpTypeRange();
        final ValueRange codesRange = vppIcmp.getVppIcmpAce().getIcmpCodeRange();

        if (typesRange != null) {
            rule.srcportOrIcmptypeFirst = Preconditions.checkNotNull(typesRange.getFirst());
            rule.srcportOrIcmptypeLast = Preconditions.checkNotNull(typesRange.getLast());
        }

        if (codesRange != null) {
            rule.dstportOrIcmpcodeFirst = Preconditions.checkNotNull(codesRange.getFirst());
            rule.dstportOrIcmpcodeLast = Preconditions.checkNotNull(codesRange.getLast());
        }

        return rule;
    }

    static void bindSourcePortRange(@Nonnull final AclRule rule, @Nullable final PortRangeOrOperator sourcePortRange) {
        // allow all ports by default:
        rule.srcportOrIcmptypeFirst = 0;
        rule.srcportOrIcmptypeLast = MAX_PORT_NUMBER;

        if (sourcePortRange != null) {
            // lower port is mandatory
            Preconditions.checkArgument(sourcePortRange instanceof Range);
            Range portRange = (Range) sourcePortRange;
            Preconditions.checkNotNull(portRange.getLowerPort(), "Lower port is mandatory!");
            rule.srcportOrIcmptypeFirst = portNumber(portRange.getLowerPort());

            if (portRange.getUpperPort() != null) {
                rule.srcportOrIcmptypeLast = portNumber(portRange.getUpperPort());
            } else {
                // if upper port is missing, set lower port value as end of checked range:
                rule.srcportOrIcmptypeLast = rule.srcportOrIcmptypeFirst;
            }
        }
    }

    static void bindDestinationPortRange(@Nonnull final AclRule rule,
                                         @Nullable final PortRangeOrOperator destinationPortRange) {
        // allow all ports by default:
        rule.dstportOrIcmpcodeFirst = 0;
        rule.dstportOrIcmpcodeLast = MAX_PORT_NUMBER;

        if (destinationPortRange != null) {
            // lower port is mandatory
            Preconditions.checkArgument(destinationPortRange instanceof Range);
            Range portRange = (Range) destinationPortRange;
            Preconditions.checkNotNull(portRange.getLowerPort(), "Lower port is mandatory!");
            rule.dstportOrIcmpcodeFirst = portNumber(portRange.getLowerPort());

            if (portRange.getUpperPort() != null) {
                rule.dstportOrIcmpcodeLast = portNumber(portRange.getUpperPort());
            } else {
                // if upper port is missing, set lower port value as end of checked range:
                rule.dstportOrIcmpcodeLast = rule.dstportOrIcmpcodeFirst;
            }
        }
    }

    static AclRule bindTcpAttributes(AclRule rule, Tcp tcp) {
        SourcePort srcPort = Preconditions.checkNotNull(tcp.getSourcePort()).getSourcePort();
        if (srcPort instanceof RangeOrOperator) {
            bindSourcePortRange(rule, ((RangeOrOperator) srcPort).getPortRangeOrOperator());
        }

        DestinationPort dstPort = Preconditions.checkNotNull(tcp.getDestinationPort()).getDestinationPort();
        if (dstPort instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.destination.port.destination.port.RangeOrOperator) {
            bindDestinationPortRange(rule,
                    ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.destination.port.destination.port.RangeOrOperator) dstPort)
                            .getPortRangeOrOperator());
        }
        AclTcpHeaderFields.Flags flags = tcp.getFlags();

        if (flags != null) {
            rule.tcpFlagsValue = parseTcpFlagsValue(flags);
        }

        VppTcpAceAugmentation vppTcpAceAugmentation = tcp.augmentation(VppTcpAceAugmentation.class);
        if (vppTcpAceAugmentation != null && vppTcpAceAugmentation.getVppTcpAce() != null &&
                vppTcpAceAugmentation.getVppTcpAce().getFlagsMask() != null) {
            rule.tcpFlagsMask = parseTcpFlagsMask(vppTcpAceAugmentation.getVppTcpAce().getFlagsMask());
        }

        return rule;
    }

    static byte parseTcpFlagsValue(AclTcpHeaderFields.Flags flags) {
        int fin = flags.isFin() ? 1 : 0;
        int syn = flags.isSyn() ? 1 : 0;
        int rst = flags.isRst() ? 1 : 0;
        int psh = flags.isPsh() ? 1 : 0;
        int ack = flags.isAck() ? 1 : 0;
        int urg = flags.isUrg() ? 1 : 0;
        int ece = flags.isEce() ? 1 : 0;
        int cwr = flags.isCwr() ? 1 : 0;
        String strFlags = String.format("%d%d%d%d%d%d%d%d", fin, syn, rst, psh, ack, urg, ece, cwr);
        return Byte.parseByte(strFlags, 2);
    }

    static byte parseTcpFlagsMask(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.AclTcpHeaderFields.FlagsMask flags) {
        int fin = flags.isFin() ? 1 : 0;
        int syn = flags.isSyn() ? 1 : 0;
        int rst = flags.isRst() ? 1 : 0;
        int psh = flags.isPsh() ? 1 : 0;
        int ack = flags.isAck() ? 1 : 0;
        int urg = flags.isUrg() ? 1 : 0;
        int ece = flags.isEce() ? 1 : 0;
        int cwr = flags.isCwr() ? 1 : 0;
        String strFlags = String.format("%d%d%d%d%d%d%d%d", fin, syn, rst, psh, ack, urg, ece, cwr);
        return Byte.parseByte(strFlags, 2);
    }

    static AclRule bindUdpAttributes(AclRule rule, Udp udp) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.source.port.SourcePort
                srcPort = Preconditions.checkNotNull(udp.getSourcePort()).getSourcePort();

        if (srcPort instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.source.port.source.port.RangeOrOperator) {
            bindSourcePortRange(rule,
                    ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.source.port.source.port.RangeOrOperator) srcPort)
                            .getPortRangeOrOperator());
        }

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.destination.port.DestinationPort
                dstPort = Preconditions.checkNotNull(udp.getDestinationPort()).getDestinationPort();

        if (dstPort instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.destination.port.destination.port.RangeOrOperator) {
            bindDestinationPortRange(rule,
                    ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.destination.port.destination.port.RangeOrOperator) dstPort)
                            .getPortRangeOrOperator());
        }
        return rule;
    }

    static AclRule bindDefaultNodes(AclRule rule) {
        rule.srcportOrIcmptypeFirst = 0;
        rule.srcportOrIcmptypeLast = (short) 65535;
        rule.dstportOrIcmpcodeFirst = 0;
        rule.dstportOrIcmpcodeLast = (short) 65535;
        rule.tcpFlagsValue = 0;
        rule.tcpFlagsMask = 0;
        return rule;
    }

    static short portNumber(final PortNumber portNumber) {
        return portNumber.getValue().shortValue();
    }

    /**
     * Pre-bind rule with protocol based attributes (if present).
     *
     * @param vppAce rule to be processed
     * @return AclRule with protocol filled protocol fields
     */
    default AclRule createPreBindRule(@Nonnull final Ace vppAce) {
        AclRule rule = new AclRule();

        L4 l4 = Preconditions.checkNotNull(vppAce.getMatches(), "Matches are not defined for ACE: {}!", vppAce).getL4();
        if (l4 == null) {
            // returns AclRule with rule.proto set to 0 (protocol fields will be ignored by vpp)
            return rule;
        }

        if (l4.getImplementedInterface()
                .equals(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.Icmp.class)) {
            return getIcmpAclRule(vppAce, rule,
                    (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.Icmp) l4);
        } else if (l4.getImplementedInterface()
                .equals(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.Tcp.class)) {
            return getTcpAclRule(rule,
                    (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.Tcp) l4);
        } else if (l4.getImplementedInterface()
                .equals(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.Udp.class)) {
            return getUdpAclRule(rule,
                    (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.Udp) l4);
        } else {
            return bindDefaultNodes(rule);
        }
    }

    default AclRule getIcmpAclRule(@Nonnull final Ace vppAce, final AclRule rule,
                                   final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.Icmp l4) {
        if (vppAce.getMatches().getL3() == null) {
            rule.proto = ICMP_INDEX;
        } else {
            Class<? extends DataContainer> ipVersion = vppAce.getMatches().getL3().getImplementedInterface();
            rule.proto = (byte) (ipVersion.equals(Ipv6.class) ? ICMPV6_INDEX : ICMP_INDEX);
        }

        Icmp icmp = l4.getIcmp();
        return icmp != null ? bindIcmpVppFields(rule, icmp) : bindDefaultNodes(rule);
    }

    default AclRule getUdpAclRule(final AclRule rule,
                                  final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.Udp l4) {
        rule.proto = UDP_INDEX;
        Udp udp = l4.getUdp();
        return udp != null ? bindUdpAttributes(rule, udp) : bindDefaultNodes(rule);
    }

    default AclRule getTcpAclRule(final AclRule rule,
                                  final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.Tcp l4) {
        rule.proto = TCP_INDEX;
        Tcp tcp = l4.getTcp();
        return tcp != null ? bindTcpAttributes(rule, tcp) : bindDefaultNodes(rule);
    }
}
