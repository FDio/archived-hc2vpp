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

import static com.google.common.base.Preconditions.checkArgument;
import static io.fd.hc2vpp.acl.util.protocol.ProtoPreBindRuleProducer.ProtocolPair.pair;

import com.google.common.collect.ImmutableSet;
import io.fd.vpp.jvpp.acl.types.AclRule;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.DestinationPortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.SourcePortRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ValueRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.VppAceNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.IpProtocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.Icmp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.IcmpV6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.Other;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.Tcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.Udp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.icmp.IcmpNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.icmp.v6.IcmpV6Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.tcp.TcpNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.udp.UdpNodes;

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
    short MAX_PORT_NUMBER = (short)65535;

    Set<ProtocolPair> PROTOCOL_PAIRS = ImmutableSet.of(pair(Icmp.class, ICMP_INDEX), pair(Tcp.class, TCP_INDEX),
            pair(Udp.class, UDP_INDEX), pair(IcmpV6.class, ICMPV6_INDEX));

    class ProtocolPair {
        private final Class<? extends IpProtocol> protocolClass;
        private final int index;

        private ProtocolPair(final Class<? extends IpProtocol> protocolClass, final int index) {
            this.protocolClass = protocolClass;
            this.index = index;
        }

        static ProtocolPair pair(@Nonnull final Class<? extends IpProtocol> protocolClass, @Nonnull final int index) {
            return new ProtocolPair(protocolClass, index);
        }

        boolean match(@Nonnull final Class<? extends IpProtocol> protocolClass) {
            return this.protocolClass.isAssignableFrom(protocolClass);
        }

        int getIndex() {
            return this.index;
        }
    }

    static byte protocol(final IpProtocol ipProtocol) {
        final Optional<ProtocolPair> optPair = PROTOCOL_PAIRS.stream()
                .filter(protocolPair -> protocolPair.match(ipProtocol.getClass()))
                .findAny();

        if (!optPair.isPresent()) {
            if (Other.class.isAssignableFrom(ipProtocol.getClass())) {
                return Other.class.cast(ipProtocol).getOtherNodes().getProtocol().byteValue();
            }

            throw new IllegalArgumentException(String.format("Unsupported Protocol Type %s", ipProtocol.getClass()));
        }
        return (byte) optPair.get().getIndex();
    }

    static AclRule bindIcmpNodes(AclRule rule, VppAce ace) {
        final VppAceNodes vppAceNodes = ace.getVppAceNodes();
        checkArgument(vppAceNodes.getIpProtocol() instanceof Icmp);
        final IcmpNodes icmp = Icmp.class.cast(vppAceNodes.getIpProtocol()).getIcmpNodes();
        final ValueRange typesRange = icmp.getIcmpTypeRange();
        final ValueRange codesRange = icmp.getIcmpCodeRange();

        rule.srcportOrIcmptypeFirst = typesRange.getFirst();
        rule.srcportOrIcmptypeLast = typesRange.getLast();
        rule.dstportOrIcmpcodeFirst = codesRange.getFirst();
        rule.dstportOrIcmpcodeLast = codesRange.getLast();

        return rule;
    }

    static AclRule bindIcmpv6Nodes(AclRule rule, VppAce ace) {
        final VppAceNodes vppAceNodes = ace.getVppAceNodes();
        checkArgument(vppAceNodes.getIpProtocol() instanceof IcmpV6);
        final IcmpV6Nodes icmpV6 = IcmpV6.class.cast(vppAceNodes.getIpProtocol()).getIcmpV6Nodes();
        final ValueRange typesRange = icmpV6.getIcmpTypeRange();
        final ValueRange codesRange = icmpV6.getIcmpCodeRange();

        rule.srcportOrIcmptypeFirst = typesRange.getFirst();
        rule.srcportOrIcmptypeLast = typesRange.getLast();
        rule.dstportOrIcmpcodeFirst = codesRange.getFirst();
        rule.dstportOrIcmpcodeLast = codesRange.getLast();

        return rule;
    }

    static void bindSourcePortRange(@Nonnull final AclRule rule, @Nullable final SourcePortRange sourcePortRange) {
        // allow all ports by default:
        rule.srcportOrIcmptypeFirst = 0;
        rule.srcportOrIcmptypeLast = MAX_PORT_NUMBER;

        if(sourcePortRange != null) {
            // lower port is mandatory
            rule.srcportOrIcmptypeFirst = portNumber(sourcePortRange.getLowerPort());

            if (sourcePortRange.getUpperPort() != null) {
                rule.srcportOrIcmptypeLast = portNumber(sourcePortRange.getUpperPort());
            } else {
                // if upper port is missing, set lower port value as end of checked range:
                rule.srcportOrIcmptypeLast = rule.srcportOrIcmptypeFirst;
            }
        }
    }

    static void bindDestinationPortRange(@Nonnull final AclRule rule, @Nullable final DestinationPortRange destinationPortRange) {
        // allow all ports by default:
        rule.dstportOrIcmpcodeFirst = 0;
        rule.dstportOrIcmpcodeLast = MAX_PORT_NUMBER;

        if(destinationPortRange != null) {
            // lower port is mandatory
            rule.dstportOrIcmpcodeFirst = portNumber(destinationPortRange.getLowerPort());

            if (destinationPortRange.getUpperPort() != null) {
                rule.dstportOrIcmpcodeLast = portNumber(destinationPortRange.getUpperPort());
            } else {
                // if upper port is missing, set lower port value as end of checked range:
                rule.dstportOrIcmpcodeLast = rule.dstportOrIcmpcodeFirst;
            }
        }
    }

    static AclRule bindTcpNodes(AclRule rule, VppAce ace) {
        final VppAceNodes vppAceNodes = ace.getVppAceNodes();
        checkArgument(vppAceNodes.getIpProtocol() instanceof Tcp);

        final TcpNodes tcp = Tcp.class.cast(vppAceNodes.getIpProtocol()).getTcpNodes();
        bindSourcePortRange(rule, tcp.getSourcePortRange());
        bindDestinationPortRange(rule, tcp.getDestinationPortRange());

        if(tcp.getTcpFlagsMask() != null) {
            rule.tcpFlagsMask = tcp.getTcpFlagsMask().byteValue();
        }
        if(tcp.getTcpFlagsValue() != null) {
            rule.tcpFlagsValue = tcp.getTcpFlagsValue().byteValue();
        }
        return rule;
    }

    static AclRule bindUdpNodes(AclRule rule, VppAce ace) {
        final VppAceNodes vppAceNodes = ace.getVppAceNodes();
        checkArgument(vppAceNodes.getIpProtocol() instanceof Udp);

        final UdpNodes udp = Udp.class.cast(vppAceNodes.getIpProtocol()).getUdpNodes();
        bindSourcePortRange(rule, udp.getSourcePortRange());
        bindDestinationPortRange(rule, udp.getDestinationPortRange());
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
    default AclRule createPreBindRule(@Nonnull final VppAce vppAce) {
        AclRule rule = new AclRule();


        final IpProtocol ipProtocol = vppAce.getVppAceNodes().getIpProtocol();
        if (ipProtocol == null) {
            // returns AclRule with rule.proto set to 0 (protocol fields will be ignored by vpp)
            return rule;
        }

        rule.proto = protocol(ipProtocol);

        switch (rule.proto) {
            case ICMP_INDEX: {
                return bindIcmpNodes(rule, vppAce);
            }

            case TCP_INDEX: {
                return bindTcpNodes(rule, vppAce);
            }

            case UDP_INDEX: {
                return bindUdpNodes(rule, vppAce);
            }

            case ICMPV6_INDEX: {
                return bindIcmpv6Nodes(rule, vppAce);
            }
            default: {
                return bindDefaultNodes(rule);
            }
        }

    }

}
