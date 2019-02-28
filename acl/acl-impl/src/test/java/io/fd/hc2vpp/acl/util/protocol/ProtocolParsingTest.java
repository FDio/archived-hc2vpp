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

import static org.junit.Assert.assertEquals;

import io.fd.hc2vpp.acl.AclTestSchemaContext;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.jvpp.acl.types.AclRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppIcmpAceAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.Acls;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.Icmp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.Tcp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.Udp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.source.port.source.port.RangeOrOperator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.port.range.or.operator.port.range.or.operator.Range;

@RunWith(HoneycombTestRunner.class)
public class ProtocolParsingTest implements ProtoPreBindRuleProducer, IpProtocolReader, AclTestSchemaContext {
    private static final byte IGNORE_PROTOCOL = 0;

    private Ace extractAce(Acls acls) {
        return acls.getAcl().get(0).getAces().getAce().get(0);
    }

    @Test
    public void testIcmpRule(@InjectTestData(resourcePath = "/rules/icmp-rule.json") Acls acls) {
        final AclRule icmpRule = createPreBindRule(extractAce(acls));

        assertEquals(1, icmpRule.proto);
        assertEquals(0, icmpRule.tcpFlagsMask);
        assertEquals(0, icmpRule.tcpFlagsValue);

        final Icmp protocol = (Icmp) parseProtocol(icmpRule);
        assertEquals(5, protocol.getIcmp().augmentation(VppIcmpAceAugmentation.class).getVppIcmpAce().getIcmpTypeRange()
                .getFirst().shortValue());
        assertEquals(8, protocol.getIcmp().augmentation(VppIcmpAceAugmentation.class).getVppIcmpAce().getIcmpTypeRange()
                .getLast().shortValue());
        assertEquals(1, protocol.getIcmp().augmentation(VppIcmpAceAugmentation.class).getVppIcmpAce().getIcmpCodeRange()
                .getFirst().shortValue());
        assertEquals(3, protocol.getIcmp().augmentation(VppIcmpAceAugmentation.class).getVppIcmpAce().getIcmpCodeRange()
                .getLast().shortValue());
    }

    @Test
    public void testIcmpv6Rule(@InjectTestData(resourcePath = "/rules/icmp-v6-rule.json") Acls acls) {
        final AclRule icmpv6Rule = createPreBindRule(extractAce(acls));

        assertEquals(58, icmpv6Rule.proto);
        assertEquals(5, icmpv6Rule.srcportOrIcmptypeFirst);
        assertEquals(8, icmpv6Rule.srcportOrIcmptypeLast);
        assertEquals(1, icmpv6Rule.dstportOrIcmpcodeFirst);
        assertEquals(3, icmpv6Rule.dstportOrIcmpcodeLast);
        assertEquals(0, icmpv6Rule.tcpFlagsMask);
        assertEquals(0, icmpv6Rule.tcpFlagsValue);

        final Icmp protocol = (Icmp) parseProtocol(icmpv6Rule);
        assertEquals(5, protocol.getIcmp().augmentation(VppIcmpAceAugmentation.class).getVppIcmpAce().getIcmpTypeRange()
                .getFirst().shortValue());
        assertEquals(8, protocol.getIcmp().augmentation(VppIcmpAceAugmentation.class).getVppIcmpAce().getIcmpTypeRange()
                .getLast().shortValue());
        assertEquals(1, protocol.getIcmp().augmentation(VppIcmpAceAugmentation.class).getVppIcmpAce().getIcmpCodeRange()
                .getFirst().shortValue());
        assertEquals(3, protocol.getIcmp().augmentation(VppIcmpAceAugmentation.class).getVppIcmpAce().getIcmpCodeRange()
                .getLast().shortValue());
    }

    @Test
    public void testTcpRule(@InjectTestData(resourcePath = "/rules/tcp-rule.json") Acls acls) {
        final AclRule tcpRule = createPreBindRule(extractAce(acls));
        assertEquals(6, tcpRule.proto);
        assertEquals(1, tcpRule.tcpFlagsMask);
        assertEquals(7, tcpRule.tcpFlagsValue);

        final Tcp protocol = (Tcp) parseProtocol(tcpRule);
        assertEquals(1,
                ((Range) ((RangeOrOperator) protocol.getTcp().getSourcePort().getSourcePort()).getPortRangeOrOperator())
                        .getLowerPort().getValue().intValue());
        assertEquals(5487,
                ((Range) ((RangeOrOperator) protocol.getTcp().getSourcePort().getSourcePort()).getPortRangeOrOperator())
                        .getUpperPort().getValue().intValue());
        assertEquals(87,
                ((Range) ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.destination.port.destination.port.RangeOrOperator) protocol
                        .getTcp().getDestinationPort().getDestinationPort())
                        .getPortRangeOrOperator()).getLowerPort().getValue().intValue());
        assertEquals(6745,
                ((Range) ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.destination.port.destination.port.RangeOrOperator) protocol
                        .getTcp().getDestinationPort().getDestinationPort())
                        .getPortRangeOrOperator()).getUpperPort().getValue().intValue());
    }

    @Test
    public void testTcpRuleNoFlags(@InjectTestData(resourcePath = "/rules/tcp-rule-no-flags.json") Acls acls) {
        final AclRule tcpRule = createPreBindRule(extractAce(acls));
        assertEquals(6, tcpRule.proto);
        assertEquals(123, tcpRule.srcportOrIcmptypeFirst);
        assertEquals(123, tcpRule.srcportOrIcmptypeLast);
        assertEquals((short) 65000, tcpRule.dstportOrIcmpcodeFirst);
        assertEquals((short) 65000, tcpRule.dstportOrIcmpcodeLast);
        assertEquals(0, tcpRule.tcpFlagsMask);
        assertEquals(0, tcpRule.tcpFlagsValue);

        final Tcp protocol = (Tcp) parseProtocol(tcpRule);
        assertEquals(123,
                ((Range) ((RangeOrOperator) protocol.getTcp().getSourcePort().getSourcePort()).getPortRangeOrOperator())
                        .getLowerPort().getValue().intValue());
        assertEquals(123,
                ((Range) ((RangeOrOperator) protocol.getTcp().getSourcePort().getSourcePort()).getPortRangeOrOperator())
                        .getUpperPort().getValue().intValue());
        assertEquals(65000,
                ((Range) ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.destination.port.destination.port.RangeOrOperator) protocol
                        .getTcp().getDestinationPort().getDestinationPort())
                        .getPortRangeOrOperator()).getLowerPort().getValue().intValue());
        assertEquals(65000,
                ((Range) ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.destination.port.destination.port.RangeOrOperator) protocol
                        .getTcp().getDestinationPort().getDestinationPort())
                        .getPortRangeOrOperator()).getUpperPort().getValue().intValue());
    }

    @Test
    public void testSourcePortRangeNotGiven() {
        AclRule rule = new AclRule();
        ProtoPreBindRuleProducer.bindSourcePortRange(rule, null);
        assertEquals(0, rule.srcportOrIcmptypeFirst);
        assertEquals(MAX_PORT_NUMBER, rule.srcportOrIcmptypeLast);
    }

    @Test
    public void testDestinationPortRangeNotGiven() {
        AclRule rule = new AclRule();
        ProtoPreBindRuleProducer.bindDestinationPortRange(rule, null);
        assertEquals(0, rule.dstportOrIcmpcodeFirst);
        assertEquals(MAX_PORT_NUMBER, rule.dstportOrIcmpcodeLast);
    }

    @Test
    public void testUdpRule(@InjectTestData(resourcePath = "/rules/udp-rule.json") Acls acls) {
        final AclRule udpRule = createPreBindRule(extractAce(acls));
        assertEquals(17, udpRule.proto);
        assertEquals(0, udpRule.tcpFlagsMask);
        assertEquals(0, udpRule.tcpFlagsValue);

        final Udp protocol = (Udp) parseProtocol(udpRule);
        assertEquals(1,
                ((Range) ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.source.port.source.port.RangeOrOperator) protocol
                        .getUdp().getSourcePort().getSourcePort()).getPortRangeOrOperator())
                        .getLowerPort().getValue().intValue());
        assertEquals(5487,
                ((Range) ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.source.port.source.port.RangeOrOperator) protocol
                        .getUdp().getSourcePort().getSourcePort()).getPortRangeOrOperator())
                        .getUpperPort().getValue().intValue());
        assertEquals(87,
                ((Range) ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.destination.port.destination.port.RangeOrOperator) protocol
                        .getUdp().getDestinationPort().getDestinationPort())
                        .getPortRangeOrOperator()).getLowerPort().getValue().intValue());
        assertEquals(6745,
                ((Range) ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.destination.port.destination.port.RangeOrOperator) protocol
                        .getUdp().getDestinationPort().getDestinationPort())
                        .getPortRangeOrOperator()).getUpperPort().getValue().intValue());
    }


    @Test
    public void tesProtocolNotSpecified(@InjectTestData(resourcePath = "/rules/no-protocol-rule.json") Acls acls) {
        final AclRule noProtocolRule = createPreBindRule(extractAce(acls));

        assertEquals(IGNORE_PROTOCOL, noProtocolRule.proto);
    }
}